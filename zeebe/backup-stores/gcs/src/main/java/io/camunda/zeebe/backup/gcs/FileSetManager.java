/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.backup.gcs;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.transfermanager.ParallelDownloadConfig;
import com.google.cloud.storage.transfermanager.ParallelUploadConfig;
import com.google.cloud.storage.transfermanager.TransferManager;
import com.google.cloud.storage.transfermanager.TransferStatus;
import io.camunda.zeebe.backup.api.BackupIdentifier;
import io.camunda.zeebe.backup.api.NamedFileSet;
import io.camunda.zeebe.backup.common.FileSet;
import io.camunda.zeebe.backup.common.FileSet.NamedFile;
import io.camunda.zeebe.backup.common.NamedFileSetImpl;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
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
  private final TransferManager transferManager;
  private final BucketInfo bucketInfo;
  private final String basePath;

  FileSetManager(
      final Storage client,
      final TransferManager transferManager,
      final BucketInfo bucketInfo,
      final String basePath) {
    this.client = client;
    this.transferManager = transferManager;
    this.bucketInfo = bucketInfo;
    this.basePath = basePath;
  }

  void save(final BackupIdentifier id, final String fileSetName, final NamedFileSet fileSet) {
    final var prefix = fileSetPath(id, fileSetName);
    final var namedFiles = fileSet.namedFiles();
    final var pathToName =
        namedFiles.entrySet().stream()
            .collect(
                Collectors.toMap(e -> e.getValue().toAbsolutePath().toString(), Map.Entry::getKey));
    final var paths = namedFiles.values().stream().toList();

    final var uploadConfig =
        ParallelUploadConfig.newBuilder()
            .setBucketName(bucketInfo.getName())
            .setUploadBlobInfoFactory(
                (bucketName, fileName) ->
                    BlobInfo.newBuilder(bucketName, prefix + pathToName.get(fileName))
                        .setContentType("application/octet-stream")
                        .build())
            .setSkipIfExists(true)
            .build();

    try {
      final var uploadJob = transferManager.uploadFiles(paths, uploadConfig);
      for (final var result : uploadJob.getUploadResults()) {
        if (result.getStatus() != TransferStatus.SUCCESS
            && result.getStatus() != TransferStatus.SKIPPED) {
          LangUtil.rethrowUnchecked(result.getException());
        }
      }
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
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
    final var prefix = fileSetPath(id, filesetName);
    final var pathByName =
        fileSet.files().stream()
            .collect(Collectors.toMap(NamedFile::name, f -> targetFolder.resolve(f.name())));

    final var blobInfos =
        fileSet.files().stream()
            .map(NamedFile::name)
            .map(
                name ->
                    BlobInfo.newBuilder(bucketInfo.getName(), prefix + name)
                        .setContentType("application/octet-stream")
                        .build())
            .toList();

    final var downloadConfig =
        ParallelDownloadConfig.newBuilder()
            .setBucketName(bucketInfo.getName())
            .setStripPrefix(prefix)
            .setDownloadDirectory(targetFolder)
            .build();

    final var downloadJob = transferManager.downloadBlobs(blobInfos, downloadConfig);

    for (final var result : downloadJob.getDownloadResults()) {
      if (result.getStatus() != TransferStatus.SUCCESS) {
        LangUtil.rethrowUnchecked(result.getException());
      }
    }

    return new NamedFileSetImpl(pathByName);
  }

  private String fileSetPath(final BackupIdentifier id, final String fileSetName) {
    return PATH_FORMAT.formatted(
        basePath, id.partitionId(), id.checkpointId(), id.nodeId(), fileSetName);
  }
}
