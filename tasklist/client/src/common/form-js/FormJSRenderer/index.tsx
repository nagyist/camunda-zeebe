/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {useCallback, useEffect, useRef, useState} from 'react';
import {FormManager} from 'common/form-js/formManager';
import {mergeVariables} from './mergeVariables';
import {ValidationMessage} from './ValidationMessage';
import {getFieldLabels} from './getFieldLabels';
import {usePrefersReducedMotion} from './usePrefersReducedMotion';
import styles from './styles.module.scss';
import '@bpmn-io/form-js-viewer/dist/assets/form-js-base.css';
import '@bpmn-io/form-js-carbon-styles/src/carbon-styles.scss';
import type {DocumentReference} from '@vzeta/camunda-api-zod-schemas/8.8';
import set from 'lodash/set';
import {FormLevelErrorMessage} from './FormLevelErrorMessage';
import {Stack} from '@carbon/react';
import {toHumanReadableBytes} from 'common/document-handling/toHumanReadableBytes';
import {useTranslation} from 'react-i18next';
import {getClientConfig} from 'common/config/getClientConfig';
import type {PartialVariable} from 'common/types';
import {extractFilePath} from './extractFilePath';

type Props = {
  handleSubmit: (variables: PartialVariable[]) => Promise<void>;
  handleFileUpload?: (
    files: Map<string, File[]>,
  ) => Promise<Map<string, DocumentReference[]>>;
  schema: string;
  data?: Record<string, unknown>;
  readOnly?: boolean;
  onMount?: (formManager: FormManager) => void;
  onRender?: () => void;
  onImportError?: () => void;
  onSubmitStart?: () => void;
  onSubmitError?: (error: unknown) => void;
  onSubmitSuccess?: () => void;
  onValidationError?: () => void;
};

function htmlDomId(
  fieldId: string,
  formId?: string,
  indices?: string[],
): string {
  const result = ['fjs-form'];
  if (formId) {
    result.push('-', formId);
  }
  result.push('-', fieldId);
  if (indices) {
    result.push(indices.join(''));
  }
  return result.join('');
}

function useScrollToError(manager: FormManager) {
  const prefersReducedMotion = usePrefersReducedMotion();
  return useCallback(
    (fieldId: string) => {
      if (prefersReducedMotion) {
        return;
      }
      const form = manager.get('form');
      const ffr = manager.get('formFieldRegistry');
      const field = ffr.get(fieldId);
      const indicies: string[] = [];
      if (field._path.length > 2) {
        let parent = field;
        while (parent._path.length > 2) {
          parent = ffr.get(parent._parent);
          if (parent.type === 'dynamiclist') {
            indicies.push('_0');
          }
        }
      }
      let firstInvalidDomId: string | undefined;
      if (field.type === 'radio' || field.type === 'checklist') {
        firstInvalidDomId = htmlDomId(fieldId, form._id, [...indicies, '-0']);
      } else if (field.type === 'datetime') {
        firstInvalidDomId = htmlDomId(fieldId, form._id, [
          ...indicies,
          '-date',
        ]);
      } else {
        firstInvalidDomId = htmlDomId(fieldId, form._id, indicies);
      }
      if (firstInvalidDomId) {
        document
          .getElementById(firstInvalidDomId)
          ?.scrollIntoView({behavior: 'auto', block: 'center'});
      }
    },
    [manager, prefersReducedMotion],
  );
}

function injectFileMetadataIntoData(options: {
  data: Record<string, unknown>;
  fileMetadata: Map<string, DocumentReference[]>;
  pathsToInject: Map<string, string>;
}): Record<string, unknown> {
  const {data, fileMetadata, pathsToInject} = options;
  let result = structuredClone(data);

  pathsToInject.forEach((filepickerPath, fileKey) => {
    const metadata = fileMetadata.get(fileKey);

    if (metadata === undefined) {
      return;
    }

    result = set(result, filepickerPath, metadata);
  });

  return result;
}

