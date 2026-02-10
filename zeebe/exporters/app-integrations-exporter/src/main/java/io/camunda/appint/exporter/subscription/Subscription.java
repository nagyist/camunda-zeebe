/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.appint.exporter.subscription;

import io.camunda.appint.exporter.config.BatchConfig;
import io.camunda.appint.exporter.dispatch.DispatcherImpl;
import io.camunda.appint.exporter.mapper.RecordMapper;
import io.camunda.appint.exporter.transport.Transport;
import io.camunda.zeebe.protocol.record.Record;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Subscription<T> {

  private final Logger log = LoggerFactory.getLogger(getClass().getPackageName());
  private final Transport<T> transport;
  private final RecordMapper<T> mapper;
  private final ReentrantLock lock = new ReentrantLock();
  private final DispatcherImpl dispatcher;
  private final BatchConfig batchConfig;
  private final Consumer<Long> positionConsumer;

  private Batch<T> currentBatch;

  public Subscription(
      final Transport<T> transport,
      final RecordMapper<T> mapper,
      final BatchConfig batchConfig,
      final Consumer<Long> positionConsumer) {
    this.transport = transport;
    this.mapper = mapper;
    this.batchConfig = batchConfig;
    this.positionConsumer = positionConsumer;
    if (batchConfig.continueOnError()) {
      log.warn(
          "Subscription is configured to continue on error. This may lead to data loss if errors occur during export.");
    }
    dispatcher = new DispatcherImpl(batchConfig.maxBatchesInFlight());
    currentBatch = new Batch<>(batchConfig.batchSize(), batchConfig.batchIntervalMs());
  }

  public void exportRecord(final Record<?> record) {
    if (mapper.supports(record)) {
      // Record matches the filter criteria, we can add it to the batch
      batchRecord(record);
    } else if (!hasBatchesInFlight()) {
      // An empty batch allows us to save the exported record position
      positionConsumer.accept(record.getPosition());
    }
  }

  private void batchRecord(final Record<?> record) {
    lock.lock();
    try {
      verifyAndAddToBatch(record);
    } finally {
      lock.unlock();
    }
  }

  private void verifyAndAddToBatch(final Record<?> record) {
    if (currentBatch.shouldFlush()) {
      flush();
    }

    if (currentBatch.addRecord(record, this::mapForBatch) && currentBatch.shouldFlush()) {
      flush();
    }
  }

  private T mapForBatch(final Record<?> record) {
    return mapper.map(record);
  }

  public void attemptFlush() {
    if (lock.tryLock()) {
      if (currentBatch.shouldFlush()) {
        try {
          flush();
        } finally {
          lock.unlock();
        }
      }
    }
  }

  private void flush() {
    final var lastPosition = currentBatch.getLastLogPosition();
    final var entries = currentBatch.getEntries();

    dispatcher.dispatch(
        () -> {
          try {
            transport.send(entries);
          } catch (final Exception e) {
            if (batchConfig.continueOnError()) {
              log.warn(
                  "Error during transport of batch. Will continue with next batch. This may lead to data loss.",
                  e);
            } else {
              log.debug("Error during transport of batch. Will retry with next attempt.", e);
              throw e;
            }
          }
          positionConsumer.accept(lastPosition);
        });

    currentBatch = new Batch<>(batchConfig.batchSize(), batchConfig.batchIntervalMs());
  }

  public Batch<T> getBatch() {
    return currentBatch;
  }

  public boolean isActive() {
    return dispatcher.isActive();
  }

  public void close() {
    transport.close();
    dispatcher.close();
  }

  public boolean hasBatchesInFlight() {
    return !currentBatch.isEmpty() || dispatcher.isActive();
  }
}
