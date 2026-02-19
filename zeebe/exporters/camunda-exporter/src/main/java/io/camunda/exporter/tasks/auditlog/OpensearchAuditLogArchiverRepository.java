/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.tasks.auditlog;

import io.camunda.exporter.config.ExporterConfiguration.HistoryConfiguration;
import io.camunda.exporter.tasks.archiver.ArchiveBatch.AuditLogCleanupBatch;
import io.camunda.exporter.tasks.util.OpensearchRepository;
import io.camunda.webapps.schema.descriptors.IndexDescriptor;
import java.time.InstantSource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.slf4j.Logger;

public class OpensearchAuditLogArchiverRepository extends OpensearchRepository
    implements AuditLogArchiverRepository {

  private final IndexDescriptor auditLogCleanupIndex;
  private final HistoryConfiguration historyConfig;

  public OpensearchAuditLogArchiverRepository(
      final OpenSearchAsyncClient client,
      final Executor executor,
      final Logger logger,
      final IndexDescriptor auditLogCleanupIndex,
      final HistoryConfiguration historyConfig,
      final InstantSource clock) {
    super(client, executor, logger);
    this.auditLogCleanupIndex = auditLogCleanupIndex;
    this.historyConfig = historyConfig;
  }

  @Override
  public CompletableFuture<AuditLogCleanupBatch> getNextBatch() {
    return null;
  }

  @Override
  public CompletableFuture<Integer> deleteAuditLogCleanupMetadata(
      final AuditLogCleanupBatch batch) {
    return null;
  }
}
