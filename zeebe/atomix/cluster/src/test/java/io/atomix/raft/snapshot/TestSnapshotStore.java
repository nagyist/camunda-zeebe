/*
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.raft.snapshot;

import io.camunda.zeebe.scheduler.future.ActorFuture;
import io.camunda.zeebe.scheduler.future.CompletableActorFuture;
import io.camunda.zeebe.snapshots.PersistedSnapshot;
import io.camunda.zeebe.snapshots.PersistedSnapshotListener;
import io.camunda.zeebe.snapshots.ReceivableSnapshotStore;
import io.camunda.zeebe.snapshots.ReceivedSnapshot;
import io.camunda.zeebe.snapshots.SnapshotException.SnapshotAlreadyExistsException;
import io.camunda.zeebe.snapshots.SnapshotId;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public class TestSnapshotStore implements ReceivableSnapshotStore {

  final AtomicReference<InMemorySnapshot> currentPersistedSnapshot;
  final NavigableMap<SnapshotId, InMemorySnapshot> persistedSnapshots =
      new ConcurrentSkipListMap<>();
  final List<InMemorySnapshot> receivedSnapshots = new CopyOnWriteArrayList<>();
  final List<PersistedSnapshotListener> listeners = new CopyOnWriteArrayList<>();

  private Runnable interceptorOnNewSnapshot = () -> {};

  public TestSnapshotStore(final AtomicReference<InMemorySnapshot> persistedSnapshotRef) {
    currentPersistedSnapshot = persistedSnapshotRef;
  }

  @Override
  public boolean hasSnapshotId(final String id) {
    return currentPersistedSnapshot.get() != null
        && currentPersistedSnapshot.get().getId().equals(id);
  }

  @Override
  public Optional<PersistedSnapshot> getLatestSnapshot() {
    return Optional.ofNullable(currentPersistedSnapshot.get());
  }

  @Override
  public ActorFuture<Set<PersistedSnapshot>> getAvailableSnapshots() {
    return CompletableActorFuture.completed(new HashSet<>(persistedSnapshots.values()));
  }

  @Override
  public ActorFuture<Long> getCompactionBound() {
    return CompletableActorFuture.completed(
        Optional.ofNullable(persistedSnapshots.firstEntry())
            .map(Entry::getValue)
            .map(InMemorySnapshot::getCompactionBound)
            .orElse(0L));
  }

  @Override
  public ActorFuture<Void> abortPendingSnapshots() {
    receivedSnapshots.clear();
    return CompletableActorFuture.completed(null);
  }

  @Override
  public ActorFuture<Boolean> addSnapshotListener(final PersistedSnapshotListener listener) {
    listeners.add(listener);
    return null;
  }

  @Override
  public ActorFuture<Boolean> removeSnapshotListener(final PersistedSnapshotListener listener) {
    listeners.remove(listener);
    return null;
  }

  @Override
  public long getCurrentSnapshotIndex() {
    if (currentPersistedSnapshot.get() == null) {
      return 0;
    }
    return currentPersistedSnapshot.get().getIndex();
  }

  @Override
  public ActorFuture<Void> delete() {
    currentPersistedSnapshot.set(null);
    receivedSnapshots.clear();
    persistedSnapshots.clear();
    return null;
  }

  @Override
  public Path getPath() {
    return null;
  }

  @Override
  public ActorFuture<ReceivedSnapshot> newReceivedSnapshot(final String snapshotId) {
    if (Optional.ofNullable(currentPersistedSnapshot.get())
        .map(PersistedSnapshot::getId)
        .orElse("")
        .equals(snapshotId)) {
      CompletableActorFuture.completedExceptionally(
          new SnapshotAlreadyExistsException("Snapshot with this ID is already persisted"));
    }

    final var newSnapshot = new InMemorySnapshot(this, snapshotId);
    receivedSnapshots.add(newSnapshot);

    return CompletableActorFuture.completed(newSnapshot);
  }

  @Override
  public void close() {}

  public void newSnapshot(final InMemorySnapshot persistedSnapshot) {
    interceptorOnNewSnapshot.run();
    currentPersistedSnapshot.set(persistedSnapshot);

    final var olderSnapshots =
        new HashMap<>(persistedSnapshots.headMap(persistedSnapshot.snapshotId(), false));
    for (final var snapshot : olderSnapshots.entrySet()) {
      if (!snapshot.getValue().isReserved()) {
        persistedSnapshots.remove(snapshot.getKey());
      }
    }

    persistedSnapshots.put(persistedSnapshot.snapshotId(), persistedSnapshot);
    listeners.forEach(l -> l.onNewSnapshot(persistedSnapshot));
  }

  /** The given interceptor wil be executed before the snapshot is committed. */
  public void interceptOnNewSnapshot(final Runnable interceptor) {
    interceptorOnNewSnapshot = interceptor;
  }

  @Override
  public Optional<PersistedSnapshot> getBootstrapSnapshot() {
    return Optional.empty();
  }

  @Override
  public ActorFuture<PersistedSnapshot> copyForBootstrap(
      final PersistedSnapshot persistedSnapshot, final BiConsumer<Path, Path> copySnapshot) {
    return CompletableActorFuture.completed(null);
  }

  @Override
  public ActorFuture<Void> deleteBootstrapSnapshots() {
    return CompletableActorFuture.completed();
  }
}
