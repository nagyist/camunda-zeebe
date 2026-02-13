/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.configuration;

import java.util.Set;

public class ProcessInstanceCreation {
  private static final String PREFIX = "camunda.process-instance-creation";
  private static final boolean DEFAULT_BUSINESS_ID_UNIQUENESS_ENABLED = false;
  private static final Set<String> LEGACY_BUSINESS_ID_UNIQUENESS_ENABLED_PROPERTIES =
      Set.of(
          "zeebe.broker.experimental.engine.processInstanceCreation.businessIdUniquenessEnabled");

  /**
   * Whether to enable business id uniqueness check when creating process instances using a
   * BusinessId.
   *
   * <p>If enabled, the broker will check if there is already an existing active root process
   * instance with the same business id and reject the creation if there is.
   *
   * <p>This is disabled by default, but can be enabled to prevent duplicate process instances with
   * the same business id from being created.
   *
   * <p>Note: When enabled, BusinessId Uniqueness validation will only be applied to process
   * instances created using a BusinessId after enabling the feature.
   */
  private boolean businessIdUniquenessEnabled = DEFAULT_BUSINESS_ID_UNIQUENESS_ENABLED;

  public boolean isBusinessIdUniquenessEnabled() {
    return UnifiedConfigurationHelper.validateLegacyConfiguration(
        PREFIX + ".business-id-uniqueness-enabled",
        businessIdUniquenessEnabled,
        Boolean.class,
        UnifiedConfigurationHelper.BackwardsCompatibilityMode.SUPPORTED,
        LEGACY_BUSINESS_ID_UNIQUENESS_ENABLED_PROPERTIES);
  }

  public void setBusinessIdUniquenessEnabled(final boolean businessIdUniquenessEnabled) {
    this.businessIdUniquenessEnabled = businessIdUniquenessEnabled;
  }
}
