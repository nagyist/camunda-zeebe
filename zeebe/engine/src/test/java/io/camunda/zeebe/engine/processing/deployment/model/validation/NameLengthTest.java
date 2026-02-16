/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.deployment.model.validation;

import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.model.bpmn.instance.BpmnModelElementInstance;
import io.camunda.zeebe.model.bpmn.instance.ParallelGateway;
import io.camunda.zeebe.model.bpmn.instance.Process;
import io.camunda.zeebe.model.bpmn.instance.ServiceTask;
import io.camunda.zeebe.model.bpmn.instance.StartEvent;
import io.camunda.zeebe.model.bpmn.instance.UserTask;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public final class NameLengthTest {

  @ParameterizedTest
  @MethodSource("nameLengthTestCases")
  void nameLengthExceeded(
      final Class<? extends BpmnModelElementInstance> expectedElementClass,
      final ProcessBuilder processBuilder) {

    final var name = "a".repeat(ProcessValidationUtil.MAX_NAME_FIELD_LENGTH + 1);

    // when
    final var process = processBuilder.build(name);

    // then
    ProcessValidationUtil.validateProcess(
        process,
        ExpectedValidationResult.expect(
            expectedElementClass,
            "Names must not be longer than the configured max-name-length of %s characters"
                .formatted(ProcessValidationUtil.MAX_NAME_FIELD_LENGTH)));
  }

  @ParameterizedTest
  @MethodSource("nameLengthTestCases")
  void nameLengthNotExceeded(
      final Class<? extends BpmnModelElementInstance> expectedElementClass,
      final ProcessBuilder processBuilder) {

    final var name = "a".repeat(ProcessValidationUtil.MAX_NAME_FIELD_LENGTH);

    // when
    final var process = processBuilder.build(name);

    // then
    ProcessValidationUtil.validateProcess(process);
  }

  private static Stream<Arguments> nameLengthTestCases() {
    return Stream.of(
        Arguments.of(
            Process.class,
            (ProcessBuilder)
                name ->
                    Bpmn.createExecutableProcess("process")
                        .name(name)
                        .startEvent()
                        .serviceTask("task", t -> t.zeebeJobType("test"))
                        .endEvent()
                        .done()),
        Arguments.of(
            ServiceTask.class,
            (ProcessBuilder)
                name ->
                    Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .serviceTask("task", t -> t.name(name).zeebeJobType("test"))
                        .endEvent()
                        .done()),
        Arguments.of(
            UserTask.class,
            (ProcessBuilder)
                name ->
                    Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .userTask("task", t -> t.name(name))
                        .endEvent()
                        .done()),
        Arguments.of(
            ParallelGateway.class,
            (ProcessBuilder)
                name ->
                    Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .userTask("task", t -> t.name(name))
                        .parallelGateway()
                        .name(name)
                        .endEvent()
                        .done()),
        Arguments.of(
            StartEvent.class,
            (ProcessBuilder)
                name ->
                    Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .name(name)
                        .serviceTask("task", t -> t.zeebeJobType("test"))
                        .endEvent()
                        .done()));
  }

  @FunctionalInterface
  private interface ProcessBuilder {
    BpmnModelInstance build(String name);
  }
}
