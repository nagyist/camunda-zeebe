/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {type MetaDataDto} from 'modules/api/processInstances/fetchFlowNodeMetaData';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {useEffect} from 'react';
import {processInstanceDetailsStore} from 'modules/stores/processInstanceDetails';
import {variablesStore} from 'modules/stores/variables';
import {flowNodeSelectionStore} from 'modules/stores/flowNodeSelection';
import {modificationsStore} from 'modules/stores/modifications';
import {Paths} from 'modules/Routes';
import {QueryClientProvider} from '@tanstack/react-query';
import {getMockQueryClient} from 'modules/react-query/mockQueryClient';
import {ProcessDefinitionKeyContext} from 'App/Processes/ListView/processDefinitionKeyContext';
import {type ProcessInstance} from '@vzeta/camunda-api-zod-schemas/8.8';
import {createInstance} from 'modules/testUtils';
import type {VariableEntity} from 'modules/types/operate';

const mockProcessInstance: ProcessInstance = {
  processInstanceKey: '1',
  state: 'ACTIVE',
  startDate: '2018-06-21',
  processDefinitionKey: '2',
  processDefinitionVersion: 1,
  processDefinitionId: 'someKey',
  tenantId: '<default>',
  processDefinitionName: 'someProcessName',
  hasIncident: false,
};

const getWrapper = (
  initialEntries: React.ComponentProps<
    typeof MemoryRouter
  >['initialEntries'] = [Paths.processInstance('1')],
) => {
  const Wrapper: React.FC<{children?: React.ReactNode}> = ({children}) => {
    useEffect(() => {
      flowNodeSelectionStore.init();

      return () => {
        processInstanceDetailsStore.reset();
        variablesStore.reset();
        flowNodeSelectionStore.reset();
        modificationsStore.reset();
      };
    }, []);

    return (
      <ProcessDefinitionKeyContext.Provider value="123">
        <QueryClientProvider client={getMockQueryClient()}>
          <MemoryRouter initialEntries={initialEntries}>
            <Routes>
              <Route path={Paths.processInstance()} element={children} />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>
      </ProcessDefinitionKeyContext.Provider>
    );
  };
  return Wrapper;
};

const mockVariables: VariableEntity[] = [
  {
    id: '2251799813686037-clientNo',
    name: 'clientNo',
    value: '"CNT-1211132-0223222"',
    hasActiveOperation: false,
    isFirst: true,
    isPreview: false,
    sortValues: ['clientNo'],
  },
  {
    id: '2251799813686037-mwst',
    name: 'mwst',
    value: '124.26',
    hasActiveOperation: false,
    isFirst: false,
    isPreview: false,
    sortValues: ['mwst'],
  },
  {
    id: '2251799813686037-mwst',
    name: 'active-operation-variable',
    value: '1',
    hasActiveOperation: true,
    isFirst: false,
    isPreview: false,
    sortValues: ['active-operation-variable'],
  },
];

const mockMetaData: MetaDataDto = {
  flowNodeId: null,
  flowNodeInstanceId: '123',
  flowNodeType: 'start-event',
  instanceCount: null,
  instanceMetadata: null,
  incident: null,
  incidentCount: 0,
};

const mockProcessInstanceDeprecated = createInstance();

export {
  getWrapper,
  mockVariables,
  mockMetaData,
  mockProcessInstance,
  mockProcessInstanceDeprecated,
};
