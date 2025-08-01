/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.tasklist.webapp.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.camunda.security.reader.TenantAccess;
import io.camunda.tasklist.util.ElasticsearchUtil;
import io.camunda.tasklist.webapp.es.tenant.ElasticsearchTenantCheckApplier;
import io.camunda.tasklist.webapp.tenant.TenantService;
import java.util.List;
import org.elasticsearch.action.search.SearchRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ElasticSearchTenantCheckApplierTest {

  @Mock private TenantService tenantService;

  @InjectMocks private ElasticsearchTenantCheckApplier instance;

  @Test
  void checkIfQueryContainsTenant() {
    // given
    final SearchRequest searchRequest = new SearchRequest("TaskTest");
    searchRequest.source().query(termsQuery("test", "1"));
    final var tenantAccess = mock(TenantAccess.class);
    final List<String> authorizedTenant = List.of("TenantA", "TenantB");
    when(tenantAccess.allowed()).thenReturn(true);
    when(tenantAccess.tenantIds()).thenReturn(authorizedTenant);
    final String queryResult =
        "{\n"
            + "  \"bool\" : {\n"
            + "    \"must\" : [\n"
            + "      {\n"
            + "        \"terms\" : {\n"
            + "          \"tenantId\" : [\n"
            + "            \"TenantA\",\n"
            + "            \"TenantB\"\n"
            + "          ],\n"
            + "          \"boost\" : 1.0\n"
            + "        }\n"
            + "      },\n"
            + "      {\n"
            + "        \"terms\" : {\n"
            + "          \"test\" : [\n"
            + "            \"1\"\n"
            + "          ],\n"
            + "          \"boost\" : 1.0\n"
            + "        }\n"
            + "      }\n"
            + "    ],\n"
            + "    \"adjust_pure_negative\" : true,\n"
            + "    \"boost\" : 1.0\n"
            + "  }\n"
            + "}";

    when(tenantService.getAuthenticatedTenants()).thenReturn(tenantAccess);

    // when
    instance.apply(searchRequest);

    // then
    assertThat(searchRequest.source().query().toString()).isEqualTo(queryResult);
  }

  @Test
  void checkIfQueryContainsAccessibleTenantsProvidedByUser() {
    // given
    final SearchRequest searchRequest = new SearchRequest("TaskTest");
    searchRequest.source().query(termsQuery("test", "1"));
    final var tenantAccess = mock(TenantAccess.class);
    final List<String> tenantsProvidedByUser = List.of("TenantA", "TenantC");
    final List<String> authorizedTenant = List.of("TenantA", "TenantB");
    when(tenantAccess.allowed()).thenReturn(true);
    when(tenantAccess.tenantIds()).thenReturn(authorizedTenant);
    final String queryResult =
        "{\n"
            + "  \"bool\" : {\n"
            + "    \"must\" : [\n"
            + "      {\n"
            + "        \"terms\" : {\n"
            + "          \"tenantId\" : [\n"
            + "            \"TenantA\"\n"
            + "          ],\n"
            + "          \"boost\" : 1.0\n"
            + "        }\n"
            + "      },\n"
            + "      {\n"
            + "        \"terms\" : {\n"
            + "          \"test\" : [\n"
            + "            \"1\"\n"
            + "          ],\n"
            + "          \"boost\" : 1.0\n"
            + "        }\n"
            + "      }\n"
            + "    ],\n"
            + "    \"adjust_pure_negative\" : true,\n"
            + "    \"boost\" : 1.0\n"
            + "  }\n"
            + "}";

    when(tenantService.getAuthenticatedTenants()).thenReturn(tenantAccess);

    // when
    instance.apply(searchRequest, tenantsProvidedByUser);

    // then
    assertThat(searchRequest.source().query().toString()).isEqualTo(queryResult);
  }

  @Test
  void checkShouldReturnNoneMatchQueryIfUserProvidedNotAccessibleTenants() {
    // given
    final SearchRequest searchRequest = new SearchRequest("TaskTest");
    searchRequest.source().query(termsQuery("test", "1"));
    final var tenantAccess = mock(TenantAccess.class);
    final List<String> tenantsProvidedByUser = List.of("UnknownTenant");
    final List<String> authorizedTenant = List.of("TenantA", "TenantB");
    when(tenantAccess.tenantIds()).thenReturn(authorizedTenant);
    when(tenantService.getAuthenticatedTenants()).thenReturn(tenantAccess);

    // when
    instance.apply(searchRequest, tenantsProvidedByUser);

    // then
    assertThat(searchRequest.source().query().toString())
        .isEqualTo(ElasticsearchUtil.createMatchNoneQuery().toString());
  }

  @Test
  void checkShouldReturnNoneMatchQueryIfUserHaveNoneTenantsAccess() {
    // given
    final SearchRequest searchRequest = new SearchRequest("TaskTest");
    searchRequest.source().query(termsQuery("test", "1"));
    final var tenantAccess = mock(TenantAccess.class);
    when(tenantAccess.denied()).thenReturn(true);
    when(tenantService.getAuthenticatedTenants()).thenReturn(tenantAccess);

    // when
    instance.apply(searchRequest);

    // then
    assertThat(searchRequest.source().query().toString())
        .isEqualTo(ElasticsearchUtil.createMatchNoneQuery().toString());
  }

  @Test
  void checkThatQueryDontContainTenantWhenMultiTenancyIsTurnedOff() {
    // given
    final SearchRequest searchRequest = new SearchRequest("TaskTest");
    searchRequest.source().query(termsQuery("test", "1"));
    final var tenantAccess = mock(TenantAccess.class);
    when(tenantAccess.wildcard()).thenReturn(true);
    when(tenantService.getAuthenticatedTenants()).thenReturn(tenantAccess);
    final String expectedQueryResult =
        "{\n"
            + "  \"terms\" : {\n"
            + "    \"test\" : [\n"
            + "      \"1\"\n"
            + "    ],\n"
            + "    \"boost\" : 1.0\n"
            + "  }\n"
            + "}";

    // when
    instance.apply(searchRequest);

    // then
    assertThat(searchRequest.source().query().toString()).isEqualTo(expectedQueryResult);
  }
}
