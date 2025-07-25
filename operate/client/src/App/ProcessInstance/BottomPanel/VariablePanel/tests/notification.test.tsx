/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {VariablePanel} from '../index';
import {
  fireEvent,
  render,
  screen,
  waitFor,
  waitForElementToBeRemoved,
  within,
} from 'modules/testing-library';
import {flowNodeSelectionStore} from 'modules/stores/flowNodeSelection';
import {variablesStore} from 'modules/stores/variables';
import {processInstanceDetailsStore} from 'modules/stores/processInstanceDetails';
import {flowNodeMetaDataStore} from 'modules/stores/flowNodeMetaData';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {
  createInstance,
  createVariable,
  createVariableV2,
  mockProcessWithInputOutputMappingsXML,
} from 'modules/testUtils';
import {modificationsStore} from 'modules/stores/modifications';
import {mockFetchVariables} from 'modules/mocks/api/processInstances/fetchVariables';
import {mockFetchFlowNodeMetadata} from 'modules/mocks/api/processInstances/fetchFlowNodeMetaData';
import {singleInstanceMetadata} from 'modules/mocks/metadata';
import {mockApplyOperation} from 'modules/mocks/api/processInstances/operations';
import {useEffect} from 'react';
import {Paths} from 'modules/Routes';
import {notificationsStore} from 'modules/stores/notifications';
import {QueryClientProvider} from '@tanstack/react-query';
import {getMockQueryClient} from 'modules/react-query/mockQueryClient';
import {mockFetchFlownodeInstancesStatistics} from 'modules/mocks/api/v2/flownodeInstances/fetchFlownodeInstancesStatistics';
import {ProcessDefinitionKeyContext} from 'App/Processes/ListView/processDefinitionKeyContext';
import {mockFetchProcessInstanceListeners} from 'modules/mocks/api/processInstances/fetchProcessInstanceListeners';
import {noListeners} from 'modules/mocks/mockProcessInstanceListeners';
import {mockFetchProcessDefinitionXml} from 'modules/mocks/api/v2/processDefinitions/fetchProcessDefinitionXml';
import {init} from 'modules/utils/flowNodeMetadata';
import {type ProcessInstance} from '@vzeta/camunda-api-zod-schemas/8.8';
import {mockFetchProcessInstance} from 'modules/mocks/api/v2/processInstances/fetchProcessInstance';
import {mockFetchProcessInstance as mockFetchProcessInstanceDeprecated} from 'modules/mocks/api/processInstances/fetchProcessInstance';
import {mockSearchVariables} from 'modules/mocks/api/v2/variables/searchVariables';

vi.mock('modules/stores/notifications', () => ({
  notificationsStore: {
    displayNotification: vi.fn(() => () => {}),
  },
}));

