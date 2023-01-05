/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.keycloak;

import org.keycloak.v1alpha1.keycloakrealmspec.realm.Users;
import org.keycloak.v1alpha1.keycloakrealmspec.realm.UsersBuilder;
import org.keycloak.v1alpha1.keycloakrealmspec.realm.users.CredentialsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class KeycloakRealmUsers {

    public static final Users ALICE_USER = new UsersBuilder()
            .withUsername("alice")
            .withEnabled()
            .withEmail("alice@example.com")
            .withCredentials(
                    new CredentialsBuilder()
                            .withType("password")
                            .withValue("alice-password")
                            .build()
            )
            .withRealmRoles("user")
            .withClientRoles(Collections.singletonMap("kafka", Collections.singletonList("kafka-topic:superapp_*:owner")))
            .build();

    public static final Users ADMIN_USER = new UsersBuilder()
            .withUsername("admin")
            .withEnabled()
            .withEmail("admin@example.com")
            .withCredentials(
                    new CredentialsBuilder()
                            .withType("password")
                            .withValue("admin-password")
                            .build()
            )
            .withRealmRoles("admin")
            .withClientRoles(Map.of(
                    "realm-management", Collections.singletonList("realm-admin"),
                    "kafka", Collections.singletonList("kafka-admin")
            ))
            .build();

    public static final Users SA_KAFKA_BROKER = new UsersBuilder()
            .withUsername("service-account-kafka-broker")
            .withEnabled()
            .withEmail("service-account-kafka-broker@placeholder.org")
            .withId("kafka-broker")
            .withClientRoles(Map.of(
                    "kafka", Collections.singletonList("kafka-admin")
            ))
            .build();

    public static final Users SA_KAFKA_CLIENT = new UsersBuilder()
            .withUsername("service-account-kafka-client")
            .withEnabled()
            .withId("kafka-client")
            .withRealmRoles("default-roles-audience")
            .build();

    public static final Users SA_KAFKA_PRODUCER = new UsersBuilder()
            .withUsername("service-account-kafka-producer")
            .withEnabled()
            .withEmail("service-account-kafka-producer@placeholder.org")
            .withId("kafka-producer")
            .build();

    public static final Users SA_KAFKA_CONSUMER = new UsersBuilder()
            .withUsername("service-account-kafka-consumer")
            .withEnabled()
            .withEmail("service-account-kafka-consumer@placeholder.org")
            .withId("kafka-consumer")
            .build();

    public static final Users SA_KAFKA_CONNECT = new UsersBuilder()
            .withUsername("service-account-kafka-connect")
            .withEnabled()
            .withEmail("service-account-kafka-connect@placeholder.org")
            .withId("kafka-connect")
            .build();

    public static final Users SA_KAFKA_BRIDGE = new UsersBuilder()
            .withUsername("service-account-kafka-bridge")
            .withEnabled()
            .withEmail("service-account-kafka-bridge@placeholder.org")
            .withId("kafka-bridge")
            .build();

    public static final Users SA_KAFKA_MM = new UsersBuilder()
            .withUsername("service-account-kafka-mirror-maker")
            .withEnabled()
            .withEmail("service-account-kafka-mirror-maker@placeholder.org")
            .withId("kafka-mirror-maker")
            .build();

    public static final Users SA_KAFKA_MM2 = new UsersBuilder()
            .withUsername("service-account-kafka-mirror-maker-2")
            .withEnabled()
            .withEmail("service-account-kafka-mirror-maker-2@placeholder.org")
            .withId("kafka-mirror-maker-2")
            .build();

    public static final Users SA_HW_PRODUCER = new UsersBuilder()
            .withUsername("service-account-hello-world-producer")
            .withEnabled()
            .withEmail("service-account-hello-world-producer@placeholder.org")
            .withId("hello-world-producer")
            .build();

    public static final Users SA_HW_CONSUMER = new UsersBuilder()
            .withUsername("service-account-hello-world-consumer")
            .withEnabled()
            .withEmail("service-account-hello-world-consumer@placeholder.org")
            .withId("hello-world-consumer")
            .build();


    public static final Users SA_HW_STREAMS = new UsersBuilder()
            .withUsername("service-account-hello-world-streams")
            .withEnabled()
            .withEmail("service-account-hello-world-streams@placeholder.org")
            .withId("hello-world-streams")
            .build();

    public static final Users SA_KAFKA_AUDIENCE_PRODUCER = new UsersBuilder()
            .withUsername("service-account-kafka-audience-producer")
            .withEnabled()
            .withEmail("service-account-kafka-audience-producer@placeholder.org")
            .withId("kafka-audience-producer")
            .build();

    public static final Users SA_KAFKA_AUDIENCE_CONSUMER = new UsersBuilder()
            .withUsername("service-account-kafka-audience-consumer")
            .withEnabled()
            .withEmail("service-account-kafka-audience-consumer@placeholder.org")
            .withId("kafka-audience-consumer")
            .build();

    public static List<Users> getAllInternalRealmUsers() {
        return List.of(
                ALICE_USER, ADMIN_USER, SA_KAFKA_BROKER, SA_KAFKA_PRODUCER, SA_KAFKA_CONSUMER, SA_KAFKA_CONNECT, SA_KAFKA_BRIDGE,
                SA_KAFKA_MM, SA_KAFKA_MM2, SA_HW_PRODUCER, SA_HW_CONSUMER, SA_HW_STREAMS, SA_KAFKA_AUDIENCE_PRODUCER, SA_KAFKA_AUDIENCE_CONSUMER
        );
    }
}
