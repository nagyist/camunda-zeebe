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
package io.camunda.client.resource;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.http.RequestMethod;
<<<<<<< HEAD
import io.camunda.client.api.response.Resource;
import io.camunda.client.protocol.rest.ResourceResult;
import io.camunda.client.util.ClientRestTest;
import io.camunda.client.util.RestGatewayPaths;
import io.camunda.client.util.RestGatewayService;
import io.camunda.client.util.assertions.LoggedRequestAssert;
=======
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.camunda.client.protocol.rest.ResourceResult;
import io.camunda.client.util.ClientRestTest;
import io.camunda.client.util.RestGatewayPaths;
>>>>>>> 570afd0fb7a (test: added tests to assert that the endpoints are called by the client)
import org.junit.jupiter.api.Test;

public class GetResourceTest extends ClientRestTest {
  @Test
  void shouldGetResource() {
<<<<<<< HEAD
    // given
=======
>>>>>>> 570afd0fb7a (test: added tests to assert that the endpoints are called by the client)
    gatewayService.onResourceGetRequest(
        123L,
        new ResourceResult()
            .resourceKey("123")
            .resourceId("test.bpmn")
            .resourceName("Test process")
            .version(1));
<<<<<<< HEAD

    // when
    final Resource response = client.newResourceGetRequest(123L).execute();

    // then
    LoggedRequestAssert.assertThat(RestGatewayService.getLastRequest())
        .hasMethod(RequestMethod.GET)
        .hasUrl(RestGatewayPaths.getResourceUrl("123"));

    assertThat(response.getResourceKey()).isEqualTo(123);
    assertThat(response.getResourceId()).isEqualTo("test.bpmn");
    assertThat(response.getResourceName()).isEqualTo("Test process");
    assertThat(response.getVersion()).isEqualTo(1);
=======
    client.newResourceGetRequest(123L).execute();
    // then
    final LoggedRequest request = gatewayService.getLastRequest();
    assertThat(request.getUrl()).isEqualTo(RestGatewayPaths.getResourceUrl("123"));
    assertThat(request.getMethod()).isEqualTo(RequestMethod.GET);
>>>>>>> 570afd0fb7a (test: added tests to assert that the endpoints are called by the client)
  }
}
