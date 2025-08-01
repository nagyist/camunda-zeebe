/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.webapp.elasticsearch.tenant;

import static io.camunda.webapps.schema.descriptors.IndexTemplateDescriptor.TENANT_ID;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

import io.camunda.operate.conditions.ElasticsearchCondition;
import io.camunda.operate.tenant.TenantCheckApplier;
import io.camunda.operate.util.ElasticsearchUtil;
import io.camunda.operate.webapp.security.tenant.TenantService;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Conditional(ElasticsearchCondition.class)
@Component
public class ElasticsearchTenantCheckApplier implements TenantCheckApplier<SearchRequest> {

  @Autowired private TenantService tenantService;

  @Override
  public SearchRequest apply(final SearchRequest searchRequest) {
    final var tenantAccess = tenantService.getAuthenticatedTenants();
    final var tenantIds = tenantAccess.tenantIds();

    final QueryBuilder finalQuery;
    final var actualQuery = searchRequest.source().query();

    if (tenantAccess.denied()) {
      finalQuery = ElasticsearchUtil.createMatchNoneQuery();

    } else if (tenantAccess.wildcard()) {
      finalQuery = actualQuery;

    } else {
      final var tenantTermsQuery = termsQuery(TENANT_ID, tenantIds);
      finalQuery = ElasticsearchUtil.joinWithAnd(tenantTermsQuery, actualQuery);
    }

    // replace query with final query
    searchRequest.source().query(finalQuery);
    return searchRequest;
  }
}
