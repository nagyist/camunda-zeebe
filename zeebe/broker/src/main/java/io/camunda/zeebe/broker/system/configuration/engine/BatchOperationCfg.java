/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.broker.system.configuration.engine;

import io.camunda.zeebe.broker.system.configuration.ConfigurationEntry;
import io.camunda.zeebe.engine.EngineConfiguration;
import java.time.Duration;

public class BatchOperationCfg implements ConfigurationEntry {
  private Duration schedulerInterval =
      EngineConfiguration.DEFAULT_BATCH_OPERATION_SCHEDULER_INTERVAL;

  public Duration getSchedulerInterval() {
    return schedulerInterval;
  }

  public void setSchedulerInterval(final Duration schedulerInterval) {
    this.schedulerInterval = schedulerInterval;
  }

  @Override
  public String toString() {
    return "BatchOperationCfg{" + "schedulerInterval=" + schedulerInterval + '}';
  }
}
