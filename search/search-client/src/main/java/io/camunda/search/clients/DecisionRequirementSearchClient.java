/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.search.clients;

import io.camunda.search.entities.DecisionRequirementsEntity;
import io.camunda.search.query.DecisionRequirementsQuery;
import io.camunda.search.query.SearchQueryResult;
import io.camunda.security.auth.SecurityContext;

public interface DecisionRequirementSearchClient {

  DecisionRequirementsEntity getDecisionRequirements(final long key, final boolean includeXml);

  SearchQueryResult<DecisionRequirementsEntity> searchDecisionRequirements(
      DecisionRequirementsQuery filter);

  DecisionRequirementSearchClient withSecurityContext(SecurityContext securityContext);
}
