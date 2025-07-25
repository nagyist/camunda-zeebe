/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.configuration;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

@ComponentScan("io.camunda.configuration")
public class UnifiedConfigurationHelper {

  private static final Map<String, Set<String>> LEGACY_PROPERTIES =
      Map.of(
          "camunda.data.secondary-storage.elasticsearch.url",
          Set.of(
              "camunda.database.url",
              "camunda.operate.elasticsearch.url",
              "camunda.operate.zeebeElasticsearch.url",
              "camunda.tasklist.elasticsearch.url",
              "camunda.tasklist.zeebeElasticsearch.url"),
          "camunda.data.secondary-storage.type",
          Set.of("camunda.database.type"));

  private static final Logger LOGGER = LoggerFactory.getLogger(UnifiedConfigurationHelper.class);

  private static Environment environment;
  private static Map<String, Set<String>> legacyPropertiesDict = LEGACY_PROPERTIES;

  public UnifiedConfigurationHelper(@Autowired Environment environment) {
    // We need to pin the environment object statically so that it can be used to perform the
    // fallback mechanism.
    UnifiedConfigurationHelper.environment = environment;
  }

  public enum BackwardsCompatibilityMode {
    NOT_SUPPORTED,
    SUPPORTED_ONLY_IF_VALUES_MATCH,
    SUPPORTED
  }

  public static <T> T validateLegacyConfiguration(
      final String newProperty,
      final T newValue,
      final Class<T> expectedType,
      final BackwardsCompatibilityMode backwardsCompatibilityMode) {
    // Input validation
    if (backwardsCompatibilityMode == null) {
      throw new UnifiedConfigurationException("backwardsCompatibilityMode cannot be null");
    }

    // Tools
    final Set<String> legacyProperties = legacyPropertiesDict.get(newProperty);

    return switch (backwardsCompatibilityMode) {
      case NOT_SUPPORTED -> {
        yield backwardsCompatibilityNotSupported(legacyProperties, newProperty, newValue);
      }
      case SUPPORTED_ONLY_IF_VALUES_MATCH -> {
        final T legacyValue = getLegacyValue(legacyProperties, expectedType);
        yield backwardsCompatibilitySupportedOnlyIfValuesMatch(
            legacyProperties, legacyValue, newProperty, newValue);
      }
      case SUPPORTED -> {
        final T legacyValue = getLegacyValue(legacyProperties, expectedType);
        yield backwardsCompatibilitySupported(legacyProperties, legacyValue, newProperty, newValue);
      }
    };
  }

  private static <T> T getLegacyValue(
      final Set<String> legacyProperties, final Class<T> expectedType) {
    final Set<T> legacyValues = new HashSet<>();

    for (final String legacyProperty : legacyProperties) {
      final String strValue = environment.getProperty(legacyProperty);
      final T legacyValue = parseLegacyValue(strValue, expectedType);
      legacyValues.add(legacyValue);
    }

    if (legacyValues.size() > 1) {
      throw new UnifiedConfigurationException(
          String.format(
              "Ambiguous legacy configuration. Legacy properties: %s; Legacy values: %s",
              String.join(", ", legacyProperties), legacyValues.toString()));
    }

    return legacyValues.iterator().next();
  }

  private static <T> T backwardsCompatibilityNotSupported(
      final Set<String> legacyProperties, final String newProperty, final T newValue) {
    final boolean legacyPresent = legacyConfigPresent(legacyProperties);
    final boolean newPresent = newConfigPresent(newProperty);

    final String warningMessage =
        String.format(
            "The following legacy properties are no longer supported and should be removed in favor of '%s': %s",
            newProperty, String.join(", ", legacyProperties));

    if (!legacyPresent) {
      // Legacy config: not present
      // We can return the newValue or default value
      return newValue;
    } else if (legacyPresent && !newPresent) {
      // Legacy config: present
      // New config...: not present
      // The legacy configuration is no longer allowed -> error
      final String errorMessage =
          String.format(
              "The following legacy configuration properties are no longer supported and must be removed in favor of '%s': %s",
              newProperty, String.join(", ", legacyProperties));
      throw new UnifiedConfigurationException(errorMessage);
    } else {
      // Legacy config: present
      // New config...: present
      // We can return the new value and log a warning
      LOGGER.warn(warningMessage);
      return newValue;
    }
  }

