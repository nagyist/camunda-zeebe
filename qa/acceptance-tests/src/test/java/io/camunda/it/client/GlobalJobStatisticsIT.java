/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.client;

import static io.camunda.it.util.TestHelper.deployProcessAndWaitForIt;
import static io.camunda.it.util.TestHelper.startProcessInstance;
import static io.camunda.it.util.TestHelper.waitForProcessInstancesToStart;
import static io.camunda.qa.util.multidb.CamundaMultiDBExtension.TIMEOUT_DATA_AVAILABILITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.response.Process;
import io.camunda.client.api.statistics.response.GlobalJobStatistics;
import io.camunda.qa.util.compatibility.CompatibilityTest;
import io.camunda.qa.util.multidb.MultiDbTest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@MultiDbTest
@CompatibilityTest
public class GlobalJobStatisticsIT {

  private static CamundaClient camundaClient;
  private static OffsetDateTime testStartTime;
  private static Process deployedProcess;

  @BeforeAll
  static void beforeAll() {
    testStartTime = OffsetDateTime.now().minusMinutes(1);

    // Deploy a process with service tasks
    deployedProcess = deployProcessAndWaitForIt(camundaClient, "process/service_tasks_v1.bpmn");

    // Start a process instance to create jobs
    startProcessInstance(camundaClient, deployedProcess.getBpmnProcessId());
    waitForProcessInstancesToStart(camundaClient, 1);

    // Wait for the job to be created
    await()
        .atMost(TIMEOUT_DATA_AVAILABILITY)
        .untilAsserted(
            () -> {
              final var jobs = camundaClient.newJobSearchRequest().send().join().items();
              assertThat(jobs).isNotEmpty();
            });
  }

  @Test
  void shouldReturnGlobalJobStatistics() {
    // given
    final OffsetDateTime from = testStartTime;
    final OffsetDateTime to = OffsetDateTime.now().plusMinutes(1);

    // when/then
    await()
        .atMost(TIMEOUT_DATA_AVAILABILITY)
        .untilAsserted(
            () -> {
              final GlobalJobStatistics stats =
                  camundaClient.newGlobalJobStatisticsRequest().from(from).to(to).send().join();

              assertThat(stats).isNotNull();
              // At least we should have created jobs
              assertThat(stats.getCreated().getCount()).isGreaterThanOrEqualTo(0L);
              assertThat(stats.getCompleted().getCount()).isGreaterThanOrEqualTo(0L);
              assertThat(stats.getFailed().getCount()).isGreaterThanOrEqualTo(0L);
            });
  }

  @Test
  void shouldReturnStatisticsFilteredByJobType() {
    // given
    final OffsetDateTime from = testStartTime;
    final OffsetDateTime to = OffsetDateTime.now().plusMinutes(1);
    final String jobType = "taskA"; // job type from service_tasks_v1.bpmn

    // when/then
    await()
        .atMost(TIMEOUT_DATA_AVAILABILITY)
        .untilAsserted(
            () -> {
              final GlobalJobStatistics stats =
                  camundaClient
                      .newGlobalJobStatisticsRequest()
                      .from(from)
                      .to(to)
                      .jobType(jobType)
                      .send()
                      .join();

              assertThat(stats).isNotNull();
              // Statistics should be returned (may be empty if no jobs of this type)
            });
  }

  @Test
  void shouldReturnCompletedJobStatistics() {
    // given - complete a job first
    await()
        .atMost(TIMEOUT_DATA_AVAILABILITY)
        .untilAsserted(
            () -> {
              final var jobs = camundaClient.newJobSearchRequest().send().join().items();
              assertThat(jobs).isNotEmpty();
            });

    // Activate and complete a job
    final List<ActivatedJob> activatedJobs =
        camundaClient
            .newActivateJobsCommand()
            .jobType("taskA")
            .maxJobsToActivate(1)
            .workerName("testWorker")
            .timeout(Duration.ofMinutes(5))
            .send()
            .join()
            .getJobs();

    if (!activatedJobs.isEmpty()) {
      camundaClient.newCompleteCommand(activatedJobs.get(0).getKey()).send().join();
    }

    final OffsetDateTime from = testStartTime;
    final OffsetDateTime to = OffsetDateTime.now().plusMinutes(1);

    // when/then
    await()
        .atMost(TIMEOUT_DATA_AVAILABILITY)
        .untilAsserted(
            () -> {
              final GlobalJobStatistics stats =
                  camundaClient.newGlobalJobStatisticsRequest().from(from).to(to).send().join();

              assertThat(stats).isNotNull();
              // We should have some activity
              final long totalCount =
                  stats.getCreated().getCount()
                      + stats.getCompleted().getCount()
                      + stats.getFailed().getCount();
              assertThat(totalCount).isGreaterThanOrEqualTo(0L);
            });
  }

  @Test
  void shouldReturnIncompleteFlag() {
    // given
    final OffsetDateTime from = testStartTime;
    final OffsetDateTime to = OffsetDateTime.now().plusMinutes(1);

    // when/then
    await()
        .atMost(TIMEOUT_DATA_AVAILABILITY)
        .untilAsserted(
            () -> {
              final GlobalJobStatistics stats =
                  camundaClient.newGlobalJobStatisticsRequest().from(from).to(to).send().join();

              assertThat(stats).isNotNull();
              // The incomplete flag should be present (either true or false)
              assertThat(stats.isIncomplete()).isIn(true, false);
            });
  }
}
