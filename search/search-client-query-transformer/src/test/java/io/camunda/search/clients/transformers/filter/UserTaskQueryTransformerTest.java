/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.search.clients.transformers.filter;

import static io.camunda.search.filter.Operation.eq;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.search.clients.query.SearchBoolQuery;
import io.camunda.search.clients.query.SearchExistsQuery;
import io.camunda.search.clients.query.SearchHasChildQuery;
import io.camunda.search.clients.query.SearchHasParentQuery;
import io.camunda.search.clients.query.SearchMatchNoneQuery;
import io.camunda.search.clients.query.SearchQuery;
import io.camunda.search.clients.query.SearchQueryOption;
import io.camunda.search.clients.query.SearchTermQuery;
import io.camunda.search.clients.query.SearchTermsQuery;
import io.camunda.search.clients.query.SearchWildcardQuery;
import io.camunda.search.clients.types.TypedValue;
import io.camunda.search.filter.FilterBuilders;
import io.camunda.search.filter.Operation;
import io.camunda.search.filter.UntypedOperation;
import io.camunda.search.filter.UserTaskFilter;
import io.camunda.search.filter.UserTaskFilter.Builder;
import io.camunda.search.filter.VariableValueFilter;
import io.camunda.search.query.SearchQueryBuilders;
import io.camunda.security.auth.Authorization;
import io.camunda.security.reader.AuthorizationCheck;
import io.camunda.security.reader.ResourceAccessChecks;
import io.camunda.security.reader.TenantCheck;
import io.camunda.webapps.schema.descriptors.template.TaskTemplate;
import io.camunda.webapps.schema.entities.usertask.TaskJoinRelationship.TaskJoinRelationshipType;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class UserTaskQueryTransformerTest extends AbstractTransformerTest {

  @Test
  public void shouldQueryOnlyByUserTasks() {
    // given
    final UserTaskFilter filter = new Builder().build();

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final SearchQueryOption queryVariant = searchRequest.queryOption();

    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (boolQuery) -> {
              assertThat(boolQuery.must())
                  .anySatisfy(
                      query -> assertExistsQuery(query.queryOption(), "flowNodeInstanceId"));
            });

    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertSearchTermQuery(
                  t.must().get(1).queryOption(), "implementation", "ZEEBE_USER_TASK");
            });
  }

  @Test
  public void shouldQueryByUserTaskKey() {
    // given
    final var filter = FilterBuilders.userTask((f) -> f.userTaskKeys(4503599627370497L));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final var queryVariant = searchRequest.queryOption();

    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertSearchTermQuery(t.must().get(0).queryOption(), "key", 4503599627370497L);
            });
  }

  @Test
  public void shouldQueryByTaskState() {
    // given
    final var filter = FilterBuilders.userTask((f) -> f.states("CREATED"));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final var queryVariant = searchRequest.queryOption();
    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertSearchTermQuery(t.must().get(0).queryOption(), "state", "CREATED");
            });
  }

  static Stream<Arguments> provideStateOperations() {
    return Stream.of(
        stateOperationCase(
            Operation.eq("CREATED"), query -> assertSearchTermQuery(query, "state", "CREATED")),
        stateOperationCase(
            Operation.neq("COMPLETED"),
            query ->
                assertThat(query)
                    .isInstanceOfSatisfying(
                        SearchBoolQuery.class,
                        boolQuery ->
                            assertThat(boolQuery.mustNot())
                                .singleElement()
                                .extracting(SearchQuery::queryOption)
                                .satisfies(
                                    mustNotQuery ->
                                        assertSearchTermQuery(
                                            mustNotQuery, "state", "COMPLETED")))),
        stateOperationCase(Operation.exists(true), query -> assertExistsQuery(query, "state")),
        stateOperationCase(
            Operation.exists(false),
            query ->
                assertThat(query)
                    .isInstanceOfSatisfying(
                        SearchBoolQuery.class,
                        boolQuery ->
                            assertThat(boolQuery.mustNot())
                                .singleElement()
                                .extracting(SearchQuery::queryOption)
                                .satisfies(
                                    mustNotQuery -> assertExistsQuery(mustNotQuery, "state")))),
        stateOperationCase(
            Operation.in("CREATING", "UPDATING"),
            query -> assertSearchTermsQuery(query, "state", "CREATING", "UPDATING")),
        stateOperationCase(
            Operation.like("CREAT*"),
            query ->
                assertThat(query)
                    .isInstanceOfSatisfying(
                        SearchWildcardQuery.class,
                        wildcardQuery -> {
                          assertThat(wildcardQuery.field()).isEqualTo("state");
                          assertThat(wildcardQuery.value()).isEqualTo("CREAT*");
                        })));
  }

  private static Arguments stateOperationCase(
      final Operation<String> operation, final Consumer<SearchQueryOption> queryAssertion) {
    return Arguments.of(operation, queryAssertion);
  }

  @ParameterizedTest(name = "[{index}] should map {0}")
  @MethodSource("provideStateOperations")
  @DisplayName("Should transform state operations into correct search query")
  void shouldTransformStateOperationsToSearchQuery(
      final Operation<String> operation, final Consumer<SearchQueryOption> queryAssertion) {

    // given
    final var filter = FilterBuilders.userTask(f -> f.stateOperations(operation));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    assertThat(searchRequest.queryOption())
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            boolQuery -> queryAssertion.accept(boolQuery.must().getFirst().queryOption()));
  }

  @Test
  public void shouldQueryByAssignee() {
    // given
    final var filter = FilterBuilders.userTask((f) -> f.assignees("assignee1"));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final var queryVariant = searchRequest.queryOption();
    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertSearchTermQuery(t.must().get(0).queryOption(), "assignee", "assignee1");
            });
  }

  @Test
  public void shouldQueryByTenantId() {
    // given
    final var filter = FilterBuilders.userTask((f) -> f.tenantIds("tenant1"));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final var queryVariant = searchRequest.queryOption();

    searchRequest.queryOption();
    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertSearchTermQuery(t.must().get(0).queryOption(), "tenantId", "tenant1");
            });
  }

  @Test
  public void shouldQueryByProcessInstanceKey() {
    // given
    final var filter = FilterBuilders.userTask((f) -> f.processInstanceKeys(12345L));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final var queryVariant = searchRequest.queryOption();

    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertSearchTermQuery(t.must().get(0).queryOption(), "processInstanceId", 12345L);
            });
  }

  @Test
  public void shouldQueryByProcessDefinitionKey() {
    // given
    final var filter = FilterBuilders.userTask((f) -> f.processDefinitionKeys(123L));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final var queryVariant = searchRequest.queryOption();

    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertSearchTermQuery(t.must().get(0).queryOption(), "processDefinitionId", 123L);
            });
  }

  @Test
  public void shouldQueryByBpmnProcessId() {
    // given
    final var filter = FilterBuilders.userTask((f) -> f.bpmnProcessIds("bpmnProcess1"));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final var queryVariant = searchRequest.queryOption();

    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertSearchTermQuery(t.must().get(0).queryOption(), "bpmnProcessId", "bpmnProcess1");
            });
  }

  @Test
  public void shouldQueryByCandidateUsers() {
    // given
    final var filter = FilterBuilders.userTask((f) -> f.candidateUsers("candidateUser1"));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final var queryVariant = searchRequest.queryOption();

    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertSearchTermQuery(
                  t.must().get(0).queryOption(), "candidateUsers", "candidateUser1");
            });
  }

  @Test
  public void shouldQueryByElementInstanceKey() {
    // given
    final var filter = FilterBuilders.userTask((f) -> f.elementInstanceKeys(12345L));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final var queryVariant = searchRequest.queryOption();

    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertSearchTermQuery(t.must().get(0).queryOption(), "flowNodeInstanceId", 12345L);
            });
  }

  @Test
  public void shouldQueryByCandidateGroups() {
    // given
    final var filter = FilterBuilders.userTask((f) -> f.candidateGroups("candidateGroup1"));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final var queryVariant = searchRequest.queryOption();

    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertSearchTermQuery(
                  t.must().get(0).queryOption(), "candidateGroups", "candidateGroup1");
            });
  }

  @Test
  public void shouldQueryByProcessInstanceVariableValueFilter() {
    // given
    final VariableValueFilter.Builder variableValueFilterBuilder =
        new VariableValueFilter.Builder();
    variableValueFilterBuilder.name("test").valueOperation(UntypedOperation.of(eq("test"))).build();

    final VariableValueFilter variableFilterValue = variableValueFilterBuilder.build();

    final var filter =
        FilterBuilders.userTask((f) -> f.processInstanceVariables(List.of(variableFilterValue)));
    final var searchQuery = SearchQueryBuilders.userTaskSearchQuery((b) -> b.filter(filter));

    // when
    final var searchRequest = transformQuery(filter);

    // then
    final var queryVariant = searchRequest.queryOption();

    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            outerBoolQuery -> {
              assertThat(outerBoolQuery.must()).isNotEmpty();

              final SearchQuery outerMustQuery = outerBoolQuery.must().get(0);
              assertThat(outerMustQuery.queryOption()).isInstanceOf(SearchHasParentQuery.class);

              // Drill down into the nested SearchBoolQuery
              final SearchHasParentQuery nestedHasParentQuery =
                  (SearchHasParentQuery) outerMustQuery.queryOption();
              assertThat(nestedHasParentQuery.parentType())
                  .isEqualTo(TaskJoinRelationshipType.PROCESS.getType());

              // Drill down into the nested SearchHasChildQuery of the hasParentQuery
              final SearchHasChildQuery childQuery =
                  (SearchHasChildQuery) nestedHasParentQuery.query().queryOption();
              assertThat(childQuery.type())
                  .isEqualTo(TaskJoinRelationshipType.PROCESS_VARIABLE.getType());

              // Drill down into the nested SearchBoolQuery of the hasChildQuery
              final SearchBoolQuery innerBoolQuery =
                  (SearchBoolQuery) childQuery.query().queryOption();
              assertThat(innerBoolQuery.must()).hasSize(2);

              assertSearchTermQuery(innerBoolQuery.must().get(0).queryOption(), "name", "test");
              assertSearchTermQuery(innerBoolQuery.must().get(1).queryOption(), "value", "test");
            });
  }

  @Test
  public void shouldApplyAuthorizationCheck() {
    // given
    final var authorization =
        Authorization.of(a -> a.processDefinition().readUserTask().resourceIds(List.of("1", "2")));
    final var authorizationCheck = AuthorizationCheck.enabled(authorization);
    final var resourceAccessChecks =
        ResourceAccessChecks.of(authorizationCheck, TenantCheck.disabled());

    // when
    final var searchQuery = transformQuery(FilterBuilders.userTask(b -> b), resourceAccessChecks);

    // then
    final SearchQueryOption queryVariant = searchQuery.queryOption();
    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (boolQuery) -> {
              assertThat(boolQuery.must())
                  .anySatisfy(
                      query ->
                          assertSearchTermsQuery(
                              query.queryOption(), TaskTemplate.BPMN_PROCESS_ID, "1", "2"));
            });
  }

  @Test
  public void shouldReturnNonMatchWhenNoResourceIdsProvided() {
    // given
    final var authorization = Authorization.of(a -> a.processDefinition().readUserTask());
    final var authorizationCheck = AuthorizationCheck.enabled(authorization);
    final var resourceAccessChecks =
        ResourceAccessChecks.of(authorizationCheck, TenantCheck.disabled());

    // when
    final var searchQuery = transformQuery(FilterBuilders.userTask(b -> b), resourceAccessChecks);

    // then
    final var queryVariant = searchQuery.queryOption();
    assertThat(queryVariant).isInstanceOf(SearchMatchNoneQuery.class);
  }

  @Test
  public void shouldIgnoreAuthorizationCheckWhenDisabled() {
    // given
    final var authorizationCheck = AuthorizationCheck.disabled();
    final var resourceAccessChecks =
        ResourceAccessChecks.of(authorizationCheck, TenantCheck.disabled());

    // when
    final var searchQuery = transformQuery(FilterBuilders.userTask(b -> b), resourceAccessChecks);

    // then
    final SearchQueryOption queryVariant = searchQuery.queryOption();

    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (boolQuery) -> {
              assertThat(boolQuery.must())
                  .anySatisfy(
                      query -> assertExistsQuery(query.queryOption(), "flowNodeInstanceId"));
            });

    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertSearchTermQuery(
                  t.must().get(1).queryOption(), "implementation", "ZEEBE_USER_TASK");
            });
  }

  @Test
  public void shouldApplyTenantCheck() {
    // given
    final var tenantCheck = TenantCheck.enabled(List.of("a", "b"));
    final var resourceAccessChecks =
        ResourceAccessChecks.of(AuthorizationCheck.disabled(), tenantCheck);

    // when
    final var searchQuery = transformQuery(FilterBuilders.userTask(b -> b), resourceAccessChecks);

    // then
    final SearchQueryOption queryVariant = searchQuery.queryOption();
    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (boolQuery) -> {
              assertThat(boolQuery.must())
                  .anySatisfy(
                      query ->
                          assertSearchTermsQuery(
                              query.queryOption(), TaskTemplate.TENANT_ID, "a", "b"));
            });
  }

  @Test
  public void shouldIgnoreTenantCheckWhenDisabled() {
    // given
    final var tenantCheck = TenantCheck.disabled();
    final var resourceAccessChecks =
        ResourceAccessChecks.of(AuthorizationCheck.disabled(), tenantCheck);

    // when
    final var searchQuery = transformQuery(FilterBuilders.userTask(b -> b), resourceAccessChecks);

    // then
    final SearchQueryOption queryVariant = searchQuery.queryOption();
    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (boolQuery) -> {
              assertThat(boolQuery.must())
                  .anySatisfy(
                      query -> assertExistsQuery(query.queryOption(), "flowNodeInstanceId"));
            });

    assertThat(queryVariant)
        .isInstanceOfSatisfying(
            SearchBoolQuery.class,
            (t) -> {
              assertSearchTermQuery(
                  t.must().get(1).queryOption(), "implementation", "ZEEBE_USER_TASK");
            });
  }

  @Test
  public void shouldApplyFilterAndChecks() {
    // given
    final var authorization =
        Authorization.of(a -> a.processDefinition().readUserTask().resourceIds(List.of("1", "2")));

    final var authorizationCheck = AuthorizationCheck.enabled(authorization);
    final var tenantCheck = TenantCheck.enabled(List.of("a", "b"));
    final var resourceAccessChecks = ResourceAccessChecks.of(authorizationCheck, tenantCheck);

    // when
    final var searchQuery =
        transformQuery(FilterBuilders.userTask(b -> b.names("abc")), resourceAccessChecks);

    // then
    final var queryVariant = searchQuery.queryOption();
    assertThat(queryVariant)
        .isInstanceOfSatisfying(SearchBoolQuery.class, t -> assertThat(t.must()).hasSize(3));
  }

  private static void assertSearchTermQuery(
      final SearchQueryOption query, final String field, final Object expectedValue) {
    assertThat(query)
        .isInstanceOfSatisfying(
            SearchTermQuery.class,
            term -> {
              assertThat(term.field()).isEqualTo(field);
              assertThat(term.value().value()).isEqualTo(expectedValue);
            });
  }

  private static void assertSearchTermsQuery(
      final SearchQueryOption query, final String field, final String... expected) {
    assertThat(query)
        .isInstanceOfSatisfying(
            SearchTermsQuery.class,
            terms -> {
              assertThat(terms.field()).isEqualTo(field);
              assertThat(terms.values()).extracting(TypedValue::value).containsExactly(expected);
            });
  }

  private static void assertExistsQuery(final SearchQueryOption query, final String field) {
    assertThat(query)
        .isInstanceOfSatisfying(
            SearchExistsQuery.class, exists -> assertThat(exists.field()).isEqualTo(field));
  }
}
