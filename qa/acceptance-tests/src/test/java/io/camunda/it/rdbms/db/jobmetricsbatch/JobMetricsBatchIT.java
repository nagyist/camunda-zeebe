/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.rdbms.db.jobmetricsbatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

import io.camunda.db.rdbms.RdbmsService;
import io.camunda.db.rdbms.write.RdbmsWriters;
import io.camunda.db.rdbms.write.domain.JobMetricsBatchDbModel;
import io.camunda.db.rdbms.write.service.JobMetricsBatchWriter;
import io.camunda.it.rdbms.db.fixtures.CommonFixtures;
import io.camunda.it.rdbms.db.util.CamundaRdbmsInvocationContextProviderExtension;
import io.camunda.it.rdbms.db.util.CamundaRdbmsTestApplication;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@Tag("rdbms")
@ExtendWith(CamundaRdbmsInvocationContextProviderExtension.class)
public class JobMetricsBatchIT {

  private static final Long PARTITION_ID = 0L;
  private static final OffsetDateTime NOW = OffsetDateTime.now();
  private static final OffsetDateTime NOW_MINUS_5M = NOW.minusMinutes(5);
  private static final OffsetDateTime NOW_MINUS_10M = NOW.minusMinutes(10);
  private static final OffsetDateTime NOW_MINUS_3Y = NOW.minusYears(3);
  private static final String TENANT1 = "tenant1";
  private static final String TENANT2 = "tenant2";
  private static final String JOB_TYPE_1 = "jobType1";
  private static final String JOB_TYPE_2 = "jobType2";
  private static final String WORKER_1 = "worker1";
  private static final String WORKER_2 = "worker2";

  private CamundaRdbmsTestApplication testApplication;
  private RdbmsWriters rdbmsWriters;
  private JobMetricsBatchWriter jobMetricsBatchWriter;

  private JobMetricsBatchDbModel writeJobMetricsBatch(
      final JobMetricsBatchWriter jobMetricsBatchWriter,
      final OffsetDateTime time,
      final String tenantId,
      final String jobType,
      final String worker,
      final int failedCount,
      final int completedCount,
      final int createdCount,
      final boolean incompleteBatch) {
    final var model =
        new JobMetricsBatchDbModel.Builder()
            .key(CommonFixtures.nextStringKey())
            .startTime(time)
            .endTime(time)
            .tenantId(tenantId)
            .jobType(jobType)
            .worker(worker)
            .failedCount(failedCount)
            .lastFailedAt(failedCount > 0 ? time : null)
            .completedCount(completedCount)
            .lastCompletedAt(completedCount > 0 ? time : null)
            .createdCount(createdCount)
            .lastCreatedAt(createdCount > 0 ? time : null)
            .incompleteBatch(incompleteBatch)
            .build();
    jobMetricsBatchWriter.create(model);
    return model;
  }

  @BeforeEach
  void setUp() {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();
    rdbmsWriters = rdbmsService.createWriter(PARTITION_ID);
    jobMetricsBatchWriter = rdbmsWriters.getJobMetricsBatchWriter();
  }

  @AfterEach
  void tearDown() {
    jobMetricsBatchWriter.cleanupMetrics(NOW.plusDays(1), Integer.MAX_VALUE);
  }

  @TestTemplate
  public void shouldInsertJobMetricsBatch() {
    // given
    final var model =
        writeJobMetricsBatch(
            jobMetricsBatchWriter, NOW, TENANT1, JOB_TYPE_1, WORKER_1, 5, 10, 15, false);
    rdbmsWriters.flush();

    // then
    assertThat(model.key()).isNotNull();
    assertThat(model.tenantId()).isEqualTo(TENANT1);
    assertThat(model.jobType()).isEqualTo(JOB_TYPE_1);
    assertThat(model.worker()).isEqualTo(WORKER_1);
    assertThat(model.failedCount()).isEqualTo(5);
    assertThat(model.completedCount()).isEqualTo(10);
    assertThat(model.createdCount()).isEqualTo(15);
    assertThat(model.incompleteBatch()).isFalse();
  }

