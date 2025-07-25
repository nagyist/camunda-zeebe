/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.tasklist.webapp.util;

import io.camunda.client.api.command.ClientException;
import io.camunda.client.api.command.ProblemException;
import io.camunda.service.exception.ServiceException;
import io.camunda.service.exception.ServiceException.Status;
import java.net.SocketTimeoutException;

public abstract class ErrorHandlingUtils {

  public static final String INVALID_STATE = "INVALID_STATE";
  public static final String TASK_ALREADY_ASSIGNED = "TASK_ALREADY_ASSIGNED";
  public static final String TASK_IS_NOT_ACTIVE = "TASK_IS_NOT_ACTIVE";
  public static final String TASK_NOT_ASSIGNED = "TASK_NOT_ASSIGNED";
  public static final String TASK_NOT_ASSIGNED_TO_CURRENT_USER =
      "TASK_NOT_ASSIGNED_TO_CURRENT_USER";
  public static final String TASK_PROCESSING_TIMEOUT = "TASK_PROCESSING_TIMEOUT";
  public static final String TIMEOUT_ERROR_MESSAGE =
      "The request timed out while processing the task.";

  public static String getErrorMessageFromServiceException(final ServiceException exception) {
    return switch (exception.getStatus()) {
      case Status.DEADLINE_EXCEEDED ->
          createErrorMessage(TASK_PROCESSING_TIMEOUT, TIMEOUT_ERROR_MESSAGE);
      case Status.INVALID_STATE ->
          createErrorMessage(Status.INVALID_STATE.name(), exception.getMessage());
      default -> exception.getMessage();
    };
  }

  public static String getErrorMessageFromClientException(final ClientException exception) {
    if (exception instanceof final ProblemException problemException
        && problemException.details().getTitle().equals(INVALID_STATE)) {
      return createErrorMessage(
          problemException.details().getTitle(), problemException.details().getDetail());
    } else if (exception.getCause() != null
        && exception.getCause().getCause() instanceof SocketTimeoutException) {
      return createErrorMessage(TASK_PROCESSING_TIMEOUT, TIMEOUT_ERROR_MESSAGE);
    }
    return exception.getMessage();
  }

  public static String createErrorMessage(final String title, final String detail) {
    return String.format(
        """
      { "title": "%s",
        "detail": "%s"
      }
      """,
        title, detail);
  }
}
