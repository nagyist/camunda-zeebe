/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.historydeletion;

import static io.camunda.it.util.TestHelper.deployDmnModel;
import static io.camunda.it.util.TestHelper.evaluateDecision;
import static io.camunda.it.util.TestHelper.waitForBatchOperationCompleted;
import static io.camunda.it.util.TestHelper.waitForBatchOperationWithCorrectTotalCount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ProblemException;
import io.camunda.client.api.response.Decision;
import io.camunda.client.api.response.DeleteDecisionInstanceResponse;
import io.camunda.client.api.search.filter.DecisionInstanceFilter;
import io.camunda.configuration.HistoryDeletion;
import io.camunda.qa.util.multidb.MultiDbTest;
import io.camunda.qa.util.multidb.MultiDbTestApplication;
import io.camunda.zeebe.qa.util.cluster.TestStandaloneBroker;
import io.camunda.zeebe.test.util.Strings;
import java.time.Duration;
import java.util.function.Consumer;
import org.awaitility.Awaitility;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.DecisionTable;
import org.camunda.bpm.model.dmn.instance.Definitions;
import org.camunda.bpm.model.dmn.instance.Input;
import org.camunda.bpm.model.dmn.instance.InputExpression;
import org.camunda.bpm.model.dmn.instance.Output;
import org.camunda.bpm.model.dmn.instance.Text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

@MultiDbTest
@DisabledIfSystemProperty(named = "test.integration.camunda.database.type", matches = "AWS_OS")
public class DeleteDecisionInstanceHistoryIT {

  @MultiDbTestApplication
  static final TestStandaloneBroker BROKER =
      new TestStandaloneBroker()
          .withProcessingConfig(
              config ->
                  config
                      .getEngine()
                      .getBatchOperations()
                      .setSchedulerInterval(Duration.ofMillis(100)))
          .withDataConfig(
              config -> {
                final var historyDeletionConfig = new HistoryDeletion();
                historyDeletionConfig.setDelayBetweenRuns(Duration.ofMillis(100));
                historyDeletionConfig.setMaxDelayBetweenRuns(Duration.ofMillis(100));
                config.setHistoryDeletion(historyDeletionConfig);
              });

  private static final Duration DELETION_TIMEOUT = Duration.ofSeconds(30);
  private static CamundaClient camundaClient;

  @Test
  void shouldDeleteTwoDecisionInstancesByIdUsingBatchOperation() {
    // given
    final var dmnModel = getDmnModelInstance(Strings.newRandomValidBpmnId());
    final Decision decision =
        deployDmnModel(camundaClient, dmnModel, dmnModel.getModel().getModelName());
    evaluateDecision(camundaClient, decision.getDecisionKey(), "{}");
    evaluateDecision(camundaClient, decision.getDecisionKey(), "{}");
    waitForDecisionInstanceCount(f -> f.decisionDefinitionId(decision.getDmnDecisionId()), 2);

    // when
    final var batchResult =
        camundaClient
            .newCreateBatchOperationCommand()
            .deleteDecisionInstance()
            .filter(f -> f.decisionDefinitionId(decision.getDmnDecisionId()))
            .send()
            .join();

    // then
    assertThat(batchResult).isNotNull();
    assertThat(batchResult.getBatchOperationKey()).isNotNull();
    waitForBatchOperationWithCorrectTotalCount(
        camundaClient, batchResult.getBatchOperationKey(), 2);
    waitForBatchOperationCompleted(camundaClient, batchResult.getBatchOperationKey(), 2, 0);

    waitForDecisionInstanceCount(f -> f.decisionDefinitionId(decision.getDmnDecisionId()), 0);
  }

  @Test
  void shouldDeleteOneOfTwoDecisionInstancesByKeyUsingBatchOperation() {
    // given
    final var dmnModel = getDmnModelInstance(Strings.newRandomValidBpmnId());
    final Decision decision =
        deployDmnModel(camundaClient, dmnModel, dmnModel.getModel().getModelName());
    final long instanceToDeleteKey =
        evaluateDecision(camundaClient, decision.getDecisionKey(), "{}").getDecisionEvaluationKey();
    final long instanceToKeepKey =
        evaluateDecision(camundaClient, decision.getDecisionKey(), "{}").getDecisionEvaluationKey();
    waitForDecisionInstanceCount(f -> f.decisionDefinitionId(decision.getDmnDecisionId()), 2);

    // when
    final var batchResult =
        camundaClient
            .newCreateBatchOperationCommand()
            .deleteDecisionInstance()
            .filter(f -> f.decisionInstanceKey(instanceToDeleteKey))
            .send()
            .join();

    // then
    assertThat(batchResult).isNotNull();
    assertThat(batchResult.getBatchOperationKey()).isNotNull();
    waitForBatchOperationWithCorrectTotalCount(
        camundaClient, batchResult.getBatchOperationKey(), 1);
    waitForBatchOperationCompleted(camundaClient, batchResult.getBatchOperationKey(), 1, 0);

    waitForDecisionInstanceCount(f -> f.decisionInstanceKey(instanceToDeleteKey), 0);
    waitForDecisionInstanceCount(f -> f.decisionInstanceKey(instanceToKeepKey), 1);
  }

