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
package io.camunda.client.impl.search.request;

import static io.camunda.client.api.search.request.SearchRequestBuilders.adHocSubProcessActivityFilter;
import static io.camunda.client.impl.search.request.TypedSearchRequestPropertyProvider.provideSearchRequestProperty;

import io.camunda.client.api.CamundaFuture;
import io.camunda.client.api.JsonMapper;
import io.camunda.client.api.command.FinalCommandStep;
import io.camunda.client.api.search.filter.AdHocSubProcessActivityFilter;
import io.camunda.client.api.search.request.AdHocSubProcessActivitySearchRequest;
import io.camunda.client.api.search.response.AdHocSubProcessActivityResponse;
import io.camunda.client.impl.http.HttpCamundaFuture;
import io.camunda.client.impl.http.HttpClient;
import io.camunda.client.impl.search.response.AdHocSubProcessActivityResponseImpl;
import io.camunda.client.protocol.rest.AdHocSubProcessActivitySearchQuery;
import io.camunda.client.protocol.rest.AdHocSubProcessActivitySearchQueryResult;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.hc.client5.http.config.RequestConfig;

public class AdHocSubProcessActivitySearchRequestImpl
    implements AdHocSubProcessActivitySearchRequest {

  private final AdHocSubProcessActivitySearchQuery request;
  private final HttpClient httpClient;
  private final JsonMapper jsonMapper;
  private final RequestConfig.Builder httpRequestConfig;

  public AdHocSubProcessActivitySearchRequestImpl(
      final HttpClient httpClient, final JsonMapper jsonMapper) {
    request = new AdHocSubProcessActivitySearchQuery();
    this.httpClient = httpClient;
    this.jsonMapper = jsonMapper;
    httpRequestConfig = httpClient.newRequestConfig();
  }

  @Override
  public AdHocSubProcessActivitySearchRequest filter(final AdHocSubProcessActivityFilter filter) {
    request.setFilter(provideSearchRequestProperty(filter));
    return this;
  }

  @Override
  public AdHocSubProcessActivitySearchRequest filter(
      final Consumer<AdHocSubProcessActivityFilter> fn) {
    return filter(adHocSubProcessActivityFilter(fn));
  }

  @Override
  public FinalCommandStep<AdHocSubProcessActivityResponse> requestTimeout(
      final Duration requestTimeout) {
    httpRequestConfig.setResponseTimeout(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);
    return this;
  }

  @Override
  public CamundaFuture<AdHocSubProcessActivityResponse> send() {
    final HttpCamundaFuture<AdHocSubProcessActivityResponse> result = new HttpCamundaFuture<>();
    httpClient.post(
        "/element-instances/ad-hoc-activities/search",
        jsonMapper.toJson(request),
        httpRequestConfig.build(),
        AdHocSubProcessActivitySearchQueryResult.class,
        AdHocSubProcessActivityResponseImpl::new,
        result);
    return result;
  }
}
