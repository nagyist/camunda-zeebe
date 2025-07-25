/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.tasklist.util.apps.modules;

import io.camunda.tasklist.TasklistModuleConfiguration;
import io.camunda.tasklist.data.DataGenerator;
import io.camunda.tasklist.data.es.DevDataGeneratorElasticSearch;
import io.camunda.tasklist.data.os.DevDataGeneratorOpenSearch;
import io.camunda.tasklist.util.TasklistZeebeIntegrationTest;
import io.camunda.tasklist.util.TestApplication;
import io.camunda.tasklist.util.TestTasklistPropertiesOverride;
import io.camunda.tasklist.webapp.management.WebappManagementModuleConfiguration;
import io.camunda.tasklist.zeebeimport.security.ImporterSecurityModuleConfiguration;
import io.camunda.webapps.WebappsModuleConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
@ComponentScan(
    basePackages = {"io.camunda.tasklist", "io.camunda.application.commons"},
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = "io\\.camunda\\.tasklist\\.util\\.apps\\..*"),
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = "io\\.camunda\\.tasklist\\.zeebeimport\\..*"),
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = "io\\.camunda\\.tasklist\\.webapp\\..*"),
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = "io\\.camunda\\.tasklist\\.archiver\\..*"),
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = "io\\.camunda\\.tasklist\\.data\\..*"),
      @ComponentScan.Filter(type = FilterType.REGEX, pattern = "io\\.camunda\\.tasklist\\.it\\..*"),
      @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = TestApplication.class),
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          value = TasklistModuleConfiguration.class)
    },
    nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@Import({
  TestTasklistPropertiesOverride.class,
  WebappsModuleConfiguration.class,
  ImporterSecurityModuleConfiguration.class,
  WebappManagementModuleConfiguration.class,
})
public class ModulesTestApplication {

  public static void main(final String[] args) throws Exception {
    SpringApplication.run(ModulesTestApplication.class, args);
  }

  @Bean(name = "dataGenerator")
  @Profile("dev-data")
  @ConditionalOnMissingBean
  public DataGenerator stubDataGenerator() {
    return TasklistZeebeIntegrationTest.IS_ELASTIC
        ? new DevDataGeneratorElasticSearch()
        : new DevDataGeneratorOpenSearch();
  }
}
