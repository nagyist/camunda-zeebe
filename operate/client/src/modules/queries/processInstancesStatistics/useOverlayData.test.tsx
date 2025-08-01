/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {renderHook, waitFor} from '@testing-library/react';
import {QueryClientProvider} from '@tanstack/react-query';
import {useProcessInstancesOverlayData} from './useOverlayData';
import {mockFetchProcessInstancesStatistics} from 'modules/mocks/api/v2/processInstances/fetchProcessInstancesStatistics';
import {getMockQueryClient} from 'modules/react-query/mockQueryClient';
import {useFilters} from 'modules/hooks/useFilters';
import * as filterModule from 'modules/hooks/useProcessInstancesFilters';
import type {ProcessInstanceFilters} from 'modules/utils/filter/shared';

vi.mock('modules/hooks/useFilters');

const mockedUseFilters = vi.mocked(useFilters);

describe('useProcessInstancesOverlayStatistics', () => {
  const wrapper = ({children}: {children: React.ReactNode}) => (
    <QueryClientProvider client={getMockQueryClient()}>
      {children}
    </QueryClientProvider>
  );

  const mockFilters: ProcessInstanceFilters = {
    flowNodeId: 'messageCatchEvent',
  };

  beforeEach(() => {
    vi.spyOn(filterModule, 'useProcessInstanceFilters').mockReturnValue({});
    mockedUseFilters.mockReturnValue({
      getFilters: () => mockFilters,
      setFilters: vi.fn(),
      areProcessInstanceStatesApplied: vi.fn(),
      areDecisionInstanceStatesApplied: vi.fn(),
    });
  });

  it('should fetch process instances overlay statistics successfully excluding completed non-end events', async () => {
    const mockData = [
      {
        elementId: 'messageCatchEvent',
        active: 2,
        canceled: 1,
        incidents: 3,
        completed: 4,
      },
    ];
    const mockOverlayData = [
      {
        flowNodeId: 'messageCatchEvent',
        payload: {
          count: 2,
          flowNodeState: 'active',
        },
        position: {
          bottom: 9,
          left: 0,
        },
        type: 'statistics-active',
      },
      {
        flowNodeId: 'messageCatchEvent',
        payload: {
          count: 3,
          flowNodeState: 'incidents',
        },
        position: {
          bottom: 9,
          right: 0,
        },
        type: 'statistics-incidents',
      },
      {
        flowNodeId: 'messageCatchEvent',
        payload: {
          count: 1,
          flowNodeState: 'canceled',
        },
        position: {
          top: -16,
          left: 0,
        },
        type: 'statistics-canceled',
      },
    ];

    mockFetchProcessInstancesStatistics().withSuccess({
      items: mockData,
    });

    const {result} = renderHook(
      () => useProcessInstancesOverlayData({}, 'process1'),
      {
        wrapper,
      },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toEqual(mockOverlayData);
  });

  it('should handle server error while fetching process instances overlay statistics', async () => {
    mockFetchProcessInstancesStatistics().withServerError();

    const {result} = renderHook(
      () => useProcessInstancesOverlayData({}, 'process1'),
      {
        wrapper,
      },
    );

    await waitFor(() => expect(result.current.isError).toBe(true));

    expect(result.current.error).toBeDefined();
    expect(result.current.error?.variant).toBe('failed-response');
    expect(result.current.error?.response).toBeDefined();
  });

  it('should handle network error while fetching process instances overlay statistics', async () => {
    mockFetchProcessInstancesStatistics().withNetworkError();

    const {result} = renderHook(
      () => useProcessInstancesOverlayData({}, 'process1'),
      {
        wrapper,
      },
    );

    await waitFor(() => expect(result.current.isError).toBe(true));

    expect(result.current.error).toBeDefined();
    expect(result.current.error?.variant).toBe('network-error');
    expect(result.current.error?.response).toBeNull();
  });

  it('should handle empty data', async () => {
    mockFetchProcessInstancesStatistics().withSuccess({
      items: [],
    });

    const {result} = renderHook(
      () => useProcessInstancesOverlayData({}, 'process1'),
      {
        wrapper,
      },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toEqual([]);
  });

  it('should handle loading state', async () => {
    mockFetchProcessInstancesStatistics().withDelay({
      items: [],
    });

    const {result} = renderHook(
      () => useProcessInstancesOverlayData({}, 'process1'),
      {
        wrapper,
      },
    );

    expect(result.current.isLoading).toBe(true);
  });

  it('should not fetch data when enabled is false', async () => {
    const {result} = renderHook(
      () => useProcessInstancesOverlayData({}, 'process1', false),
      {
        wrapper,
      },
    );

    expect(result.current.isLoading).toBe(false);
    expect(result.current.isFetched).toBe(false);
    expect(result.current.data).toBeUndefined();
  });

  it('should not fetch data when processDefinitionKey is undefined', async () => {
    const {result} = renderHook(() => useProcessInstancesOverlayData({}), {
      wrapper,
    });

    expect(result.current.isLoading).toBe(false);
    expect(result.current.isFetched).toBe(false);
    expect(result.current.data).toBeUndefined();
  });
});
