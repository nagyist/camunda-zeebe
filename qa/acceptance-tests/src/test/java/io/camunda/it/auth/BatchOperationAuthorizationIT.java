/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.auth;

import static io.camunda.client.api.search.enums.PermissionType.CREATE;
import static io.camunda.client.api.search.enums.PermissionType.CREATE_BATCH_OPERATION_CANCEL_PROCESS_INSTANCE;
import static io.camunda.client.api.search.enums.PermissionType.CREATE_PROCESS_INSTANCE;
import static io.camunda.client.api.search.enums.ResourceType.BATCH_OPERATION;
import static io.camunda.client.api.search.enums.ResourceType.PROCESS_DEFINITION;
import static io.camunda.client.api.search.enums.ResourceType.RESOURCE;
import static io.camunda.it.util.TestHelper.startProcessInstance;
import static io.camunda.it.util.TestHelper.waitForProcessInstancesToStart;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ProblemException;
import io.camunda.qa.util.auth.Authenticated;
import io.camunda.qa.util.auth.Permissions;
import io.camunda.qa.util.auth.User;
import io.camunda.qa.util.auth.UserDefinition;
import io.camunda.qa.util.multidb.MultiDbTest;
import io.camunda.qa.util.multidb.MultiDbTestApplication;
import io.camunda.zeebe.qa.util.cluster.TestStandaloneBroker;
import java.time.Duration;
import java.util.List;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.function.Executable;

@MultiDbTest
@DisabledIfSystemProperty(named = "test.integration.camunda.database.type", matches = "AWS_OS")
class BatchOperationAuthorizationIT {

  @MultiDbTestApplication
  static final TestStandaloneBroker BROKER =
      new TestStandaloneBroker().withBasicAuth().withAuthorizationsEnabled();

  private static final String ADMIN = "admin";
  private static final String RESTRICTED = "restrictedUser";
  private static final String FORBIDDEN = "forbiddenUser";

  @UserDefinition
  private static final User ADMIN_USER =
      new User(
          ADMIN,
          "password",
          List.of(
              new Permissions(RESOURCE, CREATE, List.of("*")),
              new Permissions(PROCESS_DEFINITION, CREATE_PROCESS_INSTANCE, List.of("*")),
              new Permissions(
                  BATCH_OPERATION, CREATE_BATCH_OPERATION_CANCEL_PROCESS_INSTANCE, List.of("*"))));

  @UserDefinition
  private static final User RESTRICTED_USER =
      new User(
          RESTRICTED,
          "password",
          List.of(
              new Permissions(
                  BATCH_OPERATION, CREATE_BATCH_OPERATION_CANCEL_PROCESS_INSTANCE, List.of("*"))));

  @UserDefinition
  private static final User FORBIDDEN_USER = new User(FORBIDDEN, "password", List.of());

  @BeforeAll
  static void setUp(@Authenticated(ADMIN) final CamundaClient camundaClient) {
    final List<String> processes =
        List.of("service_tasks_v1.bpmn", "service_tasks_v2.bpmn", "incident_process_v1.bpmn");
    processes.forEach(
        process -> deployResource(camundaClient, String.format("process/%s", process)));
    waitForProcessesToBeDeployed(camundaClient, processes.size());

    startProcessInstance(camundaClient, "service_tasks_v1", "{\"xyz\":\"bar\"}");
    startProcessInstance(camundaClient, "service_tasks_v2", "{\"path\":222}");
    startProcessInstance(camundaClient, "incident_process_v1");

    waitForProcessInstancesToStart(camundaClient, 3);
  }

  @Test
  void adminShouldStartBatchOperation(@Authenticated(ADMIN) final CamundaClient camundaClient) {
    // when
    final var batchOperationResponse =
        camundaClient
            .newCreateBatchOperationCommand()
            .processInstanceCancel()
            .filter(b -> {})
            .send()
            .join();

    // then
    assertThat(batchOperationResponse).isNotNull();
  }

  @Test
  void restrictedUserShouldStartBatchOperation(
      @Authenticated(RESTRICTED) final CamundaClient camundaClient) {
    // when
    final var batchOperationResponse =
        camundaClient
            .newCreateBatchOperationCommand()
            .processInstanceCancel()
            .filter(b -> {})
            .send()
            .join();

    // then
    assertThat(batchOperationResponse).isNotNull();
  }

  @Test
  void shouldReturnForbiddenForUnauthorizedBatchOperation(
      @Authenticated(FORBIDDEN) final CamundaClient camundaClient) {
    // when
    final Executable executeGet =
        () ->
            camundaClient
                .newCreateBatchOperationCommand()
                .processInstanceCancel()
                .filter(b -> {})
                .send()
                .join();

    // then
    final var problemException = assertThrows(ProblemException.class, executeGet);
    assertThat(problemException.code()).isEqualTo(403);
    assertThat(problemException.details().getDetail())
        .isEqualTo(
            "Command 'CREATE' rejected with code 'FORBIDDEN': Insufficient permissions to perform operation 'CREATE_BATCH_OPERATION_CANCEL_PROCESS_INSTANCE' on resource 'BATCH_OPERATION'");
  }

  private static void deployResource(final CamundaClient camundaClient, final String resourceName) {
    camundaClient.newDeployResourceCommand().addResourceFromClasspath(resourceName).send().join();
  }

  private static void waitForProcessesToBeDeployed(
      final CamundaClient camundaClient, final int expectedCount) {
    Awaitility.await("should deploy processes and import in Operate")
        .atMost(Duration.ofSeconds(15))
        .ignoreExceptions() // Ignore exceptions and continue retrying
        .untilAsserted(
            () -> {
              final var result = camundaClient.newProcessDefinitionSearchRequest().send().join();
              assertThat(result.items().size()).isEqualTo(expectedCount);
            });
  }
}
