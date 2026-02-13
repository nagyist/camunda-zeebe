/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {VariablePanel} from '../index';
import {render, screen, waitFor, type UserEvent} from 'modules/testing-library';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {createvariable} from 'modules/testUtils';
import {
  modificationsStore,
  type FlowNodeModification,
} from 'modules/stores/modifications';
import {useEffect, act} from 'react';
import {Paths} from 'modules/Routes';
import {QueryClientProvider} from '@tanstack/react-query';
import {getMockQueryClient} from 'modules/react-query/mockQueryClient';
import {mockFetchProcessInstance} from 'modules/mocks/api/v2/processInstances/fetchProcessInstance';
import {type ProcessInstance} from '@camunda/camunda-api-zod-schemas/8.8';
import {mockSearchVariables} from 'modules/mocks/api/v2/variables/searchVariables';
import {mockSearchJobs} from 'modules/mocks/api/v2/jobs/searchJobs';
import {mockFetchProcessDefinitionXml} from 'modules/mocks/api/v2/processDefinitions/fetchProcessDefinitionXml';
import {ProcessDefinitionKeyContext} from 'App/Processes/ListView/processDefinitionKeyContext';
import {useProcessInstanceElementSelection} from 'modules/hooks/useProcessInstanceElementSelection';
import {mockFetchElementInstance} from 'modules/mocks/api/v2/elementInstances/fetchElementInstance';

const INITIAL_ADD_MODIFICATION: FlowNodeModification = {
  type: 'token',
  payload: {
    affectedTokenCount: 1,
    flowNode: {
      id: 'flow_node_0',
      name: 'flow node 0',
    },
    operation: 'ADD_TOKEN',
    parentScopeIds: {},
    scopeId: 'random-scope-id-0',
    visibleAffectedTokenCount: 1,
  },
};

const editExistingVariableValue = async (
  user: UserEvent,
  variableName: string,
  newValue: string,
) => {
  const variableRow = screen.getByTestId(`variable-${variableName}`);
  const valueField = variableRow.querySelector('input');

  if (!valueField) {
    throw new Error(`No value field found for variable ${variableName}`);
  }

  await user.click(valueField);
  await user.keyboard('{Control>}a{/Control}');
  await user.keyboard('{Backspace}');
  await user.type(valueField, newValue);
  await user.tab();
};

const editLastNewVariableName = async (user: UserEvent, value: string) => {
  const nameField = screen.getAllByTestId('new-variable-name').at(-1);

  if (!nameField) {
    throw new Error('No name field found');
  }

  await user.click(nameField);
  await user.type(nameField, value);
  await user.tab();
};

const editLastNewVariableValue = async (user: UserEvent, value: string) => {
  const valueField = screen.getAllByTestId('new-variable-value').at(-1);

  if (!valueField) {
    throw new Error('No value field found');
  }

  await user.click(valueField);
  await user.type(valueField, value);
  await user.tab();
};

const TestSelectionControls: React.FC = () => {
  const {selectElementInstance, clearSelection} =
    useProcessInstanceElementSelection();
  return (
    <>
      <button
        type="button"
        onClick={() =>
          selectElementInstance({
            elementId: 'different_flow_node',
            elementInstanceKey: 'different_instance_id',
          })
        }
      >
        select different scope
      </button>
      <button type="button" onClick={() => clearSelection()}>
        clear selection
      </button>
    </>
  );
};

const getWrapper = (
  initialEntries: React.ComponentProps<
    typeof MemoryRouter
  >['initialEntries'] = [Paths.processInstance('1')],
) => {
  const Wrapper: React.FC<{children?: React.ReactNode}> = ({children}) => {
    useEffect(() => {
      return () => {
        modificationsStore.reset();
      };
    }, []);

    return (
      <ProcessDefinitionKeyContext.Provider value="123">
        <QueryClientProvider client={getMockQueryClient()}>
          <MemoryRouter initialEntries={initialEntries}>
            <Routes>
              <Route
                path={Paths.processInstance()}
                element={
                  <>
                    <TestSelectionControls />
                    {children}
                  </>
                }
              />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>
      </ProcessDefinitionKeyContext.Provider>
    );
  };
  return Wrapper;
};

