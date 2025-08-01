/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.client.util.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.impl.search.request.SearchRequestSort;
import io.camunda.client.protocol.rest.SortOrderEnum;

public class SortAssert {

  public static void assertSort(
      final SearchRequestSort sort, final String name, final SortOrderEnum order) {
    assertThat(sort.getField()).isEqualTo(name);
    assertThat(sort.getOrder()).isEqualTo(order);
  }
}
