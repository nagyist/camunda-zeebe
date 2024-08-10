/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.schema.index;

import static io.camunda.optimize.service.db.DatabaseConstants.MAPPING_ENABLED_SETTING;

import io.camunda.optimize.dto.optimize.ProcessDefinitionOptimizeDto;
import io.camunda.optimize.service.db.DatabaseConstants;
import java.io.IOException;
import org.elasticsearch.xcontent.XContentBuilder;

public abstract class ProcessDefinitionIndex<TBuilder> extends AbstractDefinitionIndex<TBuilder> {

  public static final int VERSION = 6;

  public static final String PROCESS_DEFINITION_ID = DEFINITION_ID;
  public static final String PROCESS_DEFINITION_KEY = DEFINITION_KEY;
  public static final String PROCESS_DEFINITION_VERSION = DEFINITION_VERSION;
  public static final String PROCESS_DEFINITION_VERSION_TAG = DEFINITION_VERSION_TAG;
  public static final String PROCESS_DEFINITION_NAME = DEFINITION_NAME;
  public static final String PROCESS_DEFINITION_XML = ProcessDefinitionOptimizeDto.Fields.bpmn20Xml;
  public static final String FLOW_NODE_DATA = ProcessDefinitionOptimizeDto.Fields.flowNodeData;
  public static final String USER_TASK_NAMES = ProcessDefinitionOptimizeDto.Fields.userTaskNames;
  public static final String TENANT_ID = DEFINITION_TENANT_ID;
  public static final String ONBOARDED = ProcessDefinitionOptimizeDto.Fields.onboarded;

  @Override
  public String getIndexName() {
    return DatabaseConstants.PROCESS_DEFINITION_INDEX_NAME;
  }

  @Override
  public int getVersion() {
    return VERSION;
  }

  @Override
  public XContentBuilder addProperties(XContentBuilder xContentBuilder) throws IOException {
    // @formatter:off
    return super.addProperties(xContentBuilder)
        .startObject(FLOW_NODE_DATA)
        .field("type", DatabaseConstants.TYPE_OBJECT)
        .field(MAPPING_ENABLED_SETTING, "false")
        .endObject()
        .startObject(USER_TASK_NAMES)
        .field("type", DatabaseConstants.TYPE_OBJECT)
        .field(MAPPING_ENABLED_SETTING, "false")
        .endObject()
        .startObject(PROCESS_DEFINITION_XML)
        .field("type", DatabaseConstants.TYPE_TEXT)
        .field("index", true)
        .field("analyzer", "is_present_analyzer")
        .endObject()
        .startObject(ONBOARDED)
        .field("type", DatabaseConstants.TYPE_BOOLEAN)
        .endObject();
    // @formatter:on
  }
}