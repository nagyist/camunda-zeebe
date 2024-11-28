/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.gateway.impl.broker.request.group;

import io.camunda.zeebe.broker.client.api.dto.BrokerExecuteCommand;
import io.camunda.zeebe.protocol.Protocol;
import io.camunda.zeebe.protocol.impl.record.value.group.GroupRecord;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.GroupIntent;
import org.agrona.DirectBuffer;

public class BrokerGroupUpdateRequest extends BrokerExecuteCommand<GroupRecord> {

  private final GroupRecord requestDto = new GroupRecord();

  public BrokerGroupUpdateRequest(final long groupKey) {
    super(ValueType.GROUP, GroupIntent.UPDATE);
    request.setKey(groupKey);
    requestDto.setGroupKey(groupKey);
    setPartitionId(Protocol.DEPLOYMENT_PARTITION);
  }

  public BrokerGroupUpdateRequest setName(final String name) {
    requestDto.setName(name);
    return this;
  }

  @Override
  public GroupRecord getRequestWriter() {
    return requestDto;
  }

  @Override
  protected GroupRecord toResponseDto(final DirectBuffer buffer) {
    final var response = new GroupRecord();
    response.wrap(buffer);
    return response;
  }
}