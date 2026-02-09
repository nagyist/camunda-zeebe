/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.backup.gcs;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.Storage.BlobWriteOption;
import io.camunda.zeebe.backup.api.BackupIdentifier;
import io.camunda.zeebe.backup.api.NamedFileSet;
import io.camunda.zeebe.backup.common.FileSet;
import io.camunda.zeebe.backup.common.FileSet.NamedFile;
import io.camunda.zeebe.backup.common.NamedFileSetImpl;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.agrona.LangUtil;

final class FileSetManager {
  /**
   * The path format consists of the following elements:
   *
   * <ul>
   *   <li>{@code basePath}
   *   <li>{@code "contents"}
   *   <li>{@code partitionId}
   *   <li>{@code checkpointId}
   *   <li>{@code nodeId}
   *   <li>{@code fileSetName}
   * </ul>
   */
  private static final String PATH_FORMAT = "%scontents/%s/%s/%s/%s/";

  private final Storage client;
  private final BucketInfo bucketInfo;
  private final String basePath;

  FileSetManager(final Storage client, final BucketInfo bucketInfo, final String basePath) {
    this.client = client;
    this.bucketInfo = bucketInfo;
    this.basePath = basePath;
  }

  void save(final BackupIdentifier id, final String fileSetName, final NamedFileSet fileSet) {
    try (final var executor =
        Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("gcs-file-set-manager", 0).factory())) {
      final var tasks =
          fileSet.namedFiles().entrySet().stream()
              .map(
                  entry ->
                      (Callable<Blob>)
                          () -> {
                            client.createFrom(
                                blobInfo(id, fileSetName, entry.getKey()),
                                entry.getValue(),
                                BlobWriteOption.doesNotExist());
                            return null;
                          })
              .toList();
      for (final var future : executor.invokeAll(tasks)) {
        future.get();
      }
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while saving backup", e);
    } catch (final ExecutionException e) {
      LangUtil.rethrowUnchecked(e.getCause());
    }
  }

  public void delete(final BackupIdentifier id, final String fileSetName) {
    for (final var blob :
        client
            .list(bucketInfo.getName(), BlobListOption.prefix(fileSetPath(id, fileSetName)))
            .iterateAll()) {
      blob.delete();
    }
  }

  public NamedFileSet restore(
      final BackupIdentifier id,
      final String filesetName,
      final FileSet fileSet,
      final Path targetFolder) {
    final var pathByName =
        fileSet.files().stream()
            .collect(Collectors.toMap(NamedFile::name, f -> targetFolder.resolve(f.name())));

    try (final var executor =
        Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("gcs-file-set-manager", 0).factory())) {

      final var tasks =
          pathByName.entrySet().stream()
              .map(
                  entry ->
                      (Callable<Void>)
                          () -> {
                            client.downloadTo(
                                blobInfo(id, filesetName, entry.getKey()).getBlobId(),
                                entry.getValue());
                            return null;
                          })
              .toList();
      for (final var future : executor.invokeAll(tasks)) {
        future.get();
      }

    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while restoring backup", e);
    } catch (final ExecutionException e) {
      LangUtil.rethrowUnchecked(e.getCause());
    }

    return new NamedFileSetImpl(pathByName);
  }

  private String fileSetPath(final BackupIdentifier id, final String fileSetName) {
    return PATH_FORMAT.formatted(
        basePath, id.partitionId(), id.checkpointId(), id.nodeId(), fileSetName);
  }

  private BlobInfo blobInfo(
      final BackupIdentifier id, final String fileSetName, final String fileName) {
    return BlobInfo.newBuilder(bucketInfo, fileSetPath(id, fileSetName) + fileName)
        .setContentType("application/octet-stream")
        .build();
  }
}
