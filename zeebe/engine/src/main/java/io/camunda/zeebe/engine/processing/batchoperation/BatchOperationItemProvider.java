/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.batchoperation;

import com.google.common.collect.Lists;
import io.camunda.search.clients.SearchClientsProxy;
import io.camunda.search.entities.ProcessInstanceEntity;
import io.camunda.search.filter.FilterBase;
import io.camunda.search.filter.IncidentFilter;
import io.camunda.search.filter.ProcessInstanceFilter;
import io.camunda.search.page.SearchQueryPageBuilders;
import io.camunda.search.query.SearchQueryBuilders;
import io.camunda.search.query.SearchQueryResult;
import io.camunda.security.auth.Authorization;
import io.camunda.security.auth.CamundaAuthentication;
import io.camunda.security.auth.SecurityContext;
import io.camunda.util.FilterUtil;
import io.camunda.zeebe.engine.EngineConfiguration;
import io.camunda.zeebe.engine.metrics.BatchOperationMetrics;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides methods to fetch entities for batch operations. It uses the search client
 * proxy to access the secondary database.
 */
public class BatchOperationItemProvider {

  private static final Logger LOG = LoggerFactory.getLogger(BatchOperationItemProvider.class);

  private final SearchClientsProxy searchClientsProxy;
  private final BatchOperationMetrics metrics;

  /**
   * The size of a page when fetching entities. This is the maximum number of items that can be
   * fetched in a single query and is limited by ElasticSearch.
   */
  private final int queryPageSize;

  /**
   * The maximum number of keys that can be passed in a single IN clause in a query. This is limited
   * to 1000 because the Oracle DB supports a maximum of 1000 elements in an IN clause.
   */
  private final int inClauseSize;

  public BatchOperationItemProvider(
      final SearchClientsProxy searchClientsProxy,
      final EngineConfiguration engineConfiguration,
      final BatchOperationMetrics metrics) {
    this.searchClientsProxy = searchClientsProxy;
    this.metrics = metrics;
    queryPageSize = engineConfiguration.getBatchOperationQueryPageSize();
    inClauseSize = engineConfiguration.getBatchOperationQueryInClauseSize();
  }

  /**
   * Fetches the process instance items for the given partitionId based on the provided filter. The
   * items are fetched sequentially in pages. Depending on the overall size of the result, this can
   * cause multiple queries to the secondary database.
   *
   * @param filter the filter to use
   * @param authentication the authentication to use
   * @param shouldAbort if the process should be aborted
   * @return a set of all found process instance items
   */
  public Set<Item> fetchProcessInstanceItems(
      final int partitionId,
      final ProcessInstanceFilter filter,
      final CamundaAuthentication authentication,
      final Supplier<Boolean> shouldAbort) {
    final var processInstanceFilter = filter.toBuilder().partitionId(partitionId).build();

    LOG.debug(
        "Fetching process instance items for partition {} with filter: {}",
        partitionId,
        processInstanceFilter);

    return fetchEntityItems(
        new ProcessInstancePageFetcher(), processInstanceFilter, authentication, shouldAbort);
  }

  /**
   * Fetches the incident items based on the provided incident filter. The items are fetched
   * sequentially in pages. Depending on the overall size of the result this can cause multiple
   * queries to the secondary database.
   *
   * @param filter the filter to use
   * @param authentication the authentication to use
   * @param shouldAbort if the process should be aborted
   * @return a set of all found process instance items
   */
  public Set<Item> fetchIncidentItems(
      final IncidentFilter filter,
      final CamundaAuthentication authentication,
      final Supplier<Boolean> shouldAbort) {

    LOG.debug("Fetching incident items with filter: {}", filter);
    return fetchEntityItems(new IncidentPageFetcher(), filter, authentication, shouldAbort);
  }

  /**
   * Fetches the incident items based on the provided process instance filter for the given
   * partitionId. This will return <b>ALL</b> incidents of the matching processInstances. The items
   * are fetched sequentially in pages. Depending on the overall size of the result, this can cause
   * multiple queries to the secondary database.
   *
   * @param filter the filter to use
   * @param authentication the authentication to use
   * @param shouldAbort if the process should be aborted
   * @return a set of all found incident items
   */
  public Set<Item> fetchIncidentItems(
      final int partitionId,
      final ProcessInstanceFilter filter,
      final CamundaAuthentication authentication,
      final Supplier<Boolean> shouldAbort) {
    // first fetch all matching processInstances
    final var processInstanceKeys =
        fetchProcessInstanceItems(partitionId, filter, authentication, shouldAbort).stream()
            .map(Item::processInstanceKey)
            .toList();

    // then fetch all incidents of the matching processInstances
    return getIncidentItemsOfProcessInstanceKeys(
        new ArrayList<>(processInstanceKeys), authentication, shouldAbort);
  }

  /**
   * Fetches the entity items based on the provided filter. The items are fetched sequentially in
   * pages. Depending on the overall size of the result, this can cause multiple queries to the
   * secondary database.
   *
   * @param itemPageFetcher the fetcher to use for fetching items
   * @param filter the filter to use
   * @param authentication the authentication to use
   * @param shouldAbort if the process should be aborted
   * @return a set of all found entity items
   */
  private <F extends FilterBase> Set<Item> fetchEntityItems(
      final ItemPageFetcher<F> itemPageFetcher,
      final F filter,
      final CamundaAuthentication authentication,
      final Supplier<Boolean> shouldAbort) {
    final var items = new LinkedHashSet<Item>();

    String searchAfter = null;
    while (true) {
      // Check if the batch operation is still present, could be canceled in the meantime
      if (shouldAbort.get()) {
        LOG.warn("Batch operation is no longer active, stopping query.");
        return Set.of();
      }

      final ItemPage result;
      try {
        result = itemPageFetcher.fetchItems(filter, searchAfter, authentication);
        metrics.recordQueryAgainstSecondaryDatabase();
      } catch (final Exception e) {
        metrics.recordFailedQueryAgainstSecondaryDatabase();
        throw e;
      }

      items.addAll(result.items);
      searchAfter = result.searchAfter();

      // the result.total count can be incorrect when using elasticsearch and could be capped at
      // 10_000. If the result.total is smaller than the queryPageSize, we can assume that we have
      // fetched all items. Otherwise, we fetch again until we fetch an empty page
      if (result.items.isEmpty() || result.total < queryPageSize) {
        break;
      }
    }

    return items;
  }

