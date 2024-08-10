/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.importing.engine.mediator;

import io.camunda.optimize.dto.engine.HistoricVariableUpdateInstanceDto;
import io.camunda.optimize.service.importing.TimestampBasedImportMediator;
import io.camunda.optimize.service.importing.engine.fetcher.instance.VariableUpdateInstanceFetcher;
import io.camunda.optimize.service.importing.engine.handler.VariableUpdateInstanceImportIndexHandler;
import io.camunda.optimize.service.importing.engine.service.VariableUpdateInstanceImportService;
import io.camunda.optimize.service.util.BackoffCalculator;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class VariableUpdateEngineImportMediator
    extends TimestampBasedImportMediator<
        VariableUpdateInstanceImportIndexHandler, HistoricVariableUpdateInstanceDto> {

  private final VariableUpdateInstanceFetcher engineEntityFetcher;

  public VariableUpdateEngineImportMediator(
      final VariableUpdateInstanceImportIndexHandler importIndexHandler,
      final VariableUpdateInstanceFetcher engineEntityFetcher,
      final VariableUpdateInstanceImportService importService,
      final ConfigurationService configurationService,
      final BackoffCalculator idleBackoffCalculator) {
    super(configurationService, idleBackoffCalculator, importIndexHandler, importService);
    this.engineEntityFetcher = engineEntityFetcher;
  }

  @Override
  protected OffsetDateTime getTimestamp(
      final HistoricVariableUpdateInstanceDto historicVariableUpdateInstanceDto) {
    return historicVariableUpdateInstanceDto.getTime();
  }

  @Override
  protected List<HistoricVariableUpdateInstanceDto> getEntitiesNextPage() {
    return engineEntityFetcher.fetchVariableInstanceUpdates(importIndexHandler.getNextPage());
  }

  @Override
  protected List<HistoricVariableUpdateInstanceDto> getEntitiesLastTimestamp() {
    return engineEntityFetcher.fetchVariableInstanceUpdates(
        importIndexHandler.getTimestampOfLastEntity());
  }

  @Override
  protected int getMaxPageSize() {
    return configurationService.getEngineImportVariableInstanceMaxPageSize();
  }

  @Override
  public MediatorRank getRank() {
    return MediatorRank.INSTANCE_SUB_ENTITIES;
  }
}