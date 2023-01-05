/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.templates.keycloak;

import org.keycloak.v1alpha1.Keycloak;
import org.keycloak.v1alpha1.KeycloakBuilder;

import java.util.Collections;

public class KeycloakTemplates {
    public Keycloak defaultKeycloak(String namespaceName) {
        return new KeycloakBuilder()
            .withNewMetadata()
                .withName("example-keycloak")
                .withLabels(Collections.singletonMap("app", "sso"))
                .withNamespace(namespaceName)
            .endMetadata()
            .withNewSpec()
                .withInstances(1L)
                .withExtensions("https://github.com/aerogear/keycloak-metrics-spi/releases/download/1.0.4/keycloak-metrics-spi-1.0.4.jar")
                .withNewExternalAccess()
                    .withEnabled()
                .endExternalAccess()
                .withNewPodDisruptionBudget()
                    .withEnabled()
                .endPodDisruptionBudget()
            .endSpec()
            .build();
    }
}