  /**
   * Fetches the incident items of the given process instance keys. The items are fetched in batches
   * to avoid hitting the potential IN clause size limit of the database. Especially Oracle has a
   * size limit of 1000 there.
   *
   * @param processInstanceKeys the process instance keys to fetch incidents for
   * @param authentication the authentication to use
   * @param shouldAbort if the process should be aborted
   * @return a set of all found incident items
   */
  private Set<Item> getIncidentItemsOfProcessInstanceKeys(
      final List<Long> processInstanceKeys,
      final CamundaAuthentication authentication,
      final Supplier<Boolean> shouldAbort) {
    final Set<Item> incidents = new LinkedHashSet<>();

    final List<List<Long>> processInstanceKeysBatches =
        Lists.partition(processInstanceKeys, inClauseSize);

    for (final List<Long> processInstanceKeysBatch : processInstanceKeysBatches) {
      if (shouldAbort.get()) {
        return Set.of();
      }
      final var filter =
          new IncidentFilter.Builder()
              .processInstanceKeyOperations(
                  FilterUtil.mapDefaultToOperation(processInstanceKeysBatch))
              .build();
      incidents.addAll(fetchIncidentItems(filter, authentication, shouldAbort));
    }

    return incidents;
  }

  /**
   * Internal abstraction to hold an item of a batch operation. This is used to represent an itemKey
   * and it's related processInstanceKey.
   *
   * @param itemKey the key of the item
   * @param processInstanceKey the key of the process instance this item belongs to
   */
  public record Item(long itemKey, long processInstanceKey) {}

  /**
   * Internal abstraction to hold the result of a page of entity items.
   *
   * @param items the fetched items
   * @param searchAfter cursor to fetch the next page of items
   * @param total the total amount of found items
   */
  private record ItemPage(List<Item> items, String searchAfter, long total) {}

  /**
   * Internal abstraction interface to get a single page of entity items of a specific type. This is
   * needed because the actual query to the secondary database is different for specific entity
   * types.
   *
   * @param <F> the filter type
   */
  private interface ItemPageFetcher<F extends FilterBase> {

    /**
     * Fetches a page of entity items based on the provided filter and search values.
     *
     * @param filter the filter to apply
     * @param searchAfter the current searchAfter
     * @return the fetched items and pagination information
     */
    ItemPage fetchItems(F filter, String searchAfter, CamundaAuthentication authentication);

    /**
     * Creates a security context for the given authentication and authorization.
     *
     * @param authentication the authentication of the user which started the batch operation
     * @param authorization the same authorization is needed, that is normally used in
     *     ProcessInstanceServices / IncidentServices
     * @return the security context
     */
    default SecurityContext createSecurityContext(
        final CamundaAuthentication authentication, final Authorization authorization) {
      return SecurityContext.of(
          b -> b.withAuthentication(authentication).withAuthorization(authorization));
    }
  }

  private final class ProcessInstancePageFetcher implements ItemPageFetcher<ProcessInstanceFilter> {
    @Override
    public ItemPage fetchItems(
        final ProcessInstanceFilter filter,
        final String searchAfter,
        final CamundaAuthentication authentication) {
      final var securityContext =
          createSecurityContext(
              authentication, Authorization.of(a -> a.processDefinition().readProcessInstance()));
      final var page =
          SearchQueryPageBuilders.page().size(queryPageSize).after(searchAfter).build();
      final var query =
          SearchQueryBuilders.processInstanceSearchQuery()
              .filter(filter)
              .page(page)
              .resultConfig(c -> c.onlyKey(true))
              .build();

      final SearchQueryResult<ProcessInstanceEntity> result =
          searchClientsProxy.withSecurityContext(securityContext).searchProcessInstances(query);

      return new ItemPage(
          result.items().stream()
              .map(pi -> new Item(pi.processInstanceKey(), pi.processInstanceKey()))
              .collect(Collectors.toList()),
          result.endCursor(),
          result.total());
    }
  }

  private final class IncidentPageFetcher implements ItemPageFetcher<IncidentFilter> {
    @Override
    public ItemPage fetchItems(
        final IncidentFilter filter,
        final String searchAfter,
        final CamundaAuthentication authentication) {
      final var securityContext =
          createSecurityContext(
              authentication, Authorization.of(a -> a.processDefinition().readProcessInstance()));
      final var page =
          SearchQueryPageBuilders.page().size(queryPageSize).after(searchAfter).build();
      final var query = SearchQueryBuilders.incidentSearchQuery().filter(filter).page(page).build();

      final var result =
          searchClientsProxy.withSecurityContext(securityContext).searchIncidents(query);

      return new ItemPage(
          result.items().stream()
              .map(pi -> new Item(pi.incidentKey(), pi.processInstanceKey()))
              .collect(Collectors.toList()),
          result.endCursor(),
          result.total());
    }
  }
}
