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
package io.camunda.spring.client.exception;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.spring.client.jobhandling.JobHandlingUtil;
import org.junit.jupiter.api.Test;

public class JobErrorTest {
  @Test
  void shouldUseRequiredArgsCtor() {
    final JobError jobError = new JobError("message1337");
    assertThat(JobHandlingUtil.createErrorMessage(jobError)).contains("message1337");
  }
}