  private static <T> T backwardsCompatibilitySupportedOnlyIfValuesMatch(
      final Set<String> legacyProperties,
      final T legacyValue,
      final String newProperty,
      final T newValue) {
    final boolean legacyPresent = legacyConfigPresent(legacyProperties);
    final boolean newPresent = newConfigPresent(newProperty);

    final String warningMessage =
        String.format(
            "The following legacy properties are no longer supported and should be removed in favor of '%s': %s",
            newProperty, String.join(", ", legacyProperties));

    if ((!legacyPresent && !newPresent) || (!legacyPresent && newPresent)) {
      // Legacy config: not present
      // New config...: not present
      // or
      // Legacy config: not present
      // New config...: present
      // then
      // We can return the new value, either default or declared
      return newValue;
    } else {
      // Legacy config: present
      // New config...: not present
      // or
      // Legacy config: present
      // New config...: present
      // then
      // We can return only if the new default value and the legacy value match
      if (Objects.equals(legacyValue, newValue)) {
        LOGGER.warn(warningMessage);
        return newValue;
      }

      final String errorMessage =
          String.format(
              "Ambiguous configuration. The value %s=%s does not match the value(s) conflicts with the values '%s' from the legacy properties %s",
              newProperty, newValue, legacyValue, String.join(", ", legacyProperties));
      throw new UnifiedConfigurationException(errorMessage);
    }
  }

  private static <T> T backwardsCompatibilitySupported(
      final Set<String> legacyProperties,
      final T legacyValue,
      final String newProperty,
      final T newValue) {
    final boolean legacyPresent = legacyConfigPresent(legacyProperties);
    final boolean newPresent = newConfigPresent(newProperty);

    final String warningMessage =
        String.format(
            "The following legacy configuration properties should be removed in favor of '%s': %s",
            newProperty, String.join(", ", legacyProperties));

    if ((!legacyPresent && !newPresent) || (!legacyPresent && newPresent)) {
      // Legacy config: not present
      // New config...: not present
      // We can retrun the new default value
      return newValue;
    } else if (legacyPresent && !newPresent) {
      // Legacy config: present
      // New config...: not present
      // We can return the legacy value
      LOGGER.warn(warningMessage);
      return legacyValue;
    } else {
      // Legacy config: present
      // New config...: present
      // The new value wins
      LOGGER.warn(warningMessage);
      return newValue;
    }
  }

  private static boolean legacyConfigPresent(Set<String> legacyProperties) {
    for (final String legacyProperty : legacyProperties) {
      if (environment.containsProperty(legacyProperty)) {
        return true;
      }
    }

    return false;
  }

  private static boolean newConfigPresent(String newProperty) {
    return environment.containsProperty(newProperty);
  }

  @SuppressWarnings("unchecked")
  private static <T> T parseLegacyValue(final String strValue, final Class<T> type) {
    if (strValue == null) {
      return null;
    }

    return switch (type.getSimpleName()) {
      case "String" -> (T) strValue;
      case "Integer" -> (T) Integer.valueOf(strValue);
      case "Boolean" -> (T) Boolean.valueOf(strValue);
      case "Duration" -> (T) Duration.parse(strValue);
      default -> throw new IllegalArgumentException("Unsupported type: " + type);
    };
  }

  /* Setters used by tests to inject the mock objects */

  public static void setCustomLegacyProperties(
      final Map<String, Set<String>> legacyPropertiesDict) {
    UnifiedConfigurationHelper.legacyPropertiesDict = legacyPropertiesDict;
  }

  public static void setCustomEnvironment(final Environment environment) {
    UnifiedConfigurationHelper.environment = environment;
  }
}
