/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {VisuallyHiddenH1} from 'modules/components/VisuallyHiddenH1';
import {InstanceDetail} from '../../Layout/InstanceDetail';
import {Breadcrumb} from '../Breadcrumb/v2';
import {processInstanceDetailsStore} from 'modules/stores/processInstanceDetails';
import {observer} from 'mobx-react';
import {useProcessInstancePageParams} from '../useProcessInstancePageParams';
import {useLocation, useNavigate} from 'react-router-dom';
import {useEffect} from 'react';
import {modificationsStore} from 'modules/stores/modifications';
import {reaction, when} from 'mobx';
import {variablesStore} from 'modules/stores/variables';
import {incidentsStore} from 'modules/stores/incidents';
import {flowNodeInstanceStore} from 'modules/stores/flowNodeInstance';
import {instanceHistoryModificationStore} from 'modules/stores/instanceHistoryModification';
import {Locations} from 'modules/Routes';
import {processInstanceDetailsDiagramStore} from 'modules/stores/processInstanceDetailsDiagram';
import {flowNodeSelectionStore} from 'modules/stores/flowNodeSelection';
import {flowNodeTimeStampStore} from 'modules/stores/flowNodeTimeStamp';
import {ProcessInstanceHeader} from '../ProcessInstanceHeader/v2';
import {TopPanel} from '../TopPanel/v2';
import {BottomPanel, ModificationFooter, Buttons} from '../styled';
import {FlowNodeInstanceLog} from '../FlowNodeInstanceLog';
import {Button, Modal} from '@carbon/react';
import {tracking} from 'modules/tracking';
import {ModalStateManager} from 'modules/components/ModalStateManager';
import {ModificationSummaryModal} from '../ModificationSummaryModal/v2';
import {useCallbackPrompt} from 'modules/hooks/useCallbackPrompt';
import {LastModification} from '../LastModification';
import {VariablePanel} from '../BottomPanel/VariablePanel';
import {Forbidden} from 'modules/components/Forbidden';
import {notificationsStore} from 'modules/stores/notifications';
import {Frame} from 'modules/components/Frame';
import {processInstanceListenersStore} from 'modules/stores/processInstanceListeners';
import {ProcessDefinitionKeyContext} from 'App/Processes/ListView/processDefinitionKeyContext';
import {useProcessInstance} from 'modules/queries/processInstance/useProcessInstance';
import {useProcessTitle} from 'modules/queries/processInstance/useProcessTitle';
import {useCallHierarchy} from 'modules/queries/callHierarchy/useCallHierarchy';
import {HTTP_STATUS_FORBIDDEN} from 'modules/constants/statusCode';

const startPolling = (processInstanceId: ProcessInstanceEntity['id']) => {
  variablesStore.startPolling(processInstanceId, {runImmediately: true});
  processInstanceDetailsStore.startPolling(processInstanceId, {
    runImmediately: true,
  });
  incidentsStore.startPolling(processInstanceId, {
    runImmediately: true,
  });
  flowNodeInstanceStore.startPolling({runImmediately: true});
};

const stopPolling = () => {
  variablesStore.stopPolling();
  processInstanceDetailsStore.stopPolling();
  incidentsStore.stopPolling();
  flowNodeInstanceStore.stopPolling();
};

