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
package io.camunda.client.impl.fetch;

import io.camunda.client.api.ZeebeFuture;
import io.camunda.client.api.command.FinalCommandStep;
import io.camunda.client.api.fetch.ProcessDefinitionGetFormRequest;
import io.camunda.client.api.search.response.Form;
import io.camunda.client.impl.http.HttpClient;
import io.camunda.client.impl.http.HttpZeebeFuture;
import io.camunda.client.impl.search.response.FormImpl;
import io.camunda.client.protocol.rest.FormItem;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.config.RequestConfig;

public class ProcessDefinitionGetFormRequestImpl implements ProcessDefinitionGetFormRequest {

  private final HttpClient httpClient;
  private final RequestConfig.Builder httpRequestConfig;
  private final long processDefinitionKey;

  public ProcessDefinitionGetFormRequestImpl(
      final HttpClient httpClient, final long processDefinitionKey) {
    this.httpClient = httpClient;
    httpRequestConfig = httpClient.newRequestConfig();
    this.processDefinitionKey = processDefinitionKey;
  }

  @Override
  public FinalCommandStep<Form> requestTimeout(final Duration requestTimeout) {
    httpRequestConfig.setResponseTimeout(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);
    return this;
  }

  @Override
  public ZeebeFuture<Form> send() {
    final HttpZeebeFuture<Form> result = new HttpZeebeFuture<>();
    httpClient.get(
        String.format("/process-definitions/%d/form", processDefinitionKey),
        httpRequestConfig.build(),
        FormItem.class,
        FormImpl::new,
        result);
    return result;
  }
}