/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.appint.exporter.dispatch;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatcher implementation that uses a single-threaded {@link ThreadPoolExecutor} with a {@link
 * PriorityBlockingQueue} to manage job execution. It ensures that only a specified number of jobs
 * are in-flight at any given time using a {@link Semaphore}. If a job fails, it is re-queued with
 * high priority to be retried before any regular priority jobs.
 */
public class DispatcherImpl implements Dispatcher {

  private final Logger log = LoggerFactory.getLogger(getClass().getPackageName());

  // Single-threaded executor to ensure jobs are executed sequentially, while allowing for
  // prioritization. We go with one thread to make sure batches are sent in order.
  private final PriorityBlockingQueue<Runnable> queue = new PriorityBlockingQueue<>();
  private final ExecutorService executor =
      new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, queue);
  private final Semaphore semaphore;
  private final int maxJobsInFlight;

  public DispatcherImpl(final int maxJobsInFlight) {
    this.maxJobsInFlight = maxJobsInFlight;
    semaphore = new Semaphore(maxJobsInFlight);
  }

  @Override
  public void dispatch(final Runnable job) {
    try {
      semaphore.acquire();
      log.trace("Dispatching job {}", job);
      executor.execute(PriorityJob.regular(() -> executeJob(job)));
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Executes the given job and releases a permit from the semaphore upon completion. If an
   * exception occurs during execution, the job is re-queued with high priority to be retried again.
   *
   * @param job, the job to execute
   */
  private void executeJob(final Runnable job) {
    try {
      job.run();
      semaphore.release();
    } catch (final Exception e) {
      log.debug("Failed to run job, re-queuing with high priority. Error: " + e.getMessage());
      executor.execute(PriorityJob.high(() -> executeJob(job)));
    }
  }

  public boolean isActive() {
    return semaphore.availablePermits() != maxJobsInFlight;
  }

  @Override
  public void close() {
    try {
      executor.shutdown();
      executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public ExecutorService getExecutor() {
    return executor;
  }

  public static void main(final String[] args) {}
}