describe('Undo variable modifications from different scope', () => {
  const mockProcessInstance: ProcessInstance = {
    processInstanceKey: 'instance_id',
    state: 'ACTIVE',
    startDate: '2018-06-21',
    processDefinitionKey: '2',
    processDefinitionVersion: 1,
    processDefinitionId: 'someKey',
    tenantId: '<default>',
    processDefinitionName: 'someProcessName',
    hasIncident: false,
  };

  beforeEach(async () => {
    mockFetchProcessInstance().withSuccess(mockProcessInstance);
    mockFetchProcessDefinitionXml().withSuccess('');
  });

  it('should preserve earlier edit modifications after undoing from a different scope', async () => {
    modificationsStore.enableModificationMode();
    modificationsStore.addModification(INITIAL_ADD_MODIFICATION);

    mockSearchJobs().withSuccess({items: [], page: {totalItems: 0}});
    mockSearchJobs().withSuccess({items: [], page: {totalItems: 0}});

    mockSearchVariables().withSuccess({
      items: [
        createVariable({name: 'foo', value: '"bar"', isTruncated: false}),
        createVariable({name: 'test', value: '123', isTruncated: false}),
      ],
      page: {totalItems: 2},
    });

    const {user} = render(
      <VariablePanel setListenerTabVisibility={vi.fn()} />,
      {wrapper: getWrapper()},
    );

    expect(await screen.findByDisplayValue('"bar"')).toBeInTheDocument();
    expect(screen.getByDisplayValue('123')).toBeInTheDocument();

    await editExistingVariableValue(user, 'foo', '1');
    await waitFor(() => {
      expect(screen.getByDisplayValue('1')).toBeInTheDocument();
    });

    await editExistingVariableValue(user, 'test', '2');
    expect(screen.getByDisplayValue('2')).toBeInTheDocument();

    await editExistingVariableValue(user, 'foo', '3');
    expect(screen.getByDisplayValue('3')).toBeInTheDocument();

    act(() => {
      modificationsStore.removeLastModification();
    });
    expect(screen.getByDisplayValue('1')).toBeInTheDocument();

    mockSearchVariables().withSuccess({
      items: [],
      page: {totalItems: 0},
    });

    mockFetchElementInstance('different_instance_id').withSuccess({
      elementInstanceKey: 'different_instance_id',
      elementId: 'different_flow_node',
      elementName: 'Different Flow Node',
      type: 'SERVICE_TASK',
      state: 'ACTIVE',
      startDate: '2018-06-21',
      processDefinitionId: 'someKey',
      processInstanceKey: 'instance_id',
      processDefinitionKey: '2',
      hasIncident: false,
      tenantId: '<default>',
    });

    await user.click(
      screen.getByRole('button', {name: /select different scope/i}),
    );

    act(() => {
      modificationsStore.removeLastModification();
    });

    mockSearchVariables().withSuccess({
      items: [
        createVariable({name: 'foo', value: '"bar"', isTruncated: false}),
        createVariable({name: 'test', value: '123', isTruncated: false}),
      ],
      page: {totalItems: 2},
    });

    await user.click(screen.getByRole('button', {name: /clear selection/i}));

    expect(screen.getByDisplayValue('1')).toBeInTheDocument();
    expect(screen.getByDisplayValue('123')).toBeInTheDocument();
  });

  it('should preserve earlier added variable modifications after undoing from a different scope', async () => {
    vi.useFakeTimers({shouldAdvanceTime: true});
    modificationsStore.enableModificationMode();
    modificationsStore.addModification(INITIAL_ADD_MODIFICATION);

    mockSearchJobs().withSuccess({items: [], page: {totalItems: 0}});
    mockSearchJobs().withSuccess({items: [], page: {totalItems: 0}});
    mockSearchJobs().withSuccess({items: [], page: {totalItems: 0}});

    mockSearchVariables().withSuccess({
      items: [],
      page: {totalItems: 0},
    });

    const {user} = render(
      <VariablePanel setListenerTabVisibility={vi.fn()} />,
      {wrapper: getWrapper()},
    );

    await waitFor(() => {
      expect(screen.getByRole('button', {name: /add variable/i})).toBeEnabled();
    });

    await user.click(screen.getByRole('button', {name: /add variable/i}));
    expect(await screen.findByTestId('new-variable-name')).toBeInTheDocument();
    await editLastNewVariableName(user, 'test2');
    await editLastNewVariableValue(user, '1');

    expect(screen.getByDisplayValue('test2')).toBeInTheDocument();
    expect(screen.getByDisplayValue('1')).toBeInTheDocument();

    await user.click(screen.getByRole('button', {name: /add variable/i}));
    await waitFor(() => {
      expect(screen.getAllByTestId('new-variable-name')).toHaveLength(2);
    });
    await editLastNewVariableName(user, 'test3');
    await editLastNewVariableValue(user, '2');

    expect(screen.getByDisplayValue('test3')).toBeInTheDocument();
    expect(screen.getByDisplayValue('2')).toBeInTheDocument();

    mockSearchVariables().withSuccess({
      items: [],
      page: {totalItems: 0},
    });

    mockFetchElementInstance('different_instance_id').withSuccess({
      elementInstanceKey: 'different_instance_id',
      elementId: 'different_flow_node',
      elementName: 'Different Flow Node',
      type: 'SERVICE_TASK',
      state: 'ACTIVE',
      startDate: '2018-06-21',
      processDefinitionId: 'someKey',
      processInstanceKey: 'instance_id',
      processDefinitionKey: '2',
      hasIncident: false,
      tenantId: '<default>',
    });

    await user.click(
      screen.getByRole('button', {name: /select different scope/i}),
    );

    act(() => {
      modificationsStore.removeLastModification();
    });

    mockSearchVariables().withSuccess({
      items: [],
      page: {totalItems: 0},
    });

    await user.click(screen.getByRole('button', {name: /clear selection/i}));

    expect(screen.getByDisplayValue('test2')).toBeInTheDocument();
    expect(screen.getByDisplayValue('1')).toBeInTheDocument();

    expect(screen.queryByDisplayValue('test3')).not.toBeInTheDocument();
    expect(screen.queryByDisplayValue('2')).not.toBeInTheDocument();

    vi.clearAllTimers();
    vi.useRealTimers();
  });
});
