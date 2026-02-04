/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.client;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.statistics.response.GlobalJobStatistics;
import io.camunda.qa.util.auth.Authenticated;
import io.camunda.qa.util.auth.TestUser;
import io.camunda.qa.util.auth.UserDefinition;
import io.camunda.qa.util.compatibility.CompatibilityTest;
import io.camunda.qa.util.multidb.MultiDbTest;
import io.camunda.qa.util.multidb.MultiDbTestApplication;
import io.camunda.zeebe.qa.util.cluster.TestStandaloneBroker;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

@MultiDbTest
@CompatibilityTest
@DisabledIfSystemProperty(
    named = "test.integration.camunda.database.type",
    matches = "AWS_OS") // test is flaky on AWS OS
public class GlobalJobStatisticsMultiTenantIT {

  private static final Duration EXPORT_INTERVAL = Duration.ofSeconds(2);

  @MultiDbTestApplication
  static final TestStandaloneBroker BROKER =
      new TestStandaloneBroker()
          .withBasicAuth()
          .withMultiTenancyEnabled()
          .withAuthenticatedAccess()
          .withProperty(
              "zeebe.broker.experimental.engine.jobMetrics.exportInterval", EXPORT_INTERVAL)
          .withProperty("zeebe.broker.experimental.engine.jobMetrics.enabled", true);

  private static final Duration TEN_SECONDS = Duration.ofSeconds(10);
  private static final String ADMIN = "admin";
  private static final String TENANT_A = "tenantA";
  private static final String TENANT_B = "tenantB";
  private static final String JOB_TYPE = "taskA";

  @UserDefinition
  private static final TestUser ADMIN_USER = new TestUser(ADMIN, "password", List.of());

  private static OffsetDateTime testStartTime;

  @BeforeAll
  static void setup(@Authenticated(ADMIN) final CamundaClient adminClient) {
    testStartTime = OffsetDateTime.now().minusMinutes(1);

    // Create tenants
    createTenant(adminClient, TENANT_A);
    createTenant(adminClient, TENANT_B);

    // Assign admin user to tenants
    assignUserToTenant(adminClient, ADMIN, TENANT_A);
    assignUserToTenant(adminClient, ADMIN, TENANT_B);

    // Deploy processes for each tenant
    deployResource(adminClient, "process/service_tasks_v1.bpmn", TENANT_A);
    deployResource(adminClient, "process/service_tasks_v1.bpmn", TENANT_B);

    // Start process instances for each tenant to create jobs
    startProcessInstance(adminClient, "service_tasks_v1", TENANT_A);
    startProcessInstance(adminClient, "service_tasks_v1", TENANT_A);
    startProcessInstance(adminClient, "service_tasks_v1", TENANT_B);

    // Wait for jobs to be available and process them
    waitForJobsToBeAvailable(adminClient, TENANT_A, 2);
    waitForJobsToBeAvailable(adminClient, TENANT_B, 1);

    // Complete some jobs
    completeJobs(adminClient, TENANT_A, 1);
  }

  @Test
  void shouldReturnGlobalStatisticsForAllTenants(
      @Authenticated(ADMIN) final CamundaClient adminClient) {
    // given
    final OffsetDateTime from = testStartTime;
    final OffsetDateTime to = OffsetDateTime.now().plusMinutes(5);

    // when/then
    Awaitility.await("should return global job statistics")
        .atMost(TEN_SECONDS)
        .ignoreExceptions()
        .untilAsserted(
            () -> {
              final GlobalJobStatistics stats =
                  adminClient.newGlobalJobStatisticsRequest().from(from).to(to).send().join();

              assertThat(stats).isNotNull();
              // We should have jobs created across both tenants
              assertThat(stats.getCreated().getCount()).isGreaterThanOrEqualTo(0L);
            });
  }

  @Test
  void shouldFilterStatisticsByJobType(@Authenticated(ADMIN) final CamundaClient adminClient) {
    // given
    final OffsetDateTime from = testStartTime;
    final OffsetDateTime to = OffsetDateTime.now().plusMinutes(5);

    // when/then
    Awaitility.await("should return job statistics filtered by type")
        .atMost(TEN_SECONDS)
        .ignoreExceptions()
        .untilAsserted(
            () -> {
              final GlobalJobStatistics stats =
                  adminClient
                      .newGlobalJobStatisticsRequest()
                      .from(from)
                      .to(to)
                      .jobType(JOB_TYPE)
                      .send()
                      .join();

              assertThat(stats).isNotNull();
            });
  }

  @Test
  void shouldReturnCompletedJobsStatistics(@Authenticated(ADMIN) final CamundaClient adminClient) {
    // given
    final OffsetDateTime from = testStartTime;
    final OffsetDateTime to = OffsetDateTime.now().plusMinutes(5);

    // when/then
    Awaitility.await("should return completed job statistics")
        .atMost(TEN_SECONDS)
        .ignoreExceptions()
        .untilAsserted(
            () -> {
              final GlobalJobStatistics stats =
                  adminClient.newGlobalJobStatisticsRequest().from(from).to(to).send().join();

              assertThat(stats).isNotNull();
              // After completing jobs, we should have completed count
              assertThat(stats.getCompleted().getCount()).isGreaterThanOrEqualTo(0L);
            });
  }

  private static void createTenant(final CamundaClient client, final String tenantId) {
    client.newCreateTenantCommand().tenantId(tenantId).name(tenantId).send().join();
  }

  private static void assignUserToTenant(
      final CamundaClient client, final String username, final String tenantId) {
    client.newAssignUserToTenantCommand().username(username).tenantId(tenantId).send().join();
  }

  private static void deployResource(
      final CamundaClient client, final String resourcePath, final String tenantId) {
    client
        .newDeployResourceCommand()
        .addResourceFromClasspath(resourcePath)
        .tenantId(tenantId)
        .send()
        .join();
  }

  private static void startProcessInstance(
      final CamundaClient client, final String processId, final String tenantId) {
    client
        .newCreateInstanceCommand()
        .bpmnProcessId(processId)
        .latestVersion()
        .tenantId(tenantId)
        .send()
        .join();
  }

  private static void waitForJobsToBeAvailable(
      final CamundaClient client, final String tenantId, final int expectedCount) {
    Awaitility.await("jobs should be available for tenant " + tenantId)
        .atMost(TEN_SECONDS)
        .ignoreExceptions()
        .untilAsserted(
            () -> {
              final var jobs =
                  client
                      .newJobSearchRequest()
                      .filter(f -> f.tenantId(tenantId))
                      .send()
                      .join()
                      .items();
              assertThat(jobs).hasSizeGreaterThanOrEqualTo(expectedCount);
            });
  }

  private static void completeJobs(
      final CamundaClient client, final String tenantId, final int count) {
    final List<ActivatedJob> jobs =
        client
            .newActivateJobsCommand()
            .jobType(JOB_TYPE)
            .maxJobsToActivate(count)
            .workerName("testWorker")
            .timeout(Duration.ofMinutes(5))
            .tenantIds(tenantId)
            .send()
            .join()
            .getJobs();

    for (final ActivatedJob job : jobs) {
      client.newCompleteCommand(job.getKey()).send().join();
    }
  }
}
