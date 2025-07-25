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

import io.camunda.client.api.search.sort.UserTaskSort;
import io.camunda.client.impl.search.request.SearchRequestSortBase;

public class UserTaskSortImpl extends SearchRequestSortBase<UserTaskSort> implements UserTaskSort {

  @Override
  public UserTaskSort creationDate() {
    return field("creationDate");
  }

  @Override
  public UserTaskSort completionDate() {
    return field("completionDate");
  }

  @Override
  public UserTaskSort dueDate() {
    return field("dueDate");
  }

  @Override
  public UserTaskSort followUpDate() {
    return field("followUpDate");
  }

  @Override
  public UserTaskSort priority() {
    return field("priority");
  }

  @Override
  public UserTaskSort name() {
    return field("name");
  }

  @Override
  protected UserTaskSort self() {
    return this;
  }
}
