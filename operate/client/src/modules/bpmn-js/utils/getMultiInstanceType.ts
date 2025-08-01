/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {type BusinessObject} from 'bpmn-js/lib/NavigatedViewer';

const getMultiInstanceType = (businessObject: BusinessObject) => {
  if (businessObject.loopCharacteristics === undefined) {
    return;
  }

  return businessObject.loopCharacteristics.isSequential
    ? 'sequential'
    : 'parallel';
};

export {getMultiInstanceType};
