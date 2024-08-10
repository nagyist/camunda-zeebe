/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.exceptions;

public class OptimizeException extends Exception {

  public OptimizeException() {
    super();
  }

  public OptimizeException(String detailedErrorMessage) {
    super(detailedErrorMessage);
  }

  public OptimizeException(String detailedErrorMessage, Exception e) {
    super(detailedErrorMessage, e);
  }

  public String getErrorCode() {
    return "serverError";
  }
}