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
package io.camunda.zeebe.client.impl.command;

import io.camunda.client.protocol.rest.IncidentResolutionRequest;
import io.camunda.zeebe.client.CredentialsProvider.StatusCode;
import io.camunda.zeebe.client.api.JsonMapper;
import io.camunda.zeebe.client.api.ZeebeFuture;
import io.camunda.zeebe.client.api.command.FinalCommandStep;
import io.camunda.zeebe.client.api.command.ResolveIncidentCommandStep1;
import io.camunda.zeebe.client.api.response.ResolveIncidentResponse;
import io.camunda.zeebe.client.impl.RetriableClientFutureImpl;
import io.camunda.zeebe.client.impl.http.HttpClient;
import io.camunda.zeebe.client.impl.http.HttpZeebeFuture;
import io.camunda.zeebe.client.impl.response.ResolveIncidentResponseImpl;
import io.camunda.zeebe.gateway.protocol.GatewayGrpc.GatewayStub;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ResolveIncidentRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ResolveIncidentRequest.Builder;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.apache.hc.client5.http.config.RequestConfig;

public final class ResolveIncidentCommandImpl implements ResolveIncidentCommandStep1 {

  private final GatewayStub asyncStub;
  private final Builder builder;
  private final Predicate<StatusCode> retryPredicate;
  private Duration requestTimeout;
  private final HttpClient httpClient;
  private final RequestConfig.Builder httpRequestConfig;
  private boolean useRest;
  private final long incidentKey;
  private final IncidentResolutionRequest httpRequestObject;
  private final JsonMapper jsonMapper;

  public ResolveIncidentCommandImpl(
      final GatewayStub asyncStub,
      final long incidentKey,
      final Duration requestTimeout,
      final Predicate<StatusCode> retryPredicate,
      final HttpClient httpClient,
      final boolean preferRestOverGrpc,
      final JsonMapper jsonMapper) {
    this.asyncStub = asyncStub;
    builder = ResolveIncidentRequest.newBuilder().setIncidentKey(incidentKey);
    this.requestTimeout = requestTimeout;
    this.retryPredicate = retryPredicate;
    this.httpClient = httpClient;
    httpRequestConfig = httpClient.newRequestConfig();
    useRest = preferRestOverGrpc;
    this.incidentKey = incidentKey;
    httpRequestObject = new IncidentResolutionRequest();
    this.jsonMapper = jsonMapper;
  }

  @Override
  public FinalCommandStep<ResolveIncidentResponse> requestTimeout(final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    httpRequestConfig.setResponseTimeout(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);
    return this;
  }

  @Override
  public ZeebeFuture<ResolveIncidentResponse> send() {
    if (useRest) {
      return sendRestRequest();
    } else {
      return sendGrpcRequest();
    }
  }

  private ZeebeFuture<ResolveIncidentResponse> sendRestRequest() {
    final HttpZeebeFuture<ResolveIncidentResponse> result = new HttpZeebeFuture<>();
    httpClient.post(
        "/incidents/" + incidentKey + "/resolution",
        jsonMapper.toJson(httpRequestObject),
        httpRequestConfig.build(),
        result);
    return result;
  }

  private ZeebeFuture<ResolveIncidentResponse> sendGrpcRequest() {
    final ResolveIncidentRequest request = builder.build();

    final RetriableClientFutureImpl<
            ResolveIncidentResponse, GatewayOuterClass.ResolveIncidentResponse>
        future =
            new RetriableClientFutureImpl<>(
                ResolveIncidentResponseImpl::new,
                retryPredicate,
                streamObserver -> sendGrpcRequest(request, streamObserver));

    sendGrpcRequest(request, future);
    return future;
  }

  private void sendGrpcRequest(
      final ResolveIncidentRequest request,
      final StreamObserver<GatewayOuterClass.ResolveIncidentResponse> streamObserver) {
    asyncStub
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .resolveIncident(request, streamObserver);
  }

  @Override
  public ResolveIncidentCommandStep1 operationReference(final long operationReference) {
    builder.setOperationReference(operationReference);
    httpRequestObject.setOperationReference(operationReference);
    return this;
  }

  @Override
  public ResolveIncidentCommandStep1 useRest() {
    useRest = true;
    return this;
  }

  @Override
  public ResolveIncidentCommandStep1 useGrpc() {
    useRest = false;
    return this;
  }
}