  @Test
  void shouldDeleteDecisionInstanceByKeyUsingSingleOperation() {
    // given
    final var dmnModel = getDmnModelInstance(Strings.newRandomValidBpmnId());
    final Decision decision =
        deployDmnModel(camundaClient, dmnModel, dmnModel.getModel().getModelName());
    final long instanceToDeleteKey =
        evaluateDecision(camundaClient, decision.getDecisionKey(), "{}").getDecisionEvaluationKey();
    waitForDecisionInstanceCount(f -> f.decisionDefinitionId(decision.getDmnDecisionId()), 1);

    // when
    final DeleteDecisionInstanceResponse response =
        camundaClient.newDeleteDecisionInstanceCommand(instanceToDeleteKey).send().join();

    // then
    assertThat(response).isNotNull();
    waitForDecisionInstanceCount(f -> f.decisionDefinitionId(decision.getDmnDecisionId()), 0);
  }

  @Test
  void shouldDeleteOneOfTwoDecisionInstancesByKeyUsingSingleOperation() {
    // given
    final var dmnModel = getDmnModelInstance(Strings.newRandomValidBpmnId());
    final Decision decision =
        deployDmnModel(camundaClient, dmnModel, dmnModel.getModel().getModelName());
    final long instanceToDeleteKey =
        evaluateDecision(camundaClient, decision.getDecisionKey(), "{}").getDecisionEvaluationKey();
    final long instanceToKeepKey =
        evaluateDecision(camundaClient, decision.getDecisionKey(), "{}").getDecisionEvaluationKey();
    waitForDecisionInstanceCount(f -> f.decisionDefinitionId(decision.getDmnDecisionId()), 2);

    // when
    final DeleteDecisionInstanceResponse response =
        camundaClient.newDeleteDecisionInstanceCommand(instanceToDeleteKey).send().join();

    // then
    assertThat(response).isNotNull();
    waitForDecisionInstanceCount(f -> f.decisionInstanceKey(instanceToDeleteKey), 0);
    waitForDecisionInstanceCount(f -> f.decisionInstanceKey(instanceToKeepKey), 1);
  }

  @Test
  void shouldReturnNotFoundForNonExistingDecisionInstanceWithSingularDeletion() {
    // given - non-existing decision instance key
    final long nonExistingKey = 99999L;

    // when/then - try to delete non-existing decision instance should throw not found exception
    assertThatExceptionOfType(ProblemException.class)
        .isThrownBy(
            () -> camundaClient.newDeleteDecisionInstanceCommand(nonExistingKey).send().join())
        .satisfies(
            exception -> {
              assertThat(exception.code()).isEqualTo(404);
              assertThat(exception.details().getDetail())
                  .containsIgnoringCase("decision instance")
                  .containsIgnoringCase("not found");
            });
  }

  private void waitForDecisionInstanceCount(
      final Consumer<DecisionInstanceFilter> filter, final int expectedResultCount) {
    Awaitility.await("Expected amount of decision instances in secondary storage")
        .atMost(DELETION_TIMEOUT)
        .ignoreExceptions()
        .untilAsserted(
            () -> {
              assertThat(
                      camundaClient
                          .newDecisionInstanceSearchRequest()
                          .filter(filter)
                          .send()
                          .join()
                          .items())
                  .hasSize(expectedResultCount);
            });
  }

  private DmnModelInstance getDmnModelInstance(final String decisionId) {
    // Create an empty DMN model
    final DmnModelInstance modelInstance = Dmn.createEmptyModel();

    // Create and configure the definitions element
    final Definitions definitions = modelInstance.newInstance(Definitions.class);
    definitions.setName("DRD");
    definitions.setNamespace("http://camunda.org/schema/1.0/dmn");
    modelInstance.setDefinitions(definitions);

    // Create the decision element
    final org.camunda.bpm.model.dmn.instance.Decision decision =
        modelInstance.newInstance(org.camunda.bpm.model.dmn.instance.Decision.class);
    decision.setId(decisionId);
    decision.setName("Decision 1");
    definitions.addChildElement(decision);

    // Create the decision table
    final DecisionTable decisionTable = modelInstance.newInstance(DecisionTable.class);
    decision.addChildElement(decisionTable);

    // Add input clauses
    final Input inputExperience = modelInstance.newInstance(Input.class);
    final InputExpression inputExpressionExperience =
        modelInstance.newInstance(InputExpression.class);
    final Text textExperience = modelInstance.newInstance(Text.class);
    textExperience.setTextContent("experience");
    inputExpressionExperience.setText(textExperience);
    inputExperience.setInputExpression(inputExpressionExperience);
    decisionTable.addChildElement(inputExperience);

    final Input inputType = modelInstance.newInstance(Input.class);
    final InputExpression inputExpressionType = modelInstance.newInstance(InputExpression.class);
    final Text textElementType = modelInstance.newInstance(Text.class);
    textElementType.setTextContent("type");
    inputExpressionType.setText(textElementType);
    inputType.setInputExpression(inputExpressionType);
    decisionTable.addChildElement(inputType);

    // Add output clauses
    final Output outputCode = modelInstance.newInstance(Output.class);
    outputCode.setName("code");
    decisionTable.addChildElement(outputCode);

    final Output outputDescription = modelInstance.newInstance(Output.class);
    outputDescription.setName("description");
    decisionTable.addChildElement(outputDescription);
    return modelInstance;
  }
}
