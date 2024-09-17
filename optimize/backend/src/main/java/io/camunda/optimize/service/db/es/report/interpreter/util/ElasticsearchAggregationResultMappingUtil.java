/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.interpreter.util;

import co.elastic.clients.elasticsearch._types.aggregations.TDigestPercentilesAggregate;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ElasticsearchAggregationResultMappingUtil {

  public static Double mapToDoubleOrNull(
      final TDigestPercentilesAggregate aggregation, final double percentileValue) {
    Double percentile =
        Optional.ofNullable(aggregation.values())
            .filter(h -> h.keyed().get(Double.toString(percentileValue)) != null)
            .map(h -> Double.parseDouble(h.keyed().get(Double.toString(percentileValue))))
            .orElse(null);
    if (percentile == null || Double.isNaN(percentile) || Double.isInfinite(percentile)) {
      return null;
    } else {
      return percentile;
    }
  }

  public static Double mapToDoubleOrNull(final Double value) {
    if (Double.isInfinite(value) || Double.isNaN(value)) {
      return null;
    } else {
      return value;
    }
  }
}