const FormJSRenderer: React.FC<Props> = ({
  handleSubmit,
  schema,
  data = {},
  readOnly,
  onMount,
  onRender,
  onImportError,
  onSubmitStart,
  onSubmitError,
  onSubmitSuccess,
  onValidationError,
  handleFileUpload = () => Promise.resolve(new Map()),
}) => {
  const formManagerRef = useRef<FormManager>(new FormManager());
  const formContainerRef = useRef<HTMLDivElement | null>(null);
  const [invalidFields, setInvalidFields] = useState<
    {ids: string[]; labels: string[]} | undefined
  >();
  const [hasLargeFilePayload, setHasLargeFilePayload] = useState(false);
  const scrollToError = useScrollToError(formManagerRef.current);
  const hasInvalidFields = invalidFields !== undefined;
  const {t} = useTranslation();

  useEffect(() => {
    const formManager = formManagerRef.current;

    onMount?.(formManager);
  }, [onMount]);

  useEffect(() => {
    function render() {
      const formManager = formManagerRef.current;
      const container = formContainerRef.current;

      if (container === null) {
        return;
      }

      onRender?.();
      formManager.render({
        container,
        schema,
        data,
        onImportError,
        onSubmit: async ({
          data: newData,
          errors,
          files = new Map<string, File[]>(),
        }) => {
          onSubmitStart?.();
          setInvalidFields(undefined);
          setHasLargeFilePayload(false);
          const hasFieldErrors = Object.keys(errors).length > 0;

          if (hasFieldErrors) {
            onValidationError?.();
            const fieldIds = Object.keys(errors);
            setInvalidFields({
              ids: fieldIds,
              labels: getFieldLabels(formManager, fieldIds),
            });

            if (fieldIds.length > 0) {
              scrollToError(fieldIds[0]);
            }
          }

          const totalFilePayloadSize = Array.from(files.values())
            .flat()
            .map((file) => file.size)
            .reduce((total, itemSize) => total + itemSize, 0);
          const hasLargeFilePayload =
            totalFilePayloadSize > getClientConfig().maxRequestSize;
          if (hasLargeFilePayload) {
            onValidationError?.();
            setHasLargeFilePayload(true);
          }

          if (hasFieldErrors || hasLargeFilePayload) {
            return;
          }

          try {
            const enrichedData =
              files.size === 0
                ? newData
                : injectFileMetadataIntoData({
                    data: newData,
                    fileMetadata: await handleFileUpload(files),
                    pathsToInject: extractFilePath(newData),
                  });
            const variables = Object.entries(
              mergeVariables(data, enrichedData),
            ).map(([name, value]) => ({
              name,
              value: JSON.stringify(value),
            }));

            await handleSubmit(variables);
            onSubmitSuccess?.();
          } catch (error) {
            onSubmitError?.(error);
          }
        },
      });
    }

    render();
  }, [
    schema,
    handleSubmit,
    handleFileUpload,
    onRender,
    onImportError,
    onSubmitStart,
    onSubmitSuccess,
    onSubmitError,
    onValidationError,
    data,
    scrollToError,
  ]);

  useEffect(() => {
    const formManager = formManagerRef.current;

    return () => {
      formManager.detach();
    };
  }, []);

  useEffect(() => {
    const formManager = formManagerRef.current;

    formManager.setReadOnly(Boolean(readOnly));
  }, [readOnly]);

  return (
    <>
      <div className={styles.container}>
        <div ref={formContainerRef} className={styles.formRoot} />
      </div>

      {hasInvalidFields || hasLargeFilePayload ? (
        <Stack
          orientation="vertical"
          className={styles.formLevelErrorContainer}
          gap={3}
        >
          <hr className={styles.hr} />
          {hasInvalidFields ? (
            <ValidationMessage
              fieldIds={invalidFields.ids}
              fieldLabels={invalidFields.labels}
            />
          ) : null}
          {hasLargeFilePayload ? (
            <FormLevelErrorMessage
              readableMessage={t('formJSLargeFilePayloadError', {
                size: toHumanReadableBytes(getClientConfig().maxRequestSize),
              })}
            />
          ) : null}
        </Stack>
      ) : null}
    </>
  );
};

export {FormJSRenderer};
