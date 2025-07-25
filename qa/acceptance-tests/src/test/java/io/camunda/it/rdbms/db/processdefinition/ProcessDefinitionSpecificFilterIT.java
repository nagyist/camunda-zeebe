/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.rdbms.db.processdefinition;

import static io.camunda.it.rdbms.db.fixtures.ProcessDefinitionFixtures.createAndSaveProcessDefinition;
import static io.camunda.it.rdbms.db.fixtures.ProcessDefinitionFixtures.createAndSaveRandomProcessDefinitions;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.application.commons.rdbms.RdbmsConfiguration;
import io.camunda.db.rdbms.RdbmsService;
import io.camunda.db.rdbms.read.service.ProcessDefinitionDbReader;
import io.camunda.db.rdbms.write.RdbmsWriter;
import io.camunda.it.rdbms.db.fixtures.ProcessDefinitionFixtures;
import io.camunda.it.rdbms.db.util.RdbmsTestConfiguration;
import io.camunda.search.filter.ProcessDefinitionFilter;
import io.camunda.search.page.SearchQueryPage;
import io.camunda.search.query.ProcessDefinitionQuery;
import io.camunda.search.sort.ProcessDefinitionSort;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@Tag("rdbms")
@DataJdbcTest
@ContextConfiguration(classes = {RdbmsTestConfiguration.class, RdbmsConfiguration.class})
@AutoConfigurationPackage
@TestPropertySource(properties = {"spring.liquibase.enabled=false", "camunda.database.type=rdbms"})
public class ProcessDefinitionSpecificFilterIT {

  @Autowired private RdbmsService rdbmsService;

  @Autowired private ProcessDefinitionDbReader processDefinitionReader;

  private RdbmsWriter rdbmsWriter;

  @BeforeEach
  public void beforeAll() {
    rdbmsWriter = rdbmsService.createWriter(0L);
  }

  @ParameterizedTest
  @MethodSource("shouldFindProcessDefinitionWithSpecificFilterParameters")
  public void shouldFindProcessDefinitionWithSpecificFilter(final ProcessDefinitionFilter filter) {
    createAndSaveRandomProcessDefinitions(rdbmsWriter);
    createAndSaveProcessDefinition(
        rdbmsWriter,
        ProcessDefinitionFixtures.createRandomized(
            b ->
                b.processDefinitionKey(1337L)
                    .processDefinitionId("sorting-test-process")
                    .name("Sorting Test Process")
                    .resourceName("sorting-test-process.bpmn")
                    .versionTag("Version 1337")
                    .version(1337)
                    .tenantId("sorting-tenant1")));

    final var searchResult =
        processDefinitionReader.search(
            new ProcessDefinitionQuery(
                filter,
                ProcessDefinitionSort.of(b -> b),
                SearchQueryPage.of(b -> b.from(0).size(5))));

    assertThat(searchResult.total()).isEqualTo(1);
    assertThat(searchResult.items()).hasSize(1);
    assertThat(searchResult.items().getFirst().processDefinitionKey()).isEqualTo(1337L);
  }

  static List<ProcessDefinitionFilter> shouldFindProcessDefinitionWithSpecificFilterParameters() {
    return List.of(
        new ProcessDefinitionFilter.Builder().processDefinitionKeys(1337L).build(),
        new ProcessDefinitionFilter.Builder().processDefinitionIds("sorting-test-process").build(),
        new ProcessDefinitionFilter.Builder().names("Sorting Test Process").build(),
        new ProcessDefinitionFilter.Builder().resourceNames("sorting-test-process.bpmn").build(),
        new ProcessDefinitionFilter.Builder().versions(1337).build(),
        new ProcessDefinitionFilter.Builder().versionTags("Version 1337").build(),
        new ProcessDefinitionFilter.Builder().tenantIds("sorting-tenant1").build());
  }
}
