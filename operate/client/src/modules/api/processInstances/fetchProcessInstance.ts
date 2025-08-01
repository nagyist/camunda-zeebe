/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {requestAndParse, requestWithThrow} from 'modules/request';
import type {ProcessInstanceEntity} from 'modules/types/operate';

const fetchProcessInstance = async (
  processInstanceId: ProcessInstanceEntity['id'],
  options?: Parameters<typeof requestAndParse>[1],
) => {
  return requestAndParse<ProcessInstanceEntity>(
    {
      url: `/api/process-instances/${processInstanceId}`,
    },
    options,
  );
};

const fetchProcessInstanceDeprecated = async (processInstanceKey: string) => {
  return requestWithThrow<ProcessInstanceEntity>({
    url: `/api/process-instances/${processInstanceKey}`,
    method: 'GET',
  });
};

export {fetchProcessInstance, fetchProcessInstanceDeprecated};
