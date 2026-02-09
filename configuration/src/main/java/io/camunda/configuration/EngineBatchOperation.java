/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.configuration;

import java.time.Duration;
import java.util.Set;

public class EngineBatchOperation {

  private static final String PREFIX = "camunda.processing.engine.batch-operations";
  private static final Duration DEFAULT_BATCH_OPERATION_SCHEDULER_INTERVAL = Duration.ofSeconds(1);
  // reasonable size of a chunk record to avoid too many or too large records
  private static final int DEFAULT_BATCH_OPERATION_CHUNK_SIZE = 100;
  // key has 8 bytes, stay below 32KB block size
  private static final int DEFAULT_BATCH_OPERATION_DB_CHUNK_SIZE = 3500;
  // ES/OS have max 10000 entities per query
  private static final int DEFAULT_BATCH_OPERATION_QUERY_PAGE_SIZE = 10000;
  // Oracle can only have 1000 elements in `IN` clause
  private static final int DEFAULT_BATCH_OPERATION_QUERY_IN_CLAUSE_SIZE = 1000;
  private static final int DEFAULT_BATCH_OPERATION_QUERY_RETRY_MAX = 0;
  private static final Duration DEFAULT_BATCH_OPERATION_QUERY_RETRY_INITIAL_DELAY =
      Duration.ofSeconds(1);
  private static final Duration DEFAULT_BATCH_OPERATION_QUERY_RETRY_MAX_DELAY =
      Duration.ofSeconds(60);
  private static final int DEFAULT_BATCH_OPERATION_QUERY_RETRY_BACKOFF_FACTOR = 2;

  private static final Set<String> LEGACY_SCHEDULER_INTERVAL_PROPERTIES =
      Set.of("zeebe.broker.experimental.engine.batchOperations.schedulerInterval");

  private static final Set<String> LEGACY_CHUNK_SIZE_PROPERTIES =
      Set.of("zeebe.broker.experimental.engine.batchOperations.chunkSize");

  private static final Set<String> LEGACY_DB_CHUNK_SIZE_PROPERTIES =
      Set.of("zeebe.broker.experimental.engine.batchOperations.dbChunkSize");

  private static final Set<String> LEGACY_QUERY_PAGE_SIZE_PROPERTIES =
      Set.of("zeebe.broker.experimental.engine.batchOperations.queryPageSize");

  private static final Set<String> LEGACY_QUERY_IN_CLAUSE_SIZE_PROPERTIES =
      Set.of("zeebe.broker.experimental.engine.batchOperations.queryInClauseSize");

  private static final Set<String> LEGACY_QUERY_RETRY_MAX_PROPERTIES =
      Set.of("zeebe.broker.experimental.engine.batchOperations.queryRetryMax");

  private static final Set<String> LEGACY_QUERY_RETRY_INITIAL_DELAY_PROPERTIES =
      Set.of("zeebe.broker.experimental.engine.batchOperations.queryRetryInitialDelay");

  private static final Set<String> LEGACY_QUERY_RETRY_MAX_DELAY_PROPERTIES =
      Set.of("zeebe.broker.experimental.engine.batchOperations.queryRetryMaxDelay");

  private static final Set<String> LEGACY_QUERY_RETRY_BACKOFF_FACTOR_PROPERTIES =
      Set.of("zeebe.broker.experimental.engine.batchOperations.queryRetryBackoffFactor");

  /**
   * The interval at which the batch operation scheduler runs. Defaults to {@link
   * #DEFAULT_BATCH_OPERATION_SCHEDULER_INTERVAL}.
   */
  private Duration schedulerInterval = DEFAULT_BATCH_OPERATION_SCHEDULER_INTERVAL;

  /**
   * Number of itemKeys in one BatchOperationChunkRecord. Must be below 4MB total record size.
   * Defaults to {@link #DEFAULT_BATCH_OPERATION_CHUNK_SIZE}.
   */
  private int chunkSize = DEFAULT_BATCH_OPERATION_CHUNK_SIZE;

  /**
   * Number of itemKeys in one PersistedBatchOperationChunk. Must be below 32KB total record size.
   * Defaults to {@link #DEFAULT_BATCH_OPERATION_DB_CHUNK_SIZE}.
   */
  private int dbChunkSize = DEFAULT_BATCH_OPERATION_DB_CHUNK_SIZE;

  /**
   * The page size for batch operation queries. Defaults to {@link
   * #DEFAULT_BATCH_OPERATION_QUERY_PAGE_SIZE}.
   */
  private int queryPageSize = DEFAULT_BATCH_OPERATION_QUERY_PAGE_SIZE;

  /**
   * The size of the IN clause for batch operation queries. Defaults to {@link
   * #DEFAULT_BATCH_OPERATION_QUERY_IN_CLAUSE_SIZE}.
   */
  private int queryInClauseSize = DEFAULT_BATCH_OPERATION_QUERY_IN_CLAUSE_SIZE;

  /**
   * The maximum number of retries for batch operation queries. Defaults to {@link
   * #DEFAULT_BATCH_OPERATION_QUERY_RETRY_MAX}.
   */
  private int queryRetryMax = DEFAULT_BATCH_OPERATION_QUERY_RETRY_MAX;

  /**
   * The initial delay for retrying batch operation queries. Defaults to {@link
   * #DEFAULT_BATCH_OPERATION_QUERY_RETRY_INITIAL_DELAY}.
   */
  private Duration queryRetryInitialDelay = DEFAULT_BATCH_OPERATION_QUERY_RETRY_INITIAL_DELAY;

