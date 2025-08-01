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
package io.camunda.client.impl.search.response;

import io.camunda.client.api.search.enums.BatchOperationErrorType;
import io.camunda.client.api.search.response.BatchOperationError;

public class BatchOperationErrorImpl implements BatchOperationError {

  private final BatchOperationErrorType type;
  private final String message;
  private final int partitionId;

  public BatchOperationErrorImpl(final io.camunda.client.protocol.rest.BatchOperationError item) {
    type = item.getType() != null ? BatchOperationErrorType.valueOf(item.getType().name()) : null;
    message = item.getMessage();
    partitionId = item.getPartitionId();
  }

  @Override
  public BatchOperationErrorType getType() {
    return type;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public int getPartitionId() {
    return partitionId;
  }
}
