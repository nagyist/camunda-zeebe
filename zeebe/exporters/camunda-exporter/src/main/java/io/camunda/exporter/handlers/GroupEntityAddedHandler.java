/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.handlers;

import io.camunda.exporter.exceptions.PersistenceException;
import io.camunda.exporter.store.BatchRequest;
import io.camunda.webapps.schema.descriptors.index.GroupIndex;
import io.camunda.webapps.schema.entities.usermanagement.GroupEntity;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.GroupIntent;
import io.camunda.zeebe.protocol.record.value.GroupRecordValue;
import java.util.List;

public class GroupEntityAddedHandler implements ExportHandler<GroupEntity, GroupRecordValue> {

  private final String indexName;

  public GroupEntityAddedHandler(final String indexName) {
    this.indexName = indexName;
  }

  @Override
  public ValueType getHandledValueType() {
    return ValueType.GROUP;
  }

  @Override
  public Class<GroupEntity> getEntityType() {
    return GroupEntity.class;
  }

  @Override
  public boolean handlesRecord(final Record<GroupRecordValue> record) {
    return getHandledValueType().equals(record.getValueType())
        && GroupIntent.ENTITY_ADDED.equals(record.getIntent());
  }

  @Override
  public List<String> generateIds(final Record<GroupRecordValue> record) {
    final var groupRecord = record.getValue();
    return List.of(GroupEntity.getChildKey(groupRecord.getGroupId(), groupRecord.getEntityId()));
  }

  @Override
  public GroupEntity createNewEntity(final String id) {
    return new GroupEntity().setId(id);
  }

  @Override
  public void updateEntity(final Record<GroupRecordValue> record, final GroupEntity entity) {
    final GroupRecordValue value = record.getValue();
    final var joinRelation = GroupIndex.JOIN_RELATION_FACTORY.createChild(value.getGroupId());
    entity.setMemberId(value.getEntityId()).setJoin(joinRelation);
  }

  @Override
  public void flush(final GroupEntity entity, final BatchRequest batchRequest)
      throws PersistenceException {
    batchRequest.addWithRouting(indexName, entity, String.valueOf(entity.getJoin().parent()));
  }

  @Override
  public String getIndexName() {
    return indexName;
  }
}
