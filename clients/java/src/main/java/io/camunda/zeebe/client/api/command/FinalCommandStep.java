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
package io.camunda.zeebe.client.api.command;

import io.camunda.zeebe.client.ZeebeClientBuilder;
import io.camunda.zeebe.client.api.ZeebeFuture;
import java.time.Duration;

/**
 * @deprecated since 8.8 for removal in 8.10, replaced by {@link
 *     io.camunda.client.api.command.FinalCommandStep}
 */
public interface FinalCommandStep<T> {

  /**
   * Sets the request timeout for the command. The default request timeout can be configured using
   * {@link ZeebeClientBuilder#defaultRequestTimeout(Duration)}.
   *
   * @param requestTimeout the request timeout
   * @return the configured command
   */
  FinalCommandStep<T> requestTimeout(Duration requestTimeout);

  /**
   * Sends the command to the Zeebe broker. This operation is asynchronous. In case of success, the
   * future returns the event that was generated by the Zeebe broker in response to the command.
   *
   * <p>Call {@link ZeebeFuture#join()} to wait until the response is available.
   *
   * <pre>
   * Future&#60;JobEvent&#62 future = command.send();
   * JobEvent event = future.join();
   * </pre>
   *
   * @return a future tracking state of success/failure of the command.
   */
  ZeebeFuture<T> send();
}
