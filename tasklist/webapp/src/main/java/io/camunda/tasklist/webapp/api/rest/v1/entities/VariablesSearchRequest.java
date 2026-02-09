/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.tasklist.webapp.api.rest.v1.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

@Schema(description = "Request object to search tasks variables by provided variable names.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VariablesSearchRequest {
  @ArraySchema(arraySchema = @Schema(description = "Names of variables to find."))
  private List<String> variableNames = new ArrayList<>();

  @ArraySchema(
      arraySchema =
          @Schema(
              description = "An array of variable names that should be included in the response."))
  private List<IncludeVariable> includeVariables = new ArrayList<>();

  public List<String> getVariableNames() {
    return variableNames;
  }

  public VariablesSearchRequest setVariableNames(final List<String> variableNames) {
    this.variableNames = variableNames;
    return this;
  }

  public List<IncludeVariable> getIncludeVariables() {
    return includeVariables;
  }

  public VariablesSearchRequest setIncludeVariables(final List<IncludeVariable> includeVariables) {
    this.includeVariables = includeVariables;
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(variableNames, includeVariables);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final VariablesSearchRequest that = (VariablesSearchRequest) o;
    return Objects.equals(variableNames, that.variableNames)
        && Objects.equals(includeVariables, that.includeVariables);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", VariablesSearchRequest.class.getSimpleName() + "[", "]")
        .add("variableNames=" + variableNames)
        .add("includeVariables=" + includeVariables)
        .toString();
  }
}
