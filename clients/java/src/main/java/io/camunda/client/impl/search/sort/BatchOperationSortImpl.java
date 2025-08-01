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
package io.camunda.client.impl.search.sort;

import io.camunda.client.api.search.sort.BatchOperationSort;
import io.camunda.client.impl.search.request.SearchRequestSortBase;

public class BatchOperationSortImpl extends SearchRequestSortBase<BatchOperationSort>
    implements BatchOperationSort {

  @Override
  public BatchOperationSort batchOperationKey() {
    return field("batchOperationKey");
  }

  @Override
  public BatchOperationSort state() {
    return field("state");
  }

  @Override
  public BatchOperationSort operationType() {
    return field("operationType");
  }

  @Override
  public BatchOperationSort startDate() {
    return field("startDate");
  }

  @Override
  public BatchOperationSort endDate() {
    return field("endDate");
  }

  @Override
  protected BatchOperationSort self() {
    return this;
  }
}
