/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.backup.management;

import io.camunda.zeebe.backup.api.Backup;
import io.camunda.zeebe.backup.api.BackupIdentifier;
import io.camunda.zeebe.backup.api.NamedFileSet;
import io.camunda.zeebe.backup.common.BackupDescriptorImpl;
import io.camunda.zeebe.backup.common.BackupImpl;
import io.camunda.zeebe.backup.common.NamedFileSetImpl;
import io.camunda.zeebe.scheduler.ConcurrencyControl;
import io.camunda.zeebe.scheduler.future.ActorFuture;
import io.camunda.zeebe.snapshots.PersistedSnapshot;
import io.camunda.zeebe.snapshots.PersistedSnapshotStore;
import io.camunda.zeebe.snapshots.SnapshotException.SnapshotNotFoundException;
import io.camunda.zeebe.snapshots.SnapshotReservation;
import io.camunda.zeebe.util.Either;
import io.camunda.zeebe.util.VersionUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InProgressBackupImpl implements InProgressBackup {

  private static final Logger LOG = LoggerFactory.getLogger(InProgressBackupImpl.class);

  private static final String ERROR_MSG_NO_VALID_SNAPSHOT =
      "Cannot find a snapshot that can be included in the backup %d. All available snapshots (%s) have processedPosition or lastFollowupEventPosition > checkpointPosition %d";

  private final PersistedSnapshotStore snapshotStore;
  private final BackupIdentifier backupId;
  private final long checkpointPosition;
  private final int numberOfPartitions;
  private final ConcurrencyControl concurrencyControl;

  private final Path segmentsDirectory;

  private final JournalInfoProvider journalInfoProvider;

  // Snapshot related data
  private boolean hasSnapshot = true;
  private Set<PersistedSnapshot> availableValidSnapshots;
  private SnapshotReservation snapshotReservation;
  private PersistedSnapshot reservedSnapshot;
  private NamedFileSet snapshotFileSet;
  private NamedFileSet segmentsFileSet;

  InProgressBackupImpl(
      final PersistedSnapshotStore snapshotStore,
      final BackupIdentifier backupId,
      final long checkpointPosition,
      final int numberOfPartitions,
      final ConcurrencyControl concurrencyControl,
      final Path segmentsDirectory,
      final JournalInfoProvider journalInfoProvider) {
    this.snapshotStore = snapshotStore;
    this.backupId = backupId;
    this.checkpointPosition = checkpointPosition;
    this.numberOfPartitions = numberOfPartitions;
    this.concurrencyControl = concurrencyControl;
    this.segmentsDirectory = segmentsDirectory;
    this.journalInfoProvider = journalInfoProvider;
  }

  @Override
  public long checkpointId() {
    return backupId.checkpointId();
  }

  @Override
  public long checkpointPosition() {
    return checkpointPosition;
  }

  @Override
  public BackupIdentifier id() {
    return backupId;
  }

  @Override
  public ActorFuture<Void> findValidSnapshot() {
    final ActorFuture<Void> result = concurrencyControl.createFuture();
    snapshotStore
        .getAvailableSnapshots()
        .onComplete(
            (snapshots, error) -> {
              if (error != null) {
                result.completeExceptionally(error);
              } else if (snapshots.isEmpty()) {
                // no snapshot is taken until now, so return successfully
                hasSnapshot = false;
                availableValidSnapshots = Collections.emptySet();
                result.complete(null);
              } else {
                final var eitherSnapshots = findValidSnapshot(snapshots);
                if (eitherSnapshots.isLeft()) {
                  result.completeExceptionally(
                      new SnapshotNotFoundException(eitherSnapshots.getLeft()));
                } else {
                  availableValidSnapshots = eitherSnapshots.get();
                  result.complete(null);
                }
              }
            });

    return result;
  }

  @Override
  public ActorFuture<Void> reserveSnapshot() {
    final ActorFuture<Void> future = concurrencyControl.createFuture();
    if (hasSnapshot) {
      // Try reserve snapshot in the order - latest snapshot first
      final var snapshotIterator =
          availableValidSnapshots.stream()
              .sorted(Comparator.comparingLong(PersistedSnapshot::getCompactionBound).reversed())
              .iterator();

      tryReserveAnySnapshot(snapshotIterator, future);
    } else {
      // No snapshot to reserve
      future.complete(null);
    }

    return future;
  }

  @Override
  public ActorFuture<Void> findSnapshotFiles() {
    if (!hasSnapshot) {
      snapshotFileSet = new NamedFileSetImpl(Map.of());
      return concurrencyControl.createCompletedFuture();
    }

    final ActorFuture<Void> filesCollected = concurrencyControl.createFuture();

    final Path snapshotRoot = reservedSnapshot.getPath();
    try (final var stream = Files.list(snapshotRoot)) {
      final var snapshotFiles = stream.collect(Collectors.toSet());
      final var checksumFile = reservedSnapshot.getChecksumPath();

      final Map<String, Path> fileSet = new HashMap<>();
      snapshotFiles.forEach(
          path -> {
            final var name = snapshotRoot.relativize(path);
            fileSet.put(name.toString(), path);
          });

      fileSet.put(checksumFile.getFileName().toString(), checksumFile);

      snapshotFileSet = new NamedFileSetImpl(fileSet);

      filesCollected.complete(null);

    } catch (final IOException e) {
      filesCollected.completeExceptionally(e);
    }

    return filesCollected;
  }

  @Override
  public ActorFuture<Void> findSegmentFiles() {
    final ActorFuture<Void> filesCollected = concurrencyControl.createFuture();
    try {
      long maxIndex = 0L;
      if (availableValidSnapshots != null) {
        maxIndex =
            availableValidSnapshots.stream()
                .mapToLong(PersistedSnapshot::getIndex)
                .max()
                .orElse(0L);
      }
      final var segments = journalInfoProvider.getTailSegments(maxIndex);
      segments.whenComplete(
          (journalMetadata, throwable) -> {
            if (throwable != null) {
              filesCollected.completeExceptionally(throwable);
            } else if (journalMetadata.isEmpty()) {
              filesCollected.completeExceptionally(
                  new IllegalStateException("Segments must not be empty"));
            } else {
              final Map<String, Path> map =
                  journalMetadata.stream()
                      .collect(
                          Collectors.toMap(
                              path -> segmentsDirectory.relativize(path).toString(),
                              Function.identity()));
              segmentsFileSet = new NamedFileSetImpl(map);
              filesCollected.complete(null);
            }
          });
    } catch (final Exception e) {
      filesCollected.completeExceptionally(e);
    }
    return filesCollected;
  }

  @Override
  public Backup createBackup() {
    final Optional<String> snapshotId;

    if (hasSnapshot) {
      snapshotId = Optional.of(reservedSnapshot.getId());
    } else {
      snapshotId = Optional.empty();
    }

    final var backupDescriptor =
        new BackupDescriptorImpl(
            snapshotId, checkpointPosition, numberOfPartitions, VersionUtil.getVersion());
    return new BackupImpl(backupId, backupDescriptor, snapshotFileSet, segmentsFileSet);
  }

  @Override
  public void close() {
    if (snapshotReservation != null) {
      snapshotReservation.release();
      LOG.debug("Released reservation for snapshot {}", reservedSnapshot.getId());
    }
  }

  private Either<String, Set<PersistedSnapshot>> findValidSnapshot(
      final Set<PersistedSnapshot> snapshots) {
    final var validSnapshots =
        snapshots.stream()
            .filter(s -> s.getMetadata().processedPosition() < checkpointPosition) // &&
            .filter(s -> s.getMetadata().lastFollowupEventPosition() < checkpointPosition)
            .collect(Collectors.toSet());

    if (validSnapshots.isEmpty()) {
      return Either.left(
          String.format(
              ERROR_MSG_NO_VALID_SNAPSHOT, checkpointId(), snapshots, checkpointPosition));
    } else {
      return Either.right(validSnapshots);
    }
  }

  private void tryReserveAnySnapshot(
      final Iterator<PersistedSnapshot> snapshotIterator, final ActorFuture<Void> future) {
    final var snapshot = snapshotIterator.next();

    LOG.debug("Attempting to reserve snapshot {}", snapshot.getId());
    final ActorFuture<SnapshotReservation> reservationFuture = snapshot.reserve();
    reservationFuture.onComplete(
        (reservation, error) -> {
          if (error != null) {
            LOG.trace("Attempting to reserve snapshot {}, but failed", snapshot.getId(), error);
            if (snapshotIterator.hasNext()) {
              tryReserveAnySnapshot(snapshotIterator, future);
            } else {
              // fail future.
              future.completeExceptionally(
                  String.format(
                      "Attempted to reserve snapshots %s, but no snapshot could be reserved",
                      availableValidSnapshots),
                  error);
            }
          } else {
            // complete
            snapshotReservation = reservation;
            reservedSnapshot = snapshot;
            LOG.debug("Reserved snapshot {}", snapshot.getId());
            future.complete(null);
          }
        });
  }
}
