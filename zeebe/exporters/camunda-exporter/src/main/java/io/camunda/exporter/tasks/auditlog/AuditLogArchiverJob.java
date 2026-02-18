/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.tasks.auditlog;

import io.camunda.exporter.metrics.CamundaExporterMetrics;
import io.camunda.exporter.tasks.BackgroundTask;
import io.camunda.webapps.schema.descriptors.index.AuditLogCleanupIndex;
import io.camunda.webapps.schema.descriptors.template.AuditLogTemplate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import org.slf4j.Logger;

public class AuditLogArchiverJob implements BackgroundTask {

  final CamundaExporterMetrics metrics;
  private final AuditLogArchiverRepository repository;
  private final AuditLogCleanupIndex auditLogCleanupIndex;
  private final AuditLogTemplate auditLogTemplate;
  private final Executor executor;
  private final Logger logger;

  public AuditLogArchiverJob(
      final AuditLogArchiverRepository repository,
      final AuditLogCleanupIndex auditLogCleanupIndex,
      final AuditLogTemplate auditLogTemplate,
      final CamundaExporterMetrics exporterMetrics,
      final Logger logger,
      final Executor executor) {
    this.repository = repository;
    this.auditLogCleanupIndex = auditLogCleanupIndex;
    this.auditLogTemplate = auditLogTemplate;
    this.executor = executor;
    metrics = exporterMetrics;
    this.logger = logger;
  }

  CompletableFuture<AuditLogCleanupBatch> getNextBatch() {
    return repository.getNextBatch();
  }

  @Override
  public CompletionStage<Integer> execute() {
    return getNextBatch()
        .thenComposeAsync(this::archiveAuditLogs)
        .thenComposeAsync(this::deleteAuditLogCleanupMetadata);
  }

  private CompletionStage<Integer> deleteAuditLogCleanupMetadata(final AuditLogCleanupBatch batch) {
    final var idsToDelete = batch.ids();
    // TODO: delete audit logs cleanup data by IDs
    return null;
  }

  private CompletionStage<AuditLogCleanupBatch> archiveAuditLogs(final AuditLogCleanupBatch batch) {
    batch
        .auditLogArchive()
        .forEach(
            ((auditLogFilter, keys) ->
                archiveByKeyFieldAndFilters(
                    auditLogFilter.keyField(),
                    keys,
                    Map.of(AuditLogTemplate.ENTITY_TYPE, auditLogFilter.entityType()))));
    return CompletableFuture.completedFuture(batch);
  }

  private void archiveByKeyFieldAndFilters(
      final String keyField, final List<String> keys, final Map<String, String> filters) {
    // TODO: implement archiving of audit logs
  }
}
