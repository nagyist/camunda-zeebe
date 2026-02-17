/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.deployment.model.validation;

import io.camunda.zeebe.engine.processing.deployment.transform.BpmnValidatorConfig;
import java.util.Collection;
import java.util.List;
import org.camunda.bpm.model.xml.validation.ModelElementValidator;

public final class ZeebeConfigurationValidators {

  public static Collection<ModelElementValidator<?>> getValidators(
      final BpmnValidatorConfig config) {
    return List.of(new IdLengthValidator(config.maxIdFieldLength()));
  }
}
