/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {PERMISSIONS} from 'modules/constants';
import {useProcessInstanceDeprecated} from '../processInstance/deprecated/useProcessInstanceDeprecated';
import type {
  ProcessInstanceEntity,
  ResourceBasedPermissionDto,
} from 'modules/types/operate';

const permissionsParser = (processInstance: ProcessInstanceEntity) => {
  if (!window.clientConfig?.resourcePermissionsEnabled) {
    return PERMISSIONS;
  }

  return processInstance.permissions;
};

const usePermissions = () =>
  useProcessInstanceDeprecated<ResourceBasedPermissionDto[] | undefined | null>(
    permissionsParser,
  );

export {permissionsParser, usePermissions};
