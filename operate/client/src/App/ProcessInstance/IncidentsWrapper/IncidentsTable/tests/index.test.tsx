/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {IncidentsTable} from '../index';
import {formatDate} from 'modules/utils/date';
import {render, screen, within} from 'modules/testing-library';
import {incidentsStore} from 'modules/stores/incidents';
import {Wrapper, incidentsMock, firstIncident, secondIncident} from './mocks';
import {mockFetchProcessDefinitionXml} from 'modules/mocks/api/v2/processDefinitions/fetchProcessDefinitionXml';
import {mockMe} from 'modules/mocks/api/v2/me';
import {createUser} from 'modules/testUtils';

describe('IncidentsTable', () => {
  it('should render the right column headers', () => {
    mockFetchProcessDefinitionXml().withSuccess('');
    incidentsStore.setIncidents(incidentsMock);

    render(<IncidentsTable />, {wrapper: Wrapper});

    expect(screen.getByText('Incident Type')).toBeInTheDocument();
    expect(screen.getByText('Failing Flow Node')).toBeInTheDocument();
    expect(screen.getByText('Job Id')).toBeInTheDocument();
    expect(screen.getByText('Creation Date')).toBeInTheDocument();
    expect(screen.getByText('Error Message')).toBeInTheDocument();
    expect(screen.getByText('Operations')).toBeInTheDocument();
    expect(screen.getByText('Root Cause Instance')).toBeInTheDocument();
  });

  it('should render the right column headers for restricted user', () => {
    mockFetchProcessDefinitionXml().withSuccess('');
    incidentsStore.setIncidents(incidentsMock);
    mockMe().withSuccess(createUser());

    render(<IncidentsTable />, {wrapper: Wrapper});

    expect(screen.getByText('Incident Type')).toBeInTheDocument();
    expect(screen.getByText('Failing Flow Node')).toBeInTheDocument();
    expect(screen.getByText('Job Id')).toBeInTheDocument();
    expect(screen.getByText('Creation Date')).toBeInTheDocument();
    expect(screen.getByText('Error Message')).toBeInTheDocument();
    expect(screen.getByText('Operations')).toBeInTheDocument();
    expect(screen.getByText('Root Cause Instance')).toBeInTheDocument();
  });

  it('should render the right column headers for restricted user (with resource-based permissions)', () => {
    mockFetchProcessDefinitionXml().withSuccess('');
    mockMe().withSuccess(createUser());
    vi.stubGlobal('clientConfig', {
      resourcePermissionsEnabled: true,
    });

    incidentsStore.setIncidents(incidentsMock);

    render(<IncidentsTable />, {wrapper: Wrapper});

    expect(screen.getByText('Incident Type')).toBeInTheDocument();
    expect(screen.getByText('Failing Flow Node')).toBeInTheDocument();
    expect(screen.getByText('Job Id')).toBeInTheDocument();
    expect(screen.getByText('Creation Date')).toBeInTheDocument();
    expect(screen.getByText('Error Message')).toBeInTheDocument();
    expect(screen.queryByText('Operations')).not.toBeInTheDocument();
    expect(screen.getByText('Root Cause Instance')).toBeInTheDocument();
  });

  it('should render incident details', () => {
    mockFetchProcessDefinitionXml().withSuccess('');
    incidentsStore.setIncidents(incidentsMock);

    render(<IncidentsTable />, {wrapper: Wrapper});
    let withinRow = within(
      screen.getByRole('row', {
        name: new RegExp(firstIncident!.errorType.name),
      }),
    );

    expect(
      withinRow.getByText(firstIncident!.errorType.name),
    ).toBeInTheDocument();
    expect(withinRow.getByText(firstIncident.flowNodeId)).toBeInTheDocument();
    expect(withinRow.getByText(firstIncident.jobId!)).toBeInTheDocument();
    expect(
      withinRow.getByText(formatDate(firstIncident.creationTime) || '--'),
    ).toBeInTheDocument();
    expect(withinRow.getByText(firstIncident.errorMessage)).toBeInTheDocument();
    expect(
      withinRow.getByRole('link', {
        description: /view root cause instance/i,
      }),
    ).toBeInTheDocument();
    expect(
      withinRow.queryByRole('button', {name: 'Retry Incident'}),
    ).not.toBeInTheDocument();
    withinRow = within(
      screen.getByRole('row', {
        name: new RegExp(secondIncident!.errorType.name),
      }),
    );
    expect(
      withinRow.getByText(secondIncident.errorType.name),
    ).toBeInTheDocument();
    expect(withinRow.getByText(secondIncident.flowNodeId)).toBeInTheDocument();
    expect(withinRow.getByText(secondIncident.jobId!)).toBeInTheDocument();
    expect(
      withinRow.getByText(formatDate(secondIncident.creationTime) || '--'),
    ).toBeInTheDocument();
    expect(
      withinRow.getByText(secondIncident.errorMessage),
    ).toBeInTheDocument();
    expect(
      withinRow.getByRole('button', {name: 'Retry Incident'}),
    ).toBeInTheDocument();
  });

  it('should render incident details (with resource-based permissions enabled)', () => {
    mockFetchProcessDefinitionXml().withSuccess('');
    mockMe().withSuccess(createUser());
    vi.stubGlobal('clientConfig', {
      resourcePermissionsEnabled: true,
    });

    incidentsStore.setIncidents(incidentsMock);

    render(<IncidentsTable />, {wrapper: Wrapper});
    let withinRow = within(
      screen.getByRole('row', {
        name: new RegExp(firstIncident.errorType.name),
      }),
    );

    expect(
      withinRow.getByText(firstIncident.errorType.name),
    ).toBeInTheDocument();
    expect(withinRow.getByText(firstIncident.flowNodeId)).toBeInTheDocument();
    expect(withinRow.getByText(firstIncident.jobId!)).toBeInTheDocument();
    expect(
      withinRow.getByText(formatDate(firstIncident.creationTime) || '--'),
    ).toBeInTheDocument();
    expect(withinRow.getByText(firstIncident.errorMessage)).toBeInTheDocument();

    expect(
      withinRow.getByRole('link', {
        description: /view root cause instance/i,
      }),
    ).toBeInTheDocument();
    expect(
      withinRow.queryByRole('button', {name: 'Retry Incident'}),
    ).not.toBeInTheDocument();

    withinRow = within(
      screen.getByRole('row', {
        name: new RegExp(secondIncident.errorType.name),
      }),
    );
    expect(
      withinRow.getByText(secondIncident.errorType.name),
    ).toBeInTheDocument();
    expect(withinRow.getByText(secondIncident.flowNodeId)).toBeInTheDocument();
    expect(withinRow.getByText(secondIncident.jobId!)).toBeInTheDocument();
    expect(
      withinRow.getByText(formatDate(secondIncident.creationTime) || '--'),
    ).toBeInTheDocument();
    expect(
      withinRow.getByText(secondIncident.errorMessage),
    ).toBeInTheDocument();
    expect(
      withinRow.queryByRole('button', {name: 'Retry Incident'}),
    ).not.toBeInTheDocument();
    expect(
      withinRow.queryByRole('link', {
        description: /view root cause instance/i,
      }),
    ).not.toBeInTheDocument();
  });

  it('should display -- for jobId', () => {
    mockFetchProcessDefinitionXml().withSuccess('');
    const incidentMock = {...firstIncident, jobId: null};
    const incidents = [incidentMock];

    incidentsStore.setIncidents({...incidentsMock, incidents, count: 1});

    render(<IncidentsTable />, {wrapper: Wrapper});

    let withinFirstRow = within(
      screen.getByRole('row', {
        name: new RegExp(incidentMock.errorType.name),
      }),
    );

    expect(withinFirstRow.getByText('--')).toBeInTheDocument();
  });

  it('should show a more button for long error messages', () => {
    mockFetchProcessDefinitionXml().withSuccess('');
    incidentsStore.setIncidents(incidentsMock);
    render(<IncidentsTable />, {wrapper: Wrapper});
    let withinFirstRow = within(
      screen.getByRole('row', {
        name: new RegExp(firstIncident.errorType.name),
      }),
    );

    expect(withinFirstRow.queryByText('More')).not.toBeInTheDocument();

    let withinSecondRow = within(
      screen.getByRole('row', {
        name: new RegExp(secondIncident.errorType.name),
      }),
    );

    expect(withinSecondRow.getByText('More')).toBeInTheDocument();
  });

  it('should open an modal when clicking on the more button', async () => {
    incidentsStore.setIncidents(incidentsMock);
    mockFetchProcessDefinitionXml().withSuccess('');
    const {user} = render(<IncidentsTable />, {wrapper: Wrapper});

    let withinSecondRow = within(
      screen.getByRole('row', {
        name: new RegExp(secondIncident.errorType.name),
      }),
    );

    expect(withinSecondRow.getByText('More')).toBeInTheDocument();

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

    await user.click(withinSecondRow.getByText('More'));

    const modal = screen.getByRole('dialog');

    expect(
      await within(modal).findByTestId('monaco-editor'),
    ).toBeInTheDocument();
    expect(
      within(modal).getByText(`Flow Node "${secondIncident.flowNodeId}" Error`),
    ).toBeInTheDocument();
  });
});
