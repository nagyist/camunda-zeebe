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
package io.camunda.zeebe.spring.client.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.api.JsonMapper;
import io.camunda.client.impl.CamundaObjectMapper;
import io.camunda.zeebe.spring.common.json.SdkObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonMapperConfiguration {
  private final ObjectMapper objectMapper;

  @Autowired
  public JsonMapperConfiguration(@Autowired(required = false) final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Bean(name = "zeebeJsonMapper")
  @ConditionalOnMissingBean
  public JsonMapper jsonMapper() {
    if (objectMapper == null) {
      return new CamundaObjectMapper();
    }
    return new CamundaObjectMapper(objectMapper);
  }

  @Bean(name = "commonJsonMapper")
  @ConditionalOnMissingBean
  public io.camunda.zeebe.spring.common.json.JsonMapper commonJsonMapper() {
    if (objectMapper == null) {
      return new SdkObjectMapper();
    }
    return new SdkObjectMapper(objectMapper);
  }
}