/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.tasks.auditlog;

import java.util.concurrent.CompletableFuture;

public interface AuditLogArchiverRepository extends AutoCloseable {

  /**
   * Retrieves the next batch of audit logs to be archived.
   *
   * @return a {@link CompletableFuture} containing the next batch of audit logs to be archived
   */
  CompletableFuture<AuditLogCleanupBatch> getNextBatch();
}
