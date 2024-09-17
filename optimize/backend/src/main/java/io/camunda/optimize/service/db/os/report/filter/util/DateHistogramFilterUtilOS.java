/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.os.report.filter.util;

import static io.camunda.optimize.service.db.os.externalcode.client.dsl.AggregationDSL.fieldDateMath;
import static io.camunda.optimize.service.db.report.filter.util.DateHistogramFilterUtil.getMaxDateFilterOffsetDateTime;
import static io.camunda.optimize.service.db.report.filter.util.DateHistogramFilterUtil.getMinDateFilterOffsetDateTime;

import io.camunda.optimize.dto.optimize.query.report.single.decision.filter.EvaluationDateFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.DateFilterDataDto;
import io.camunda.optimize.service.db.filter.FilterContext;
import io.camunda.optimize.service.db.os.report.context.DateAggregationContextOS;
import io.camunda.optimize.service.db.os.report.filter.QueryFilterOS;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.opensearch.client.opensearch._types.aggregations.ExtendedBounds;
import org.opensearch.client.opensearch._types.aggregations.FieldDateMath;
import org.opensearch.client.opensearch._types.query_dsl.Query;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateHistogramFilterUtilOS {
  public static List<Query> createDecisionDateHistogramLimitingFilter(
      final DateAggregationContextOS context) {
    return context
        .getDecisionQueryFilterEnhancer()
        .getEvaluationDateQueryFilter()
        .filterQueries(evaluationDateFilter(context), context.getFilterContext());
  }

  private static List<DateFilterDataDto<?>> evaluationDateFilter(
      final DateAggregationContextOS context) {
    return context
        .getDecisionQueryFilterEnhancer()
        .extractFilters(context.getDecisionFilters(), EvaluationDateFilterDto.class);
  }

  public static Optional<ExtendedBounds<FieldDateMath>> extendBounds(
      final DateAggregationContextOS context, final DateTimeFormatter dateFormatter) {
    return getExtendedBoundsFromDateFilters(evaluationDateFilter(context), dateFormatter, context);
  }

  public static List<Query> createFilterBoolQueryBuilder(
      final List<DateFilterDataDto<?>> filters,
      final QueryFilterOS<DateFilterDataDto<?>> queryFilter,
      final FilterContext filterContext) {
    return queryFilter.filterQueries(filters, filterContext);
  }

  //
  public static Optional<ExtendedBounds<FieldDateMath>> getExtendedBoundsFromDateFilters(
      final List<DateFilterDataDto<?>> dateFilters,
      final DateTimeFormatter dateFormatter,
      final DateAggregationContextOS context) {
    if (dateFilters.isEmpty()) {
      return Optional.empty();
    }

    // in case of several dateFilters, use min (oldest) one as start, and max (newest) one as end
    final Optional<OffsetDateTime> filterStart = getMinDateFilterOffsetDateTime(dateFilters);
    final OffsetDateTime filterEnd = getMaxDateFilterOffsetDateTime(dateFilters);
    return filterStart.map(
        start ->
            new ExtendedBounds.Builder<FieldDateMath>()
                .min(
                    fieldDateMath(
                        dateFormatter.format(start.atZoneSameInstant(context.getTimezone()))))
                .max(
                    fieldDateMath(
                        dateFormatter.format(filterEnd.atZoneSameInstant(context.getTimezone()))))
                .build());
  }
}