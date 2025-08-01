/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import { FC, useEffect, useState } from "react";
import { Tag } from "@carbon/react";
import { UseEntityModalCustomProps } from "src/components/modal";
import useTranslate from "src/utility/localization";
import { useApi, useApiCall } from "src/utility/api";
import { searchRoles, Role } from "src/utility/api/roles";
import { TranslatedErrorInlineNotification } from "src/components/notifications/InlineNotification";
import styled from "styled-components";
import DropdownSearch from "src/components/form/DropdownSearch";
import FormModal from "src/components/modal/FormModal";
import { assignGroupRole, Group } from "src/utility/api/groups";
import { useNotifications } from "src/components/notifications";

const SelectedRoles = styled.div`
  margin-top: 0;
`;

const AssignRolesModal: FC<
  UseEntityModalCustomProps<{ id: Group["groupId"] }, { assignedRoles: Role[] }>
> = ({ entity: group, assignedRoles, onSuccess, open, onClose }) => {
  const { t, Translate } = useTranslate("groups");
  const { enqueueNotification } = useNotifications();
  const [selectedRoles, setSelectedRoles] = useState<Role[]>([]);
  const [loadingAssignRole, setLoadingAssignRole] = useState(false);

  const {
    data: roleSearchResults,
    loading,
    reload,
    error,
  } = useApi(searchRoles);

  const [callAssignRole] = useApiCall(assignGroupRole);

  const unassignedRoles =
    roleSearchResults?.items.filter(
      ({ roleId }) =>
        !assignedRoles.some((role) => role.roleId === roleId) &&
        !selectedRoles.some((role) => role.roleId === roleId),
    ) || [];

  const onSelectRole = (role: Role) => {
    setSelectedRoles([...selectedRoles, role]);
  };

  const onUnselectRole =
    ({ roleId }: Role) =>
    () => {
      setSelectedRoles(selectedRoles.filter((role) => role.roleId !== roleId));
    };

  const canSubmit = group && selectedRoles.length;

  const handleSubmit = async () => {
    if (!canSubmit) return;

    setLoadingAssignRole(true);

    const results = await Promise.all(
      selectedRoles.map(({ roleId }) =>
        callAssignRole({ roleId, groupId: group.id }),
      ),
    );

    setLoadingAssignRole(false);

    if (results.every(({ success }) => success)) {
      if (selectedRoles.length === 1) {
        enqueueNotification({
          kind: "success",
          title: t("roleAssigned"),
          subtitle: t("roleAssignedSuccessfully"),
        });
      } else {
        enqueueNotification({
          kind: "success",
          title: t("rolesAssigned"),
          subtitle: t("rolesAssignedSuccessfully"),
        });
      }
      onSuccess();
    }
  };

  useEffect(() => {
    if (open) {
      setSelectedRoles([]);
    }
  }, [open]);

  return (
    <FormModal
      headline={t("assignRole")}
      confirmLabel={t("assignRole")}
      loading={loadingAssignRole}
      loadingDescription={t("assigningRole")}
      open={open}
      onSubmit={handleSubmit}
      submitDisabled={!canSubmit}
      onClose={onClose}
      overflowVisible
    >
      <p>
        <Translate i18nKey="searchAndAssignRoleToGroup">
          Search and assign role to group
        </Translate>
      </p>
      {selectedRoles.length > 0 && (
        <SelectedRoles>
          {selectedRoles.map((role) => (
            <Tag
              key={role.roleId}
              onClose={onUnselectRole(role)}
              size="md"
              type="blue"
              filter
            >
              {role.roleId}
            </Tag>
          ))}
        </SelectedRoles>
      )}
      <DropdownSearch
        autoFocus
        items={unassignedRoles}
        itemTitle={({ roleId }) => roleId}
        itemSubTitle={({ name }) => name}
        placeholder={t("searchByRoleId")}
        onSelect={onSelectRole}
      />
      {!loading && error && (
        <TranslatedErrorInlineNotification
          title={t("rolesCouldNotLoad")}
          actionButton={{ label: t("retry"), onClick: reload }}
        />
      )}
    </FormModal>
  );
};

export default AssignRolesModal;
