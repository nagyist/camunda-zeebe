/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.authentication.service;

import io.camunda.authentication.ConditionalOnAuthenticationMethod;
import io.camunda.authentication.entity.AuthenticationContext;
import io.camunda.authentication.entity.CamundaOAuthPrincipal;
import io.camunda.authentication.entity.CamundaOidcUser;
import io.camunda.authentication.entity.CamundaUserDTO;
import io.camunda.search.entities.RoleEntity;
import io.camunda.security.entity.AuthenticationMethod;
import io.camunda.security.entity.ClusterMetadata.AppName;
import jakarta.json.Json;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnAuthenticationMethod(AuthenticationMethod.OIDC)
@Profile("consolidated-auth")
public class OidcCamundaUserService implements CamundaUserService {
  private static final String SALES_PLAN_TYPE = "";

  // TODO: This needs to be set for SaaS purposes
  private static final Map<AppName, String> C8_LINKS = Map.of();

  private Optional<CamundaOAuthPrincipal> getCamundaUser() {
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .map(Authentication::getPrincipal)
        .map(principal -> principal instanceof final CamundaOAuthPrincipal user ? user : null);
  }

  @Override
  public CamundaUserDTO getCurrentUser() {
    return getCamundaUser()
        .map(
            user -> {
              final AuthenticationContext auth = user.getAuthenticationContext();
              return new CamundaUserDTO(
                  auth.username(),
                  null,
                  user.getDisplayName(),
                  auth.username(),
                  user.getEmail(),
                  auth.authorizedApplications(),
                  auth.tenants(),
                  auth.groups(),
                  auth.roles().stream().map(RoleEntity::name).toList(),
                  SALES_PLAN_TYPE,
                  C8_LINKS,
                  true);
            })
        .orElse(null);
  }

  @Override
  public String getUserToken() {
    return getCamundaUser()
        .map(
            user -> {
              if (user instanceof final CamundaOidcUser camundaOAuthPrincipal) {
                // If the user has an access token, return it; otherwise, return the ID token to
                // match the fallback behavior of CamundaOidcUserService#loadUser.
                final var token =
                    camundaOAuthPrincipal.getAccessToken() != null
                        ? camundaOAuthPrincipal.getAccessToken()
                        : camundaOAuthPrincipal.getIdToken().getTokenValue();
                return Json.createValue(token).toString();
              }

              throw new UnsupportedOperationException(
                  "Not supported for token class: " + user.getClass().getName());
            })
        .orElseThrow(
            () ->
                new UnsupportedOperationException(
                    "User is not authenticated or does not have a valid token"));
  }
}