  @TestTemplate
  public void shouldInsertMultipleJobMetricsBatches() {
    // given
    writeJobMetricsBatch(jobMetricsBatchWriter, NOW, TENANT1, JOB_TYPE_1, WORKER_1, 1, 2, 3, false);
    writeJobMetricsBatch(
        jobMetricsBatchWriter, NOW_MINUS_5M, TENANT1, JOB_TYPE_2, WORKER_2, 4, 5, 6, false);
    writeJobMetricsBatch(
        jobMetricsBatchWriter, NOW_MINUS_10M, TENANT2, JOB_TYPE_1, WORKER_1, 7, 8, 9, true);

    // when
    rdbmsWriters.flush();

    // when - then
    assertThatCode(rdbmsWriters::flush).doesNotThrowAnyException();
  }

  @TestTemplate
  public void shouldInsertJobMetricsBatchWithDifferentTenants() {
    // given
    writeJobMetricsBatch(
        jobMetricsBatchWriter, NOW, TENANT1, JOB_TYPE_1, WORKER_1, 5, 10, 15, false);
    writeJobMetricsBatch(jobMetricsBatchWriter, NOW, TENANT2, JOB_TYPE_1, WORKER_1, 3, 6, 9, false);
    rdbmsWriters.flush();

    // when - then
    assertThatCode(rdbmsWriters::flush).doesNotThrowAnyException();
  }

  @TestTemplate
  public void shouldInsertJobMetricsBatchWithIncompleteBatch() {
    // given
    final var model =
        writeJobMetricsBatch(
            jobMetricsBatchWriter, NOW, TENANT1, JOB_TYPE_1, WORKER_1, 5, 10, 15, true);
    rdbmsWriters.flush();

    // then
    assertThat(model.incompleteBatch()).isTrue();
  }

  @TestTemplate
  public void shouldInsertJobMetricsBatchWithNullLastUpdatedTimes() {
    // given - create a batch with zero counts, so no lastUpdatedAt times
    final var model =
        new JobMetricsBatchDbModel.Builder()
            .key(CommonFixtures.nextStringKey())
            .startTime(NOW)
            .endTime(NOW)
            .tenantId(TENANT1)
            .jobType(JOB_TYPE_1)
            .worker(WORKER_1)
            .failedCount(0)
            .lastFailedAt(null)
            .completedCount(0)
            .lastCompletedAt(null)
            .createdCount(0)
            .lastCreatedAt(null)
            .incompleteBatch(false)
            .build();
    jobMetricsBatchWriter.create(model);
    rdbmsWriters.flush();

    // then
    assertThat(model.lastFailedAt()).isNull();
    assertThat(model.lastCompletedAt()).isNull();
    assertThat(model.lastCreatedAt()).isNull();
  }

  @TestTemplate
  public void shouldCleanupJobMetricsBatchesProperly() {
    // given
    writeJobMetricsBatch(
        jobMetricsBatchWriter, NOW, TENANT1, JOB_TYPE_1, WORKER_1, 5, 10, 15, false);
    writeJobMetricsBatch(
        jobMetricsBatchWriter, NOW, TENANT1, JOB_TYPE_2, WORKER_1, 5, 10, 15, false);
    writeJobMetricsBatch(
        jobMetricsBatchWriter, NOW_MINUS_3Y, TENANT2, JOB_TYPE_1, WORKER_1, 3, 6, 9, false);
    writeJobMetricsBatch(
        jobMetricsBatchWriter, NOW_MINUS_3Y, TENANT2, JOB_TYPE_2, WORKER_2, 1, 2, 3, false);
    rdbmsWriters.flush();

    // when - cleanup records older than 2 years
    Awaitility.await("should cleanup old job metrics batches")
        .atMost(Duration.ofSeconds(4))
        .ignoreExceptions()
        .untilAsserted(
            () -> {
              final int deletedCount = jobMetricsBatchWriter.cleanupMetrics(NOW.minusYears(2), 100);
              assertThat(deletedCount).isEqualTo(2);
            });
  }

  @TestTemplate
  public void shouldCleanupJobMetricsBatchesWithLimit() {
    // given - create 5 old records
    for (int i = 0; i < 5; i++) {
      writeJobMetricsBatch(
          jobMetricsBatchWriter, NOW_MINUS_3Y, TENANT1, JOB_TYPE_1 + i, WORKER_1, 1, 2, 3, false);
    }
    rdbmsWriters.flush();

    // when - cleanup with limit of 3
    Awaitility.await("should cleanup limited job metrics batches")
        .atMost(Duration.ofSeconds(4))
        .ignoreExceptions()
        .untilAsserted(
            () -> {
              final int deletedCount = jobMetricsBatchWriter.cleanupMetrics(NOW.minusYears(2), 3);
              assertThat(deletedCount).isEqualTo(3);
            });
  }