const ProcessInstance: React.FC = observer(() => {
  const {data: processInstance, error} = useProcessInstance();
  const {data: processTitle} = useProcessTitle();
  const {data: callHierarchy} = useCallHierarchy();
  const {processInstanceId = ''} = useProcessInstancePageParams();
  const navigate = useNavigate();
  const location = useLocation();

  const {isNavigationInterrupted, confirmNavigation, cancelNavigation} =
    useCallbackPrompt({
      shouldInterrupt: modificationsStore.isModificationModeEnabled,
    });

  useEffect(() => {
    const disposer = reaction(
      () => modificationsStore.isModificationModeEnabled,
      (isModificationModeEnabled) => {
        if (isModificationModeEnabled) {
          stopPolling();
        } else {
          instanceHistoryModificationStore.reset();
          startPolling(processInstanceId);
        }
      },
    );

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        startPolling(processInstanceId);
      } else {
        stopPolling();
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      disposer();
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [processInstanceId]);

  useEffect(() => {
    const {
      state: {processInstance},
    } = processInstanceDetailsStore;

    if (processInstanceId !== processInstance?.id) {
      processInstanceDetailsStore.init({
        id: processInstanceId,
        onRefetchFailure: () => {
          navigate(
            Locations.processes({
              active: true,
              incidents: true,
            }),
          );

          notificationsStore.displayNotification({
            kind: 'error',
            title: `Instance ${processInstanceId} could not be found`,
            isDismissable: true,
          });
        },
        onPollingFailure: () => {
          navigate(Locations.processes());

          notificationsStore.displayNotification({
            kind: 'success',
            title: 'Instance deleted',
            isDismissable: true,
          });
        },
      });
      flowNodeInstanceStore.init();
      processInstanceDetailsDiagramStore.init();
      flowNodeSelectionStore.init();
    }
  }, [processInstanceId, navigate, location]);

  useEffect(() => {
    return () => {
      instanceHistoryModificationStore.reset();
      processInstanceDetailsStore.reset();
      flowNodeInstanceStore.reset();
      processInstanceDetailsDiagramStore.reset();
      flowNodeTimeStampStore.reset();
      flowNodeSelectionStore.reset();
      modificationsStore.reset();
    };
  }, [processInstanceId]);

  useEffect(() => {
    let processTitleDisposer = when(
      () => !!processTitle,
      () => {
        document.title = processTitle ?? '';
      },
    );

    return () => {
      processTitleDisposer();
    };
  });

  const {
    isModificationModeEnabled,
    state: {modifications, status: modificationStatus},
  } = modificationsStore;

  const isBreadcrumbVisible = callHierarchy && callHierarchy.items.length > 0;

  const hasPendingModifications = modifications.length > 0;

  const {
    state: {isListenerTabSelected},
  } = processInstanceListenersStore;

  if (error?.response?.status === HTTP_STATUS_FORBIDDEN) {
    return <Forbidden />;
  }

  return (
    <ProcessDefinitionKeyContext.Provider
      value={processInstance?.processDefinitionKey}
    >
      <VisuallyHiddenH1>
        {`Operate Process Instance${
          isModificationModeEnabled ? ' - Modification Mode' : ''
        }`}
      </VisuallyHiddenH1>
      <Frame
        frame={{
          isVisible: isModificationModeEnabled,
          headerTitle: 'Process Instance Modification Mode',
        }}
      >
        {processInstance && (
          <InstanceDetail
            hasLoadingOverlay={modificationStatus === 'applying-modifications'}
            breadcrumb={
              isBreadcrumbVisible && callHierarchy ? (
                <Breadcrumb
                  callHierarchy={callHierarchy.items}
                  processInstance={processInstance}
                />
              ) : undefined
            }
            header={<ProcessInstanceHeader processInstance={processInstance} />}
            topPanel={<TopPanel />}
            bottomPanel={
              <BottomPanel $shouldExpandPanel={isListenerTabSelected}>
                <FlowNodeInstanceLog />
                <VariablePanel />
              </BottomPanel>
            }
            footer={
              isModificationModeEnabled ? (
                <ModificationFooter>
                  <LastModification />
                  <Buttons orientation="horizontal" gap={4}>
                    <ModalStateManager
                      renderLauncher={({setOpen}) => (
                        <Button
                          kind="secondary"
                          size="sm"
                          onClick={() => {
                            tracking.track({
                              eventName: 'discard-all-summary',
                              hasPendingModifications,
                            });
                            setOpen(true);
                          }}
                          data-testid="discard-all-button"
                        >
                          Discard All
                        </Button>
                      )}
                    >
                      {({open, setOpen}) => (
                        <Modal
                          modalHeading="Discard Modifications"
                          preventCloseOnClickOutside
                          danger
                          primaryButtonText="Discard"
                          secondaryButtonText="Cancel"
                          open={open}
                          onRequestClose={() => setOpen(false)}
                          onRequestSubmit={() => {
                            tracking.track({
                              eventName: 'discard-modifications',
                              hasPendingModifications,
                            });
                            modificationsStore.reset();
                            setOpen(false);
                          }}
                        >
                          <p>
                            About to discard all added modifications for
                            instance {processInstanceId}.
                          </p>
                          <p>Click "Discard" to proceed.</p>
                        </Modal>
                      )}
                    </ModalStateManager>
                    <ModalStateManager
                      renderLauncher={({setOpen}) => (
                        <Button
                          kind="primary"
                          size="sm"
                          onClick={() => {
                            tracking.track({
                              eventName: 'apply-modifications-summary',
                              hasPendingModifications,
                            });
                            setOpen(true);
                          }}
                          data-testid="apply-modifications-button"
                          disabled={!hasPendingModifications}
                        >
                          Apply Modifications
                        </Button>
                      )}
                    >
                      {({open, setOpen}) => (
                        <ModificationSummaryModal
                          open={open}
                          setOpen={setOpen}
                        />
                      )}
                    </ModalStateManager>
                  </Buttons>
                </ModificationFooter>
              ) : undefined
            }
            type="process"
          />
        )}
      </Frame>
      {isNavigationInterrupted && (
        <Modal
          open={isNavigationInterrupted}
          modalHeading="Leave Modification Mode"
          preventCloseOnClickOutside
          onRequestClose={cancelNavigation}
          secondaryButtonText="Stay"
          primaryButtonText="Leave"
          onRequestSubmit={() => {
            tracking.track({eventName: 'leave-modification-mode'});
            confirmNavigation();
          }}
        >
          <p>
            By leaving this page, all planned modification/s will be discarded.
          </p>
        </Modal>
      )}
    </ProcessDefinitionKeyContext.Provider>
  );
});

export {ProcessInstance};
