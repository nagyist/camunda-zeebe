/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.tasks.archiver;

import io.camunda.exporter.metrics.CamundaExporterMetrics;
import io.camunda.webapps.schema.descriptors.index.AuditLogCleanupIndex;
import io.camunda.webapps.schema.descriptors.template.AuditLogTemplate;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditLogArchiverJobTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogArchiverJobTest.class);

  private final Executor executor = Runnable::run;

  private final AuditLogCleanupIndex auditLogCleanupIndex = new AuditLogCleanupIndex("", true);
  private final AuditLogTemplate auditLogTemplate = new AuditLogTemplate("", true);

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final CamundaExporterMetrics metrics = new CamundaExporterMetrics(meterRegistry);

  @AfterEach
  void cleanUp() {
    meterRegistry.clear();
  }

  @Test
  void shouldMoveAuditLogs() {}

  @Test
  void shouldMoveAuditLogsByCorrectField() {}

  @Test
  void shouldMoveAuditLogsBeforeCleanupData() {}
}