  @TestTemplate
  public void shouldNotCleanupRecentJobMetricsBatches() {
    // given - create only recent records
    writeJobMetricsBatch(
        jobMetricsBatchWriter, NOW, TENANT1, JOB_TYPE_1, WORKER_1, 5, 10, 15, false);
    writeJobMetricsBatch(
        jobMetricsBatchWriter, NOW_MINUS_5M, TENANT1, JOB_TYPE_2, WORKER_2, 3, 6, 9, false);
    rdbmsWriters.flush();

    // when - cleanup records older than 2 years (none should be affected)
    final int deletedCount = jobMetricsBatchWriter.cleanupMetrics(NOW.minusYears(2), 100);

    // then
    assertThat(deletedCount).isEqualTo(0);
  }

  @TestTemplate
  public void shouldInsertJobMetricsBatchWithAllFields() {
    // given
    final var startTime = NOW.minusMinutes(30);
    final var endTime = NOW;
    final var model =
        new JobMetricsBatchDbModel.Builder()
            .key(CommonFixtures.nextStringKey())
            .startTime(startTime)
            .endTime(endTime)
            .tenantId(TENANT1)
            .jobType(JOB_TYPE_1)
            .worker(WORKER_1)
            .failedCount(100)
            .lastFailedAt(NOW.minusMinutes(5))
            .completedCount(200)
            .lastCompletedAt(NOW.minusMinutes(3))
            .createdCount(300)
            .lastCreatedAt(NOW.minusMinutes(1))
            .incompleteBatch(false)
            .build();
    jobMetricsBatchWriter.create(model);
    rdbmsWriters.flush();

    // then
    assertThat(model.startTime()).isEqualTo(startTime);
    assertThat(model.endTime()).isEqualTo(endTime);
    assertThat(model.failedCount()).isEqualTo(100);
    assertThat(model.completedCount()).isEqualTo(200);
    assertThat(model.createdCount()).isEqualTo(300);
    assertThat(model.lastFailedAt()).isNotNull();
    assertThat(model.lastCompletedAt()).isNotNull();
    assertThat(model.lastCreatedAt()).isNotNull();
  }

  @TestTemplate
  public void shouldHandleBatchWithOnlyFailedJobs() {
    // given
    final var model =
        writeJobMetricsBatch(
            jobMetricsBatchWriter,
            NOW,
            TENANT1,
            JOB_TYPE_1,
            WORKER_1,
            10, // failedCount
            0, // completedCount
            0, // createdCount
            false);
    rdbmsWriters.flush();

    // then
    assertThat(model.failedCount()).isEqualTo(10);
    assertThat(model.completedCount()).isZero();
    assertThat(model.createdCount()).isZero();
    assertThat(model.lastFailedAt()).isNotNull();
    assertThat(model.lastCompletedAt()).isNull();
    assertThat(model.lastCreatedAt()).isNull();
  }

  @TestTemplate
  public void shouldHandleBatchWithOnlyCompletedJobs() {
    // given
    final var model =
        writeJobMetricsBatch(
            jobMetricsBatchWriter,
            NOW,
            TENANT1,
            JOB_TYPE_1,
            WORKER_1,
            0, // failedCount
            20, // completedCount
            0, // createdCount
            false);
    rdbmsWriters.flush();

    // then
    assertThat(model.failedCount()).isZero();
    assertThat(model.completedCount()).isEqualTo(20);
    assertThat(model.createdCount()).isZero();
    assertThat(model.lastFailedAt()).isNull();
    assertThat(model.lastCompletedAt()).isNotNull();
    assertThat(model.lastCreatedAt()).isNull();
  }

  @TestTemplate
  public void shouldHandleBatchWithOnlyCreatedJobs() {
    // given
    final var model =
        writeJobMetricsBatch(
            jobMetricsBatchWriter,
            NOW,
            TENANT1,
            JOB_TYPE_1,
            WORKER_1,
            0, // failedCount
            0, // completedCount
            30, // createdCount
            false);
    rdbmsWriters.flush();

    // then
    assertThat(model.failedCount()).isZero();
    assertThat(model.completedCount()).isZero();
    assertThat(model.createdCount()).isEqualTo(30);
    assertThat(model.lastFailedAt()).isNull();
    assertThat(model.lastCompletedAt()).isNull();
    assertThat(model.lastCreatedAt()).isNotNull();
  }
}
