/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.application.commons.backup;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.tasklist.data.conditionals.ElasticSearchCondition;
import io.camunda.webapps.backup.BackupRepository;
import io.camunda.webapps.backup.repository.BackupRepositoryProps;
import io.camunda.webapps.backup.repository.WebappsSnapshotNameProvider;
import io.camunda.webapps.backup.repository.elasticsearch.ElasticsearchBackupRepository;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * A backup repository to be used when tasklist is not run together with Operate, because of the
 * different qualifiers for the elasticSearch clients between tasklist & operate When the ES client
 * is shared between operate & tasklist then this class can be removed after adding tasklist to the
 * valid profiles for {@link ElasticsearchBackupRepository}
 *
 * <p>Note that the condition used refers to tasklist ElasticSearchCondition
 */
@Conditional(ElasticSearchCondition.class)
@Configuration
@Profile("tasklist & !operate")
// only active if standalone, otherwise the operate one is used
public class ElasticsearchTasklistBackupRepository {

  private final RestHighLevelClient esClient;
  private final ObjectMapper objectMapper;
  private final BackupRepositoryProps backupRepositoryProps;

  public ElasticsearchTasklistBackupRepository(
      @Qualifier("tasklistEsClient") final RestHighLevelClient esClient,
      final ObjectMapper objectMapper,
      final BackupRepositoryProps backupRepositoryProps) {
    this.esClient = esClient;
    this.objectMapper = objectMapper;
    this.backupRepositoryProps = backupRepositoryProps;
  }

  @Bean
  public BackupRepository backupRepository() {
    return new ElasticsearchBackupRepository(
        esClient, objectMapper, backupRepositoryProps, new WebappsSnapshotNameProvider());
  }
}