/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import { FC } from "react";
import { Edit, TrashCan, Add } from "@carbon/react/icons";
import { C3EmptyState } from "@camunda/camunda-composite-components";
import useTranslate from "src/utility/localization";
import { usePaginatedApi } from "src/utility/api";
import Page, { PageHeader } from "src/components/layout/Page";
import EntityList from "src/components/entityList";
import { documentationHref } from "src/components/documentation";
import { TranslatedErrorInlineNotification } from "src/components/notifications/InlineNotification";
import useModal, { useEntityModal } from "src/components/modal/useModal";
import { AddModal } from "src/pages/mapping-rules/modals/add-modal";
import { searchMappingRule } from "src/utility/api/mapping-rules";
import DeleteModal from "src/pages/mapping-rules/modals/DeleteModal";
import EditModal from "src/pages/mapping-rules/modals/EditModal";

const List: FC = () => {
  const { t } = useTranslate("mappingRules");
  const {
    data: mappingRuleSearchResults,
    loading,
    reload,
    success,
    search,
    ...paginationProps
  } = usePaginatedApi(searchMappingRule);

  const [addMappingRule, addMappingRuleModal] = useModal(AddModal, reload);
  const [editMappingRule, editMappingRuleModal] = useEntityModal(
    EditModal,
    reload,
  );
  const [deleteMappingRule, deleteMappingRuleModal] = useEntityModal(
    DeleteModal,
    reload,
  );

  const shouldShowEmptyState =
    success && !search && !mappingRuleSearchResults?.items.length;

  const pageHeader = (
    <PageHeader
      title={t("mappingRules")}
      linkText={t("mappingRules")}
      linkUrl=""
      shouldShowDocumentationLink={!shouldShowEmptyState}
    />
  );

  if (shouldShowEmptyState) {
    return (
      <Page>
        {pageHeader}
        <C3EmptyState
          heading={t("noMappingRules")}
          description={t("mappingRuleJWTToken")}
          button={{
            label: t("createMappingRule"),
            onClick: addMappingRule,
            icon: Add,
          }}
          link={{
            href: documentationHref("https://docs.camunda.io/", ""),
            label: t("learnMoreMappingRule"),
          }}
        />
        {addMappingRuleModal}
      </Page>
    );
  }

  return (
    <Page>
      {pageHeader}
      <EntityList
        data={
          mappingRuleSearchResults == null ? [] : mappingRuleSearchResults.items
        }
        headers={[
          {
            header: t("mappingRuleId"),
            key: "mappingRuleId",
            isSortable: true,
          },
          { header: t("mappingRuleName"), key: "name", isSortable: true },
          { header: t("claimName"), key: "claimName", isSortable: true },
          { header: t("claimValue"), key: "claimValue", isSortable: true },
        ]}
        addEntityLabel={t("createMappingRule")}
        onAddEntity={addMappingRule}
        loading={loading}
        menuItems={[
          {
            label: t("edit"),
            icon: Edit,
            onClick: editMappingRule,
          },
          {
            label: t("delete"),
            icon: TrashCan,
            isDangerous: true,
            onClick: deleteMappingRule,
          },
        ]}
        searchKey="mappingRuleId"
        {...paginationProps}
      />
      {!loading && !success && (
        <TranslatedErrorInlineNotification
          title={t("loadMappingRulesError")}
          actionButton={{ label: t("retry"), onClick: reload }}
        />
      )}
      {addMappingRuleModal}
      {deleteMappingRuleModal}
      {editMappingRuleModal}
    </Page>
  );
};

export default List;
