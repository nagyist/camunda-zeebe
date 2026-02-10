/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.appint.exporter.dispatch;

/**
 * A job with a priority. The priority is used to determine the order of execution of the jobs. A
 * high priority job will be executed before any regular priority job.
 *
 * <p>To be used with a {@link java.util.concurrent.PriorityBlockingQueue}, which is used by the
 * {@link java.util.concurrent.ThreadPoolExecutor} in the {@link DispatcherImpl}.
 *
 * @param priority
 * @param runnable
 */
record PriorityJob(Priority priority, Runnable runnable)
    implements Runnable, Comparable<PriorityJob> {

  public static PriorityJob high(final Runnable runnable) {
    return new PriorityJob(Priority.HIGH, runnable);
  }

  public static PriorityJob regular(final Runnable runnable) {
    return new PriorityJob(Priority.REGULAR, runnable);
  }

  @Override
  public void run() {
    runnable.run();
  }

  @Override
  public int compareTo(final PriorityJob other) {
    return Integer.compare(priority.value, other.priority.value);
  }

  /** The lower the value, the higher the priority. */
  enum Priority {
    HIGH(0),
    REGULAR(10);

    private final int value;

    Priority(final int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }
}
