/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.webapps.schema.entities.usermanagement;

import io.camunda.webapps.schema.entities.AbstractExporterEntity;

public class MappingEntity extends AbstractExporterEntity<MappingEntity> {

  public static final String DEFAULT_TENANT_IDENTIFIER = "<default>";
  private Long mappingKey;
  private String claimName;
  private String claimValue;

  public MappingEntity() {}

  public Long getMappingKey() {
    return mappingKey;
  }

  public MappingEntity setMappingKey(final Long mappingKey) {
    this.mappingKey = mappingKey;
    return this;
  }

  public String getClaimName() {
    return claimName;
  }

  public MappingEntity setClaimName(final String claimName) {
    this.claimName = claimName;
    return this;
  }

  public String getClaimValue() {
    return claimValue;
  }

  public MappingEntity setClaimValue(final String claimValue) {
    this.claimValue = claimValue;
    return this;
  }
}