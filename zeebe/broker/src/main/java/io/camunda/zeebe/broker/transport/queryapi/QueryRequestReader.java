/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.broker.transport.queryapi;

import io.camunda.zeebe.broker.transport.AsyncApiRequestHandler.RequestReader;
import io.camunda.zeebe.protocol.record.ExecuteQueryRequestDecoder;
import io.camunda.zeebe.protocol.record.MessageHeaderDecoder;
import org.agrona.DirectBuffer;

public class QueryRequestReader implements RequestReader {
  private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
  private final ExecuteQueryRequestDecoder messageDecoder = new ExecuteQueryRequestDecoder();

  @Override
  public void reset() {
    // nothing to reset here, decoders are re-wrapped for every request
  }

  @Override
  public void wrap(final DirectBuffer buffer, final int offset, final int length) {
    messageDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
  }

  public ExecuteQueryRequestDecoder getMessageDecoder() {
    return messageDecoder;
  }
}
