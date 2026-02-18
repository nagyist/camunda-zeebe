/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.webapps.schema.entities.auditlog;

import io.camunda.webapps.schema.entities.AbstractExporterEntity;
import io.camunda.webapps.schema.entities.SinceVersion;
import java.time.OffsetDateTime;

public class AuditLogCleanupEntity extends AbstractExporterEntity<AuditLogCleanupEntity> {

  // the key by which to delete audit logs
  @SinceVersion(value = "8.9.0", requireDefault = false)
  private String key;

  // the AuditLogTemplate field that the key refers to
  @SinceVersion(value = "8.9.0", requireDefault = false)
  private String keyField;

  // the type of the audit logs to delete, can be null to delete only by key
  @SinceVersion(value = "8.9.0", requireDefault = false)
  private AuditLogEntityType entityType;

  // the timestamp after which to archive the related audit logs
  @SinceVersion(value = "8.9.0", requireDefault = false)
  private OffsetDateTime archivingDate;

  public String getKey() {
    return key;
  }

  public AuditLogCleanupEntity setKey(final String key) {
    this.key = key;
    return this;
  }

  public String getKeyField() {
    return keyField;
  }

  public AuditLogCleanupEntity setKeyField(final String keyField) {
    this.keyField = keyField;
    return this;
  }

  public AuditLogEntityType getEntityType() {
    return entityType;
  }

  public AuditLogCleanupEntity setEntityType(final AuditLogEntityType entityType) {
    this.entityType = entityType;
    return this;
  }

  public OffsetDateTime getArchivingDate() {
    return archivingDate;
  }

  public AuditLogCleanupEntity setArchivingDate(final OffsetDateTime archivingDate) {
    this.archivingDate = archivingDate;
    return this;
  }
}