  /**
   * The maximum delay for retrying batch operation queries. Defaults to {@link
   * #DEFAULT_BATCH_OPERATION_QUERY_RETRY_MAX_DELAY}.
   */
  private Duration queryRetryMaxDelay = DEFAULT_BATCH_OPERATION_QUERY_RETRY_MAX_DELAY;

  /**
   * The backoff factor for retrying batch operation queries. Defaults to {@link
   * #DEFAULT_BATCH_OPERATION_QUERY_RETRY_BACKOFF_FACTOR}.
   */
  private int queryRetryBackoffFactor = DEFAULT_BATCH_OPERATION_QUERY_RETRY_BACKOFF_FACTOR;

  public Duration getSchedulerInterval() {
    return UnifiedConfigurationHelper.validateLegacyConfiguration(
        PREFIX + ".scheduler-interval",
        schedulerInterval,
        Duration.class,
        UnifiedConfigurationHelper.BackwardsCompatibilityMode.SUPPORTED,
        LEGACY_SCHEDULER_INTERVAL_PROPERTIES);
  }

  public void setSchedulerInterval(final Duration schedulerInterval) {
    this.schedulerInterval = schedulerInterval;
  }

  public int getChunkSize() {
    return UnifiedConfigurationHelper.validateLegacyConfiguration(
        PREFIX + ".chunk-size",
        chunkSize,
        Integer.class,
        UnifiedConfigurationHelper.BackwardsCompatibilityMode.SUPPORTED,
        LEGACY_CHUNK_SIZE_PROPERTIES);
  }

  public void setChunkSize(final int chunkSize) {
    this.chunkSize = chunkSize;
  }

  public int getDbChunkSize() {
    return UnifiedConfigurationHelper.validateLegacyConfiguration(
        PREFIX + ".db-chunk-size",
        dbChunkSize,
        Integer.class,
        UnifiedConfigurationHelper.BackwardsCompatibilityMode.SUPPORTED,
        LEGACY_DB_CHUNK_SIZE_PROPERTIES);
  }

  public void setDbChunkSize(final int dbChunkSize) {
    this.dbChunkSize = dbChunkSize;
  }

  public int getQueryPageSize() {
    return UnifiedConfigurationHelper.validateLegacyConfiguration(
        PREFIX + ".query-page-size",
        queryPageSize,
        Integer.class,
        UnifiedConfigurationHelper.BackwardsCompatibilityMode.SUPPORTED,
        LEGACY_QUERY_PAGE_SIZE_PROPERTIES);
  }

  public void setQueryPageSize(final int queryPageSize) {
    this.queryPageSize = queryPageSize;
  }

  public int getQueryInClauseSize() {
    return UnifiedConfigurationHelper.validateLegacyConfiguration(
        PREFIX + ".query-in-clause-size",
        queryInClauseSize,
        Integer.class,
        UnifiedConfigurationHelper.BackwardsCompatibilityMode.SUPPORTED,
        LEGACY_QUERY_IN_CLAUSE_SIZE_PROPERTIES);
  }

  public void setQueryInClauseSize(final int queryInClauseSize) {
    this.queryInClauseSize = queryInClauseSize;
  }

  public int getQueryRetryMax() {
    return UnifiedConfigurationHelper.validateLegacyConfiguration(
        PREFIX + ".query-retry-max",
        queryRetryMax,
        Integer.class,
        UnifiedConfigurationHelper.BackwardsCompatibilityMode.SUPPORTED,
        LEGACY_QUERY_RETRY_MAX_PROPERTIES);
  }

  public void setQueryRetryMax(final int queryRetryMax) {
    this.queryRetryMax = queryRetryMax;
  }

  public Duration getQueryRetryInitialDelay() {
    return UnifiedConfigurationHelper.validateLegacyConfiguration(
        PREFIX + ".query-retry-initial-delay",
        queryRetryInitialDelay,
        Duration.class,
        UnifiedConfigurationHelper.BackwardsCompatibilityMode.SUPPORTED,
        LEGACY_QUERY_RETRY_INITIAL_DELAY_PROPERTIES);
  }

  public void setQueryRetryInitialDelay(final Duration queryRetryInitialDelay) {
    this.queryRetryInitialDelay = queryRetryInitialDelay;
  }

  public Duration getQueryRetryMaxDelay() {
    return UnifiedConfigurationHelper.validateLegacyConfiguration(
        PREFIX + ".query-retry-max-delay",
        queryRetryMaxDelay,
        Duration.class,
        UnifiedConfigurationHelper.BackwardsCompatibilityMode.SUPPORTED,
        LEGACY_QUERY_RETRY_MAX_DELAY_PROPERTIES);
  }

  public void setQueryRetryMaxDelay(final Duration queryRetryMaxDelay) {
    this.queryRetryMaxDelay = queryRetryMaxDelay;
  }

  public int getQueryRetryBackoffFactor() {
    return UnifiedConfigurationHelper.validateLegacyConfiguration(
        PREFIX + ".query-retry-backoff-factor",
        queryRetryBackoffFactor,
        Integer.class,
        UnifiedConfigurationHelper.BackwardsCompatibilityMode.SUPPORTED,
        LEGACY_QUERY_RETRY_BACKOFF_FACTOR_PROPERTIES);
  }

  public void setQueryRetryBackoffFactor(final int queryRetryBackoffFactor) {
    this.queryRetryBackoffFactor = queryRetryBackoffFactor;
  }
}
