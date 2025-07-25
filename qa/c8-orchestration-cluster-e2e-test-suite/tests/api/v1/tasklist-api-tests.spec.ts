/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {test} from 'fixtures';
import {expect} from '@playwright/test';
import {authAPI} from 'utils/apiHelpers';
import {createInstances, deploy} from 'utils/zeebeClient';

const baseURL = process.env.CORE_APPLICATION_URL;

test.beforeAll(async () => {
  await Promise.all([
    deploy([
      './resources/User_Task_Process_With_Form_API.bpmn',
      './resources/New Form.form',
    ]),
  ]);
  await createInstances('Form_User_Task_API', 1, 3);
  await authAPI('demo', 'demo');
});

test.describe('API tests', () => {
  test.use({
    storageState: 'utils/.auth',
    baseURL: baseURL,
  });

  test('Search for tasks', async ({request}) => {
    const response = await request.post('/v1/tasks/search');
    expect(response.status()).toBe(200);
  });

  test('Get a task via ID', async ({request}) => {
    let taskData: {id: string}[] = [];
    await expect(async () => {
      const response = await request.post('/v1/tasks/search');
      expect(response.status()).toBe(200);
      taskData = await response.json();
      expect(taskData.length).toBeGreaterThan(0);
    }).toPass({
      intervals: [3_000, 4_000, 5_000],
      timeout: 30_000,
    });
    const response = await request.get(`/v1/tasks/${taskData[0].id}`);
    expect(response.status()).toBe(200);
  });
});