const getWrapper = (
  initialEntries: React.ComponentProps<
    typeof MemoryRouter
  >['initialEntries'] = [Paths.processInstance('1')],
) => {
  const Wrapper: React.FC<{children?: React.ReactNode}> = ({children}) => {
    useEffect(() => {
      return () => {
        variablesStore.reset();
        flowNodeSelectionStore.reset();
        flowNodeMetaDataStore.reset();
        modificationsStore.reset();
        processInstanceDetailsStore.reset();
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

describe('VariablePanel', () => {
  beforeEach(() => {
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

    const mockProcessInstanceDeprecated = createInstance();

    mockFetchProcessInstance().withSuccess(mockProcessInstance);
    mockFetchProcessInstance().withSuccess(mockProcessInstance);
    mockFetchProcessInstanceDeprecated().withSuccess(
      mockProcessInstanceDeprecated,
    );
    mockFetchProcessInstanceDeprecated().withSuccess(
      mockProcessInstanceDeprecated,
    );
    mockFetchProcessInstanceDeprecated().withSuccess(
      mockProcessInstanceDeprecated,
    );

    const statistics = [
      {
        elementId: 'TEST_FLOW_NODE',
        active: 0,
        canceled: 0,
        incidents: 0,
        completed: 1,
      },
      {
        elementId: 'Activity_0qtp1k6',
        active: 0,
        canceled: 0,
        incidents: 1,
        completed: 0,
      },
    ];

    mockFetchFlownodeInstancesStatistics().withSuccess({
      items: statistics,
    });
    mockFetchFlownodeInstancesStatistics().withSuccess({
      items: statistics,
    });

    mockFetchVariables().withSuccess([createVariable()]);
    mockFetchVariables().withSuccess([createVariable()]);
    mockFetchVariables().withSuccess([createVariable()]);
    mockSearchVariables().withSuccess({
      items: [createVariableV2()],
      page: {
        totalItems: 1,
      },
    });
    mockFetchFlowNodeMetadata().withSuccess(singleInstanceMetadata);
    mockFetchProcessDefinitionXml().withSuccess(
      mockProcessWithInputOutputMappingsXML,
    );
    mockFetchProcessInstanceListeners().withSuccess(noListeners);

    init('process-instance', statistics);
    flowNodeSelectionStore.init();
    processInstanceDetailsStore.setProcessInstance(
      createInstance({
        id: 'instance_id',
        state: 'ACTIVE',
      }),
    );
  });

  afterEach(() => {
    vi.clearAllTimers();
  });

  it('should display error notification if add variable operation could not be created', async () => {
    mockFetchVariables().withSuccess([createVariable()]);

    const {user} = render(
      <VariablePanel setListenerTabVisibility={vi.fn()} />,
      {wrapper: getWrapper()},
    );
    await waitFor(() =>
      expect(
        screen.getByRole('button', {
          name: /add variable/i,
        }),
      ).toBeEnabled(),
    );

    await user.click(
      screen.getByRole('button', {
        name: /add variable/i,
      }),
    );
    expect(
      screen.queryByRole('button', {
        name: /add variable/i,
      }),
    ).not.toBeInTheDocument();

    await user.type(
      screen.getByRole('textbox', {
        name: /name/i,
      }),
      'foo',
    );
    await user.type(
      screen.getByRole('textbox', {
        name: /value/i,
      }),
      '"bar"',
    );

    mockApplyOperation().withDelayedServerError();

    await waitFor(() =>
      expect(
        screen.getByRole('button', {
          name: /save variable/i,
        }),
      ).toBeEnabled(),
    );

    await user.click(
      screen.getByRole('button', {
        name: /save variable/i,
      }),
    );

    await waitForElementToBeRemoved(
      within(screen.getByTestId('foo')).queryByTestId(
        'variable-operation-spinner',
      ),
    );

    expect(
      await screen.findByRole('button', {
        name: /add variable/i,
      }),
    ).toBeInTheDocument();

    expect(notificationsStore.displayNotification).toHaveBeenCalledWith({
      isDismissable: true,
      kind: 'error',
      title: 'Variable could not be saved',
    });
  });

  it('should display error notification if add variable operation could not be created because of auth error', async () => {
    const statistics = [
      {
        elementId: 'TEST_FLOW_NODE',
        active: 0,
        canceled: 0,
        incidents: 0,
        completed: 1,
      },
      {
        elementId: 'Activity_0qtp1k6',
        active: 0,
        canceled: 0,
        incidents: 1,
        completed: 0,
      },
    ];

    mockFetchVariables().withSuccess([createVariable()]);
    mockFetchFlownodeInstancesStatistics().withSuccess({
      items: statistics,
    });

    const {user} = render(
      <VariablePanel setListenerTabVisibility={vi.fn()} />,
      {wrapper: getWrapper()},
    );
    await waitFor(() =>
      expect(
        screen.getByRole('button', {
          name: /add variable/i,
        }),
      ).toBeEnabled(),
    );

    await user.click(
      screen.getByRole('button', {
        name: /add variable/i,
      }),
    );
    expect(
      screen.queryByRole('button', {
        name: /add variable/i,
      }),
    ).not.toBeInTheDocument();

    await user.type(
      screen.getByRole('textbox', {
        name: /name/i,
      }),
      'foo',
    );
    await user.type(
      screen.getByRole('textbox', {
        name: /value/i,
      }),
      '"bar"',
    );

    mockApplyOperation().withDelayedServerError(403);

    await waitFor(() =>
      expect(
        screen.getByRole('button', {
          name: /save variable/i,
        }),
      ).toBeEnabled(),
    );

    await user.click(
      screen.getByRole('button', {
        name: /save variable/i,
      }),
    );

    await waitForElementToBeRemoved(
      within(screen.getByTestId('foo')).queryByTestId(
        'variable-operation-spinner',
      ),
    );

    expect(
      await screen.findByRole('button', {
        name: /add variable/i,
      }),
    ).toBeInTheDocument();

    expect(notificationsStore.displayNotification).toHaveBeenCalledWith({
      isDismissable: true,
      kind: 'error',
      title: 'Variable could not be saved',
      subtitle: 'You do not have permission',
    });
  });

  it('should display error notification if add variable operation fails', async () => {
    vi.useFakeTimers({shouldAdvanceTime: true});

    const {user} = render(
      <VariablePanel setListenerTabVisibility={vi.fn()} />,
      {wrapper: getWrapper()},
    );
    await waitFor(() =>
      expect(
        screen.getByRole('button', {
          name: /add variable/i,
        }),
      ).toBeEnabled(),
    );

    await user.click(
      screen.getByRole('button', {
        name: /add variable/i,
      }),
    );
    expect(
      screen.queryByRole('button', {
        name: /add variable/i,
      }),
    ).not.toBeInTheDocument();

    await user.type(
      screen.getByRole('textbox', {
        name: /name/i,
      }),
      'foo',
    );
    await user.type(
      screen.getByRole('textbox', {
        name: /value/i,
      }),
      '"bar"',
    );

    mockFetchVariables().withSuccess([createVariable()]);

    mockFetchFlowNodeMetadata().withSuccess(singleInstanceMetadata);

    vi.runOnlyPendingTimers();
    await waitFor(() =>
      expect(
        screen.getByRole('button', {
          name: /save variable/i,
        }),
      ).toBeEnabled(),
    );

    mockFetchVariables().withSuccess([createVariable()]);
    mockApplyOperation().withNetworkError();

    fireEvent.click(
      screen.getByRole('button', {
        name: /save variable/i,
      }),
    );

    expect(
      within(screen.getByTestId('foo')).getByTestId(
        'variable-operation-spinner',
      ),
    ).toBeInTheDocument();

    vi.runOnlyPendingTimers();

    await waitForElementToBeRemoved(screen.queryByTestId('foo'));

    expect(
      screen.getByRole('button', {
        name: /add variable/i,
      }),
    ).toBeInTheDocument();

    expect(notificationsStore.displayNotification).toHaveBeenCalledWith({
      isDismissable: true,
      kind: 'error',
      title: 'Variable could not be saved',
    });

    vi.clearAllTimers();
    vi.useRealTimers();
  });
});
