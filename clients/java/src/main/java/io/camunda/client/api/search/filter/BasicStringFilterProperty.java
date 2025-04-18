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
package io.camunda.client.api.search.filter;

import java.util.ArrayList;
import java.util.List;

public class BasicStringFilterProperty {

  private String eq;
  private String neq;
  private Boolean exists;
  private List<String> in = new ArrayList<>();
  private List<String> notIn = new ArrayList<>();

  public String getEq() {
    return eq;
  }

  public void setEq(final String eq) {
    this.eq = eq;
  }

  public String getNeq() {
    return neq;
  }

  public void setNeq(final String neq) {
    this.neq = neq;
  }

  public Boolean getExists() {
    return exists;
  }

  public void setExists(final Boolean exists) {
    this.exists = exists;
  }

  public List<String> getIn() {
    return in;
  }

  public void setIn(final List<String> in) {
    this.in = in;
  }

  public List<String> getNotIn() {
    return notIn;
  }

  public void setNotIn(final List<String> notIn) {
    this.notIn = notIn;
  }
}
