/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {z} from 'zod';
import {
	advancedDateTimeFilterSchema,
	API_VERSION,
	getEnumFilterSchema,
	getQueryRequestBodySchema,
	getQueryResponseBodySchema,
	type Endpoint,
} from '../common';

const incidentErrorTypeSchema = z.enum([
	'UNSPECIFIED',
	'UNKNOWN',
	'IO_MAPPING_ERROR',
	'JOB_NO_RETRIES',
	'EXECUTION_LISTENER_NO_RETRIES',
	'TASK_LISTENER_NO_RETRIES',
	'CONDITION_ERROR',
	'EXTRACT_VALUE_ERROR',
	'CALLED_ELEMENT_ERROR',
	'UNHANDLED_ERROR_EVENT',
	'MESSAGE_SIZE_EXCEEDED',
	'CALLED_DECISION_ERROR',
	'DECISION_EVALUATION_ERROR',
	'FORM_NOT_FOUND',
	'RESOURCE_NOT_FOUND',
]);
type IncidentErrorType = z.infer<typeof incidentErrorTypeSchema>;

const incidentStateSchema = z.enum(['ACTIVE', 'MIGRATED', 'RESOLVED', 'PENDING']);
type IncidentState = z.infer<typeof incidentStateSchema>;

const incidentSchema = z.object({
	processDefinitionId: z.string(),
	errorType: incidentErrorTypeSchema,
	errorMessage: z.string(),
	elementId: z.string(),
	creationTime: z.string(),
	state: incidentStateSchema,
	tenantId: z.string(),
	incidentKey: z.string(),
	processDefinitionKey: z.string(),
	processInstanceKey: z.string(),
	elementInstanceKey: z.string(),
	jobKey: z.string().optional(),
});
type Incident = z.infer<typeof incidentSchema>;

const getIncidentProcessInstanceStatisticsByError: Endpoint = {
	method: 'POST',
	getUrl: () => `/${API_VERSION}/incidents/statistics/process-instances-by-error`,
};

const getIncidentProcessInstanceStatisticsByDefinition: Endpoint = {
	method: 'POST',
	getUrl: () => `/${API_VERSION}/incidents/statistics/process-instances-by-definition`,
};

const incidentProcessInstanceStatisticsByErrorSchema = z.object({
	errorHashCode: z.number(),
	errorMessage: z.string(),
	activeInstancesWithErrorCount: z.number(),
});

const incidentProcessInstanceStatisticsByDefinitionSchema = z.object({
	processDefinitionId: z.string(),
	processDefinitionKey: z.number(),
	processDefinitionName: z.string().optional(),
	processDefinitionVersion: z.number(),
	tenantId: z.string(),
	activeInstancesWithErrorCount: z.number(),
});

type IncidentProcessInstanceStatisticsByError = z.infer<typeof incidentProcessInstanceStatisticsByErrorSchema>;

type IncidentProcessInstanceStatisticsByDefinition = z.infer<
	typeof incidentProcessInstanceStatisticsByDefinitionSchema
>;

const getIncidentProcessInstanceStatisticsByErrorRequestBodySchema = getQueryRequestBodySchema({
	sortFields: ['errorMessage', 'activeInstancesWithErrorCount'] as const,
	filter: z.never(),
});

const incidentProcessInstanceStatisticsByDefinitionFilterSchema = z.object({
	errorHashCode: z.number(),
});

const getIncidentProcessInstanceStatisticsByDefinitionRequestBodySchema = getQueryRequestBodySchema({
	sortFields: ['processDefinitionKey', 'activeInstancesWithErrorCount', 'tenantId'] as const,
	filter: incidentProcessInstanceStatisticsByDefinitionFilterSchema,
});

type GetIncidentProcessInstanceStatisticsByErrorRequestBody = z.infer<
	typeof getIncidentProcessInstanceStatisticsByErrorRequestBodySchema
>;

type GetIncidentProcessInstanceStatisticsByDefinitionRequestBody = z.infer<
	typeof getIncidentProcessInstanceStatisticsByDefinitionRequestBodySchema
>;

const getIncidentProcessInstanceStatisticsByErrorResponseBodySchema = getQueryResponseBodySchema(
	incidentProcessInstanceStatisticsByErrorSchema,
);

const getIncidentProcessInstanceStatisticsByDefinitionResponseBodySchema = getQueryResponseBodySchema(
	incidentProcessInstanceStatisticsByDefinitionSchema,
);

type GetIncidentProcessInstanceStatisticsByErrorResponseBody = z.infer<
	typeof getIncidentProcessInstanceStatisticsByErrorResponseBodySchema
>;

type GetIncidentProcessInstanceStatisticsByDefinitionResponseBody = z.infer<
	typeof getIncidentProcessInstanceStatisticsByDefinitionResponseBodySchema
>;

const queryIncidentsRequestBodySchema = getQueryRequestBodySchema({
	sortFields: [
		'incidentKey',
		'processDefinitionKey',
		'processDefinitionId',
		'processInstanceKey',
		'errorType',
		'errorMessage',
		'elementId',
		'elementInstanceKey',
		'creationTime',
		'state',
		'jobKey',
		'tenantId',
	] as const,
	filter: z
		.object({
			errorType: getEnumFilterSchema(incidentErrorTypeSchema),
			creationTime: advancedDateTimeFilterSchema,
			state: getEnumFilterSchema(incidentStateSchema),
			...incidentSchema.pick({
				processDefinitionId: true,
				errorMessage: true,
				elementId: true,
				tenantId: true,
				incidentKey: true,
				processDefinitionKey: true,
				processInstanceKey: true,
				elementInstanceKey: true,
				jobKey: true,
			}).shape,
		})
		.partial(),
});
type QueryIncidentsRequestBody = z.infer<typeof queryIncidentsRequestBodySchema>;

const queryIncidentsResponseBodySchema = getQueryResponseBodySchema(incidentSchema);
type QueryIncidentsResponseBody = z.infer<typeof queryIncidentsResponseBodySchema>;

export {
	getIncidentProcessInstanceStatisticsByError,
	incidentProcessInstanceStatisticsByErrorSchema,
	getIncidentProcessInstanceStatisticsByErrorRequestBodySchema,
	getIncidentProcessInstanceStatisticsByErrorResponseBodySchema,
	getIncidentProcessInstanceStatisticsByDefinition,
	incidentProcessInstanceStatisticsByDefinitionSchema,
	getIncidentProcessInstanceStatisticsByDefinitionRequestBodySchema,
	getIncidentProcessInstanceStatisticsByDefinitionResponseBodySchema,
	queryIncidentsRequestBodySchema,
	queryIncidentsResponseBodySchema,
	incidentErrorTypeSchema,
	incidentStateSchema,
	incidentSchema,
};
export type {
	IncidentProcessInstanceStatisticsByError,
	GetIncidentProcessInstanceStatisticsByErrorRequestBody,
	GetIncidentProcessInstanceStatisticsByErrorResponseBody,
	IncidentProcessInstanceStatisticsByDefinition,
	GetIncidentProcessInstanceStatisticsByDefinitionRequestBody,
	GetIncidentProcessInstanceStatisticsByDefinitionResponseBody,
	IncidentErrorType,
	IncidentState,
	Incident,
	QueryIncidentsRequestBody,
	QueryIncidentsResponseBody,
};
