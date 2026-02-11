/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.backup.gcs;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.google.api.core.ApiFutures;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.transfermanager.DownloadJob;
import com.google.cloud.storage.transfermanager.DownloadResult;
import com.google.cloud.storage.transfermanager.ParallelDownloadConfig;
import com.google.cloud.storage.transfermanager.ParallelUploadConfig;
import com.google.cloud.storage.transfermanager.TransferManager;
import com.google.cloud.storage.transfermanager.TransferStatus;
import com.google.cloud.storage.transfermanager.UploadJob;
import com.google.cloud.storage.transfermanager.UploadResult;
import io.camunda.zeebe.backup.common.BackupIdentifierImpl;
import io.camunda.zeebe.backup.common.FileSet;
import io.camunda.zeebe.backup.common.FileSet.NamedFile;
import io.camunda.zeebe.backup.common.NamedFileSetImpl;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class FileSetManagerTest {
  private final Storage storage;
  private final TransferManager transferManager;
  private final FileSetManager manager;

  FileSetManagerTest(@Mock final Storage storage, @Mock final TransferManager transferManager) {
    this.storage = storage;
    this.transferManager = transferManager;
    manager = new FileSetManager(storage, transferManager, BucketInfo.of("bucket"), "basePath");
  }

  @Test
  void shouldSaveFileSet() throws IOException {
    // given
    final var backupIdentifier = new BackupIdentifierImpl(1, 2, 3);
    final var namedFileSet =
        new NamedFileSetImpl(
            Map.of("snapshotFile1", Path.of("file1"), "snapshotFile2", Path.of("file2")));

    final var uploadJob =
        UploadJob.newBuilder()
            .setParallelUploadConfig(
                ParallelUploadConfig.newBuilder().setBucketName("bucket").build())
            .build();
    when(transferManager.uploadFiles(anyList(), any(ParallelUploadConfig.class)))
        .thenReturn(uploadJob);

    // when
    manager.save(backupIdentifier, "filesetName", namedFileSet);

    // then
    verify(transferManager).uploadFiles(anyList(), any(ParallelUploadConfig.class));
  }

  @Test
  void shouldThrowExceptionOnSaveFileSet() throws IOException {
    // given
    final var backupIdentifier = new BackupIdentifierImpl(1, 2, 3);
    final var namedFileSet =
        new NamedFileSetImpl(
            Map.of("snapshotFile1", Path.of("file1"), "snapshotFile2", Path.of("file2")));

    final var failedResult =
        UploadResult.newBuilder(
                BlobInfo.newBuilder("bucket", "test").build(), TransferStatus.FAILED_TO_FINISH)
            .setException(new StorageException(412, "expected"))
            .build();
    final var uploadJob =
        UploadJob.newBuilder()
            .setUploadResults(List.of(ApiFutures.immediateFuture(failedResult)))
            .setParallelUploadConfig(
                ParallelUploadConfig.newBuilder().setBucketName("bucket").build())
            .build();
    when(transferManager.uploadFiles(anyList(), any(ParallelUploadConfig.class)))
        .thenReturn(uploadJob);

    // when throw
    Assertions.assertThatThrownBy(() -> manager.save(backupIdentifier, "filesetName", namedFileSet))
        .isInstanceOf(StorageException.class)
        .hasMessageContaining("expected");
  }

  @SuppressWarnings("unchecked")
  @Test
  void shouldDeleteFileSet() {
    // given
    final var backupIdentifier = new BackupIdentifierImpl(1, 2, 3);

    final var mockBlob = mock(Blob.class);
    final var mockPage = mock(Page.class);
    when(mockPage.iterateAll()).thenReturn(List.of(mockBlob));
    when(storage.list(eq("bucket"), any())).thenReturn(mockPage);

    // when
    manager.delete(backupIdentifier, "filesetName");

    // then
    verify(mockBlob).delete();
  }

  @Test
  void shouldThrowExceptionOnDeleteFileSetWhenListThrows() {
    // given
    final var backupIdentifier = new BackupIdentifierImpl(1, 2, 3);
    when(storage.list(eq("bucket"), any())).thenThrow(new StorageException(412, "expected"));

    // when throw
    Assertions.assertThatThrownBy(() -> manager.delete(backupIdentifier, "filesetName"))
        .isInstanceOf(StorageException.class)
        .hasMessageContaining("expected");
  }

  @SuppressWarnings("unchecked")
  @Test
  void shouldThrowExceptionOnDeleteFileSetWhenBlobDeleteThrows() {
    // given
    final var backupIdentifier = new BackupIdentifierImpl(1, 2, 3);

    final Blob mockBlob = mock(Blob.class);
    when(mockBlob.delete()).thenThrow(new StorageException(412, "expected"));
    final var mockPage = mock(Page.class);
    when(mockPage.iterateAll()).thenReturn(List.of(mockBlob));
    when(storage.list(eq("bucket"), any())).thenReturn(mockPage);

    // when throw
    Assertions.assertThatThrownBy(() -> manager.delete(backupIdentifier, "filesetName"))
        .isInstanceOf(StorageException.class)
        .hasMessageContaining("expected");
  }

  @Test
  void shouldRestoreFileSet() {
    // given
    final var backupIdentifier = new BackupIdentifierImpl(1, 2, 3);
    final var fileSet =
        new FileSet(List.of(new NamedFile("snapshotFile"), new NamedFile("snapshotFile2")));
    final Path restorePath = Path.of("restorePath");

    final var downloadJob =
        DownloadJob.newBuilder()
            .setParallelDownloadConfig(
                ParallelDownloadConfig.newBuilder()
                    .setBucketName("bucket")
                    .setDownloadDirectory(restorePath)
                    .build())
            .build();
    when(transferManager.downloadBlobs(anyList(), any(ParallelDownloadConfig.class)))
        .thenReturn(downloadJob);

    // when
    final var namedFileSet = manager.restore(backupIdentifier, "filesetName", fileSet, restorePath);

    // then
    final Path expectedPath1 = Path.of("restorePath/snapshotFile");
    final Path expectedPath2 = Path.of("restorePath/snapshotFile2");

    Assertions.assertThat(namedFileSet.namedFiles())
        .isEqualTo(Map.of("snapshotFile", expectedPath1, "snapshotFile2", expectedPath2));

    verify(transferManager).downloadBlobs(anyList(), any(ParallelDownloadConfig.class));
  }

  @Test
  void shouldThrowRestoreFileSetWhenDownloadToFails() {
    // given
    final var backupIdentifier = new BackupIdentifierImpl(1, 2, 3);
    final var fileSet =
        new FileSet(List.of(new NamedFile("snapshotFile"), new NamedFile("snapshotFile2")));
    final Path restorePath = Path.of("restorePath");

    final var failedResult =
        DownloadResult.newBuilder(
                BlobInfo.newBuilder("bucket", "test").build(), TransferStatus.FAILED_TO_FINISH)
            .setException(new StorageException(412, "expected"))
            .build();
    final var downloadJob =
        DownloadJob.newBuilder()
            .setDownloadResults(List.of(ApiFutures.immediateFuture(failedResult)))
            .setParallelDownloadConfig(
                ParallelDownloadConfig.newBuilder()
                    .setBucketName("bucket")
                    .setDownloadDirectory(restorePath)
                    .build())
            .build();
    when(transferManager.downloadBlobs(anyList(), any(ParallelDownloadConfig.class)))
        .thenReturn(downloadJob);

    // when - then throw
    assertThatThrownBy(() -> manager.restore(backupIdentifier, "filesetName", fileSet, restorePath))
        .isInstanceOf(StorageException.class)
        .hasMessageContaining("expected");
  }
}
