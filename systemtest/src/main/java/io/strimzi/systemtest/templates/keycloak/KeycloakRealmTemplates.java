/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.templates.keycloak;

import io.strimzi.systemtest.keycloak.KeycloakRealmClients;
import io.strimzi.systemtest.keycloak.KeycloakRealmUsers;
import org.keycloak.v1alpha1.KeycloakRealm;
import org.keycloak.v1alpha1.KeycloakRealmBuilder;
import org.keycloak.v1alpha1.keycloakrealmspec.realm.ClientScopeMappingsBuilder;
import org.keycloak.v1alpha1.keycloakrealmspec.realm.ClientScopesBuilder;
import org.keycloak.v1alpha1.keycloakrealmspec.realm.RolesBuilder;
import org.keycloak.v1alpha1.keycloakrealmspec.realm.ScopeMappingsBuilder;
import org.keycloak.v1alpha1.keycloakrealmspec.realm.clientscopes.ProtocolMappersBuilder;
import org.keycloak.v1alpha1.keycloakrealmspec.realm.roles.ClientBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class KeycloakRealmTemplates {

    public KeycloakRealm internalRealm(String namespaceName) {
        return new KeycloakRealmBuilder()
            .withNewMetadata()
                .withName("internal")
                .withNamespace(namespaceName)
            .endMetadata()
            .withNewSpec()
                .withNewRealm()
                    .withRealm("internal")
                    .withAccessTokenLifespan(300)
                    .withEnabled()
                    .withSslRequired("external")
                    .withRoles(
                            new RolesBuilder()
                                .addNewRolesRealm()
                                    .withName("user")
                                .endRolesRealm()
                                .addNewRolesRealm()
                                    .withName("admin")
                                .endRolesRealm()
                                .addToClient("kafka", List.of(
                                    new ClientBuilder()
                                            .withName("kafka-admin")
                                            .withClientRole()
                                            .build(),
                                    new ClientBuilder()
                                            .withName("kafka-topic:superapp_*:owner")
                                            .withClientRole()
                                            .build(),
                                    new ClientBuilder()
                                            .withName("kafka-topic:superapp_*:consumer")
                                            .withClientRole()
                                            .build()
                                ))
                                .build()
                    )
                    .withScopeMappings(List.of(
                            new ScopeMappingsBuilder()
                                    .withClient("kafka-producer")
                                    .withRoles("offline_access")
                                    .build(),
                            new ScopeMappingsBuilder()
                                    .withClient("kafka-consumer")
                                    .withRoles("offline_access")
                                    .build(),
                            new ScopeMappingsBuilder()
                                    .withClientScope("offline_access")
                                    .withRoles("offline_access")
                                    .build()
                    ))
                    .withClientScopeMappings(Map.of("kafka", Collections.singletonList(
                            new ClientScopeMappingsBuilder()
                                    .withClient("kafka-broker")
                                    .withRoles("kafka-admin")
                                    .build())
                    ))
                    .withClients(KeycloakRealmClients.getAllClients())
                    .addAllToUsers(KeycloakRealmUsers.getAllInternalRealmUsers())
                .endRealm()
            .endSpec()
            .build();
    }

    public KeycloakRealm scopeAudienceRealm(String namespaceName) {
        return new KeycloakRealmBuilder()
                .withNewMetadata()
                    .withName("scope-test")
                    .withNamespace(namespaceName)
                .endMetadata()
                .withNewSpec()
                    .withNewRealm()
                        .withRealm("scope-test")
                        .withAccessTokenLifespan(2592000)
                        .withEnabled()
                        .withSslRequired("external")
                        .withRoles(
                                new RolesBuilder()
                                        .addNewRolesRealm()
                                            .withName("user")
                                        .endRolesRealm()
                                        .addNewRolesRealm()
                                        .withName("admin")
                                        .endRolesRealm()
                                        .addToClient(Map.of("kafka", Collections.emptyList(), "kafka-client", Collections.emptyList()))
                                        .build()
                        )
                        .withScopeMappings(List.of(
                                new ScopeMappingsBuilder()
                                        .withClient("kafka-broker")
                                        .withRoles("offline_access")
                                        .build(),
                                new ScopeMappingsBuilder()
                                        .withClient("kafka-client")
                                        .withRoles("offline_access")
                                        .build(),
                                new ScopeMappingsBuilder()
                                        .withClientScope("offline_access")
                                        .withRoles("offline_access")
                                        .build()
                        ))
                        .withClientScopeMappings(Map.of("kafka", Collections.singletonList(
                                new ClientScopeMappingsBuilder()
                                        .withClient("kafka-broker")
                                        .withRoles("kafka-admin")
                                        .build())
                        ))
                        .withClientScopes(List.of(
                                new ClientScopesBuilder()
                                        .withName("test")
                                        .withProtocol("openid-connect")
                                        .withAttributes(
                                                Map.of(
                                                        "include.in.token.scope", "true",
                                                        "display.on.consent.screen", "false"
                                                )
                                        )
                                        .build(),
                                new ClientScopesBuilder()
                                        .withName("offline_access")
                                        .withProtocol("openid-connect")
                                        .withAttributes(
                                                Map.of(
                                                        "consent.screen.text", "${offlineAccessScopeConsentText}",
                                                        "display.on.consent.screen", "true"
                                                )
                                        )
                                        .build(),
                                new ClientScopesBuilder()
                                        .withName("profile")
                                        .withProtocol("openid-connect")
                                        .withAttributes(
                                                Map.of(
                                                        "include.in.token.scope", "true",
                                                        "consent.screen.text", "${profileScopeConsentText}",
                                                        "display.on.consent.screen", "true"
                                                )
                                        )
                                        .withProtocolMappers(
                                                new ProtocolMappersBuilder()
                                                        .withName("username")
                                                        .withProtocol("openid-connect")
                                                        .withProtocolMapper("oidc-usermodel-property-mapper")
                                                        .withConsentRequired(false)
                                                        .withConfig(
                                                                Map.of(
                                                                        "userinfo.token.claim", "true",
                                                                        "user.attribute", "username",
                                                                        "id.token.claim", "true",
                                                                        "access.token.claim", "true",
                                                                        "claim.name", "preferred_username",
                                                                        "jsonType.label", "String"
                                                                )
                                                        )
                                                        .build()
                                        )
                                        .build()
                        ))
                        .withClients(KeycloakRealmClients.KAFKA, KeycloakRealmClients.SCOPE_KAFKA_BROKER, KeycloakRealmClients.KAFKA_CLIENT)
                        .addAllToUsers(List.of(KeycloakRealmUsers.ADMIN_USER, KeycloakRealmUsers.SA_KAFKA_BROKER, KeycloakRealmUsers.SA_KAFKA_CLIENT))
                    .endRealm()
                .endSpec()
                .build();
    }

    public KeycloakRealm authorizationRealm(String namespaceName) {
        return new KeycloakRealmBuilder()
                .withNewMetadata()
                .withName("kafka-authz")
                .withNamespace(namespaceName)
                .endMetadata()
                .withNewSpec()
                .withNewRealm()
                .withRealm("kafka-authz")
                .withAccessTokenLifespan(120)
                .withEnabled()
                .withSslRequired("external")
                .withRoles(
                        new RolesBuilder()
                                .addNewRolesRealm()
                                .withName("Dev Team A")
                                .endRolesRealm()
                                .addNewRolesRealm()
                                .withName("Dev Team B")
                                .endRolesRealm()
                                .addNewRolesRealm()
                                .withName("Ops Team")
                                .endRolesRealm()
                                .addToClient(
                                        Map.of(
                                                "team-a-client", Collections.emptyList(),
                                                "team-b-client", Collections.emptyList(),
                                                "kafka-cli", Collections.emptyList(),
                                                "kafka", Collections.singletonList(
                                                        new ClientBuilder()
                                                                .withName("uma_protection")
                                                                .withClientRole()
                                                                .build()
                                                )
                                        )
                                )
                                .build()
                )
                .withScopeMappings(List.of(
                        new ScopeMappingsBuilder()
                                .withClient("kafka-producer")
                                .withRoles("offline_access")
                                .build(),
                        new ScopeMappingsBuilder()
                                .withClient("kafka-consumer")
                                .withRoles("offline_access")
                                .build(),
                        new ScopeMappingsBuilder()
                                .withClientScope("offline_access")
                                .withRoles("offline_access")
                                .build()
                ))
                .withClientScopeMappings(Map.of("kafka", Collections.singletonList(
                        new ClientScopeMappingsBuilder()
                                .withClient("kafka-broker")
                                .withRoles("kafka-admin")
                                .build())
                ))
                .withClients(KeycloakRealmClients.getAllClients())
                .addAllToUsers(KeycloakRealmUsers.getAllInternalRealmUsers())
                .endRealm()
                .endSpec()
                .build();
    }
//  "groups" : [
//    {
//      "name" : "ClusterManager Group",
//      "path" : "/ClusterManager Group"
//    }, {
//      "name" : "ClusterManager-cluster2 Group",
//      "path" : "/ClusterManager-cluster2 Group"
//    }, {
//      "name" : "Ops Team Group",
//      "path" : "/Ops Team Group"
//    }
//  ],
//  "users": [
//    {
//      "username" : "alice",
//      "enabled" : true,
//      "totp" : false,
//      "emailVerified" : true,
//      "firstName" : "Alice",
//      "email" : "alice@strimzi.io",
//      "credentials" : [ {
//        "type" : "password",
//        "secretData" : "{\"value\":\"KqABIiReBuRWbP4pBct3W067pNvYzeN7ILBV+8vT8nuF5cgYs2fdl2QikJT/7bGTW/PBXg6CYLwJQFYrBK9MWg==\",\"salt\":\"EPgscX9CQz7UnuZDNZxtMw==\"}",
//        "credentialData" : "{\"hashIterations\":27500,\"algorithm\":\"pbkdf2-sha256\"}"
//      } ],
//      "disableableCredentialTypes" : [ ],
//      "requiredActions" : [ ],
//      "realmRoles" : [ "offline_access", "uma_authorization" ],
//      "clientRoles" : {
//        "account" : [ "view-profile", "manage-account" ]
//      },
//      "groups" : [ "/ClusterManager Group" ]
//    }, {
//      "username" : "bob",
//      "enabled" : true,
//      "totp" : false,
//      "emailVerified" : true,
//      "firstName" : "Bob",
//      "email" : "bob@strimzi.io",
//      "credentials" : [ {
//        "type" : "password",
//        "secretData" : "{\"value\":\"QhK0uLsKuBDrMm9Z9XHvq4EungecFRnktPgutfjKtgVv2OTPd8D390RXFvJ8KGvqIF8pdoNxHYQyvDNNwMORpg==\",\"salt\":\"yxkgwEyTnCGLn42Yr9GxBQ==\"}",
//        "credentialData" : "{\"hashIterations\":27500,\"algorithm\":\"pbkdf2-sha256\"}"
//      } ],
//      "disableableCredentialTypes" : [ ],
//      "requiredActions" : [ ],
//      "realmRoles" : [ "offline_access", "uma_authorization" ],
//      "clientRoles" : {
//        "account" : [ "view-profile", "manage-account" ]
//      },
//      "groups" : [ "/ClusterManager-cluster2 Group" ]
//    },
//    {
//      "username" : "service-account-team-a-client",
//      "enabled" : true,
//      "serviceAccountClientId" : "team-a-client",
//      "realmRoles" : [ "offline_access", "Dev Team A" ],
//      "clientRoles" : {
//        "account" : [ "manage-account", "view-profile" ]
//      },
//      "groups" : [ ]
//    },
//    {
//      "username" : "service-account-team-b-client",
//      "enabled" : true,
//      "serviceAccountClientId" : "team-b-client",
//      "realmRoles" : [ "offline_access", "Dev Team B" ],
//      "clientRoles" : {
//        "account" : [ "manage-account", "view-profile" ]
//      },
//      "groups" : [ ]
//    }
//  ],
//  "clients": [
//    {
//      "clientId": "team-a-client",
//      "enabled": true,
//      "clientAuthenticatorType": "client-secret",
//      "secret": "team-a-client-secret",
//      "bearerOnly": false,
//      "consentRequired": false,
//      "standardFlowEnabled": false,
//      "implicitFlowEnabled": false,
//      "directAccessGrantsEnabled": true,
//      "serviceAccountsEnabled": true,
//      "publicClient": false,
//      "fullScopeAllowed": true
//    },
//    {
//      "clientId": "team-b-client",
//      "enabled": true,
//      "clientAuthenticatorType": "client-secret",
//      "secret": "team-b-client-secret",
//      "bearerOnly": false,
//      "consentRequired": false,
//      "standardFlowEnabled": false,
//      "implicitFlowEnabled": false,
//      "directAccessGrantsEnabled": true,
//      "serviceAccountsEnabled": true,
//      "publicClient": false,
//      "fullScopeAllowed": true
//    },
//    {
//      "clientId": "kafka",
//      "enabled": true,
//      "clientAuthenticatorType": "client-secret",
//      "secret": "kafka-secret",
//      "bearerOnly": false,
//      "consentRequired": false,
//      "standardFlowEnabled": false,
//      "implicitFlowEnabled": false,
//      "directAccessGrantsEnabled": true,
//      "serviceAccountsEnabled": true,
//      "authorizationServicesEnabled": true,
//      "publicClient": false,
//      "fullScopeAllowed": true,
//      "authorizationSettings": {
//        "allowRemoteResourceManagement": true,
//        "policyEnforcementMode": "ENFORCING",
//        "resources": [
//          {
//            "name": "Topic:a-*",
//            "type": "Topic",
//            "ownerManagedAccess": false,
//            "displayName": "Topics that start with a-",
//            "attributes": {},
//            "uris": [],
//            "scopes": [
//              {
//                "name": "Create"
//              },
//              {
//                "name": "Delete"
//              },
//              {
//                "name": "Describe"
//              },
//              {
//                "name": "Write"
//              },
//              {
//                "name": "Read"
//              },
//              {
//                "name": "Alter"
//              },
//              {
//                "name": "DescribeConfigs"
//              },
//              {
//                "name": "AlterConfigs"
//              }
//            ]
//          },
//          {
//            "name": "Group:x-*",
//            "type": "Group",
//            "ownerManagedAccess": false,
//            "displayName": "Consumer groups that start with x-",
//            "attributes": {},
//            "uris": [],
//            "scopes": [
//              {
//                "name": "Describe"
//              },
//              {
//                "name": "Delete"
//              },
//              {
//                "name": "Read"
//              }
//            ]
//          },
//          {
//            "name": "Topic:x-*",
//            "type": "Topic",
//            "ownerManagedAccess": false,
//            "displayName": "Topics that start with x-",
//            "attributes": {},
//            "uris": [],
//            "scopes": [
//              {
//                "name": "Create"
//              },
//              {
//                "name": "Describe"
//              },
//              {
//                "name": "Delete"
//              },
//              {
//                "name": "Write"
//              },
//              {
//                "name": "Read"
//              },
//              {
//                "name": "Alter"
//              },
//              {
//                "name": "DescribeConfigs"
//              },
//              {
//                "name": "AlterConfigs"
//              }
//            ]
//          },
//          {
//            "name": "Group:a-*",
//            "type": "Group",
//            "ownerManagedAccess": false,
//            "displayName": "Groups that start with a-",
//            "attributes": {},
//            "uris": [],
//            "scopes": [
//              {
//                "name": "Describe"
//              },
//              {
//                "name": "Read"
//              }
//            ]
//          },
//          {
//            "name": "Group:*",
//            "type": "Group",
//            "ownerManagedAccess": false,
//            "displayName": "Any group",
//            "attributes": {},
//            "uris": [],
//            "scopes": [
//              {
//                "name": "Describe"
//              },
//              {
//                "name": "Read"
//              },
//              {
//                "name": "DescribeConfigs"
//              },
//              {
//                "name": "AlterConfigs"
//              }
//            ]
//          },
//          {
//            "name": "Topic:*",
//            "type": "Topic",
//            "ownerManagedAccess": false,
//            "displayName": "Any topic",
//            "attributes": {},
//            "uris": [],
//            "scopes": [
//              {
//                "name": "Create"
//              },
//              {
//                "name": "Delete"
//              },
//              {
//                "name": "Describe"
//              },
//              {
//                "name": "Write"
//              },
//              {
//                "name": "Read"
//              },
//              {
//                "name": "Alter"
//              },
//              {
//                "name": "DescribeConfigs"
//              },
//              {
//                "name": "AlterConfigs"
//              }
//            ]
//          },
//          {
//            "name": "Topic:b-*",
//            "type": "Topic",
//            "ownerManagedAccess": false,
//            "attributes": {},
//            "uris": [],
//            "scopes": [
//              {
//                "name": "Create"
//              },
//              {
//                "name": "Delete"
//              },
//              {
//                "name": "Describe"
//              },
//              {
//                "name": "Write"
//              },
//              {
//                "name": "Read"
//              },
//              {
//                "name": "Alter"
//              },
//              {
//                "name": "DescribeConfigs"
//              },
//              {
//                "name": "AlterConfigs"
//              }
//            ]
//          },
//          {
//            "name": "kafka-cluster:cluster2,Group:b_*",
//            "type": "Group",
//            "ownerManagedAccess": false,
//            "displayName": "Consumer groups on cluster cluster2 that start with b_",
//            "attributes": {},
//            "uris": [],
//            "scopes": [
//              {
//                "name": "Describe"
//              },
//              {
//                "name": "Delete"
//              },
//              {
//                "name": "Read"
//              }
//            ]
//          },
//          {
//            "name": "kafka-cluster:cluster2,Cluster:*",
//            "type": "Cluster",
//            "ownerManagedAccess": false,
//            "displayName": "Cluster scope on cluster2",
//            "attributes": {},
//            "uris": [],
//            "scopes": [
//              {
//                "name": "DescribeConfigs"
//              },
//              {
//                "name": "AlterConfigs"
//              },
//              {
//                "name": "ClusterAction"
//              },
//              {
//                "name": "IdempotentWrite"
//              }
//            ]
//          },
//          {
//            "name": "kafka-cluster:cluster2,Group:*",
//            "type": "Group",
//            "ownerManagedAccess": false,
//            "displayName": "Any group on cluster2",
//            "attributes": {},
//            "uris": [],
//            "scopes": [
//              {
//                "name": "Delete"
//              },
//              {
//                "name": "Describe"
//              },
//              {
//                "name": "Read"
//              },
//              {
//                "name": "DescribeConfigs"
//              },
//              {
//                "name": "AlterConfigs"
//              }
//            ]
//          },
//          {
//            "name": "kafka-cluster:cluster2,Topic:*",
//            "type": "Topic",
//            "ownerManagedAccess": false,
//            "displayName": "Any topic on cluster2",
//            "attributes": {},
//            "uris": [],
//            "scopes": [
//              {
//                "name": "Create"
//              },
//              {
//                "name": "Delete"
//              },
//              {
//                "name": "Describe"
//              },
//              {
//                "name": "Write"
//              },
//              {
//                "name": "IdempotentWrite"
//              },
//              {
//                "name": "Read"
//              },
//              {
//                "name": "Alter"
//              },
//              {
//                "name": "DescribeConfigs"
//              },
//              {
//                "name": "AlterConfigs"
//              }
//            ]
//          },
//          {
//            "name" : "Cluster:*",
//            "type" : "Cluster",
//            "ownerManagedAccess" : false,
//            "attributes" : { },
//            "uris" : [ ],
//            "scopes": [
//              {
//                "name": "DescribeConfigs"
//              },
//              {
//                "name": "AlterConfigs"
//              },
//              {
//                "name": "ClusterAction"
//              },
//              {
//                "name": "IdempotentWrite"
//              }
//            ]
//          }
//        ],
//        "policies": [
//          {
//            "name": "Dev Team A",
//            "type": "role",
//            "logic": "POSITIVE",
//            "decisionStrategy": "UNANIMOUS",
//            "config": {
//              "roles": "[{\"id\":\"Dev Team A\",\"required\":true}]"
//            }
//          },
//          {
//            "name": "Dev Team B",
//            "type": "role",
//            "logic": "POSITIVE",
//            "decisionStrategy": "UNANIMOUS",
//            "config": {
//              "roles": "[{\"id\":\"Dev Team B\",\"required\":true}]"
//            }
//          },
//          {
//            "name": "Ops Team",
//            "type": "role",
//            "logic": "POSITIVE",
//            "decisionStrategy": "UNANIMOUS",
//            "config": {
//              "roles": "[{\"id\":\"Ops Team\",\"required\":true}]"
//            }
//          },
//          {
//            "name" : "ClusterManager Group",
//            "type" : "group",
//            "logic" : "POSITIVE",
//            "decisionStrategy" : "UNANIMOUS",
//            "config" : {
//              "groups" : "[{\"path\":\"/ClusterManager Group\",\"extendChildren\":false}]"
//            }
//          }, {
//            "name" : "ClusterManager of cluster2 Group",
//            "type" : "group",
//            "logic" : "POSITIVE",
//            "decisionStrategy" : "UNANIMOUS",
//            "config" : {
//              "groups" : "[{\"path\":\"/ClusterManager-cluster2 Group\",\"extendChildren\":false}]"
//            }
//          },
//          {
//            "name": "Dev Team A owns topics that start with a- on any cluster",
//            "type": "resource",
//            "logic": "POSITIVE",
//            "decisionStrategy": "UNANIMOUS",
//            "config": {
//              "resources": "[\"Topic:a-*\"]",
//              "applyPolicies": "[\"Dev Team A\"]"
//            }
//          },
//          {
//            "name": "Dev Team A can write to topics that start with x- on any cluster",
//            "type": "scope",
//            "logic": "POSITIVE",
//            "decisionStrategy": "UNANIMOUS",
//            "config": {
//              "resources": "[\"Topic:x-*\"]",
//              "scopes": "[\"Describe\",\"Write\"]",
//              "applyPolicies": "[\"Dev Team A\"]"
//            }
//          },
//          {
//             "name": "Dev Team A can do IdempotentWrites on cluster cluster2",
//             "config": {
//                 "applyPolicies": "[\"Dev Team A\"]",
//                 "resources": "[\"kafka-cluster:cluster2,Cluster:*\"]",
//                 "scopes": "[\"IdempotentWrite\"]"
//             },
//             "decisionStrategy": "UNANIMOUS",
//             "logic": "POSITIVE",
//             "type": "scope"
//          },
//          {
//            "name": "Dev Team B owns topics that start with b- on cluster cluster2",
//            "type": "resource",
//            "logic": "POSITIVE",
//            "decisionStrategy": "UNANIMOUS",
//            "config": {
//              "resources": "[\"Topic:b-*\"]",
//              "applyPolicies": "[\"Dev Team B\"]"
//            }
//          },
//          {
//            "name": "Dev Team B can do IdempotentWrites on cluster cluster2",
//            "type": "scope",
//            "logic": "POSITIVE",
//            "decisionStrategy": "UNANIMOUS",
//            "config": {
//              "resources": "[\"kafka-cluster:cluster2,Cluster:*\"]",
//              "scopes": "[\"IdempotentWrite\"]",
//              "applyPolicies": "[\"Dev Team B\"]"
//            }
//          },
//          {
//            "name": "Dev Team B can read from topics that start with x- on any cluster",
//            "type": "scope",
//            "logic": "POSITIVE",
//            "decisionStrategy": "UNANIMOUS",
//            "config": {
//              "resources": "[\"Topic:x-*\"]",
//              "scopes": "[\"Describe\",\"Read\"]",
//              "applyPolicies": "[\"Dev Team B\"]"
//            }
//          },
//          {
//            "name": "Dev Team B can update consumer group offsets that start with x- on any cluster",
//            "type": "scope",
//            "logic": "POSITIVE",
//            "decisionStrategy": "UNANIMOUS",
//            "config": {
//              "resources": "[\"Group:x-*\"]",
//              "scopes": "[\"Describe\",\"Read\"]",
//              "applyPolicies": "[\"Dev Team B\"]"
//            }
//          },
//          {
//            "name": "Dev Team A can use consumer groups that start with a- on any cluster",
//            "type": "resource",
//            "logic": "POSITIVE",
//            "decisionStrategy": "UNANIMOUS",
//            "config": {
//              "resources": "[\"Group:a-*\"]",
//              "applyPolicies": "[\"Dev Team A\"]"
//            }
//          },
//          {
//            "name": "ClusterManager of cluster2 Group has full access to topics on cluster2",
//            "type": "resource",
//            "logic": "POSITIVE",
//            "decisionStrategy": "UNANIMOUS",
//            "config": {
//              "resources": "[\"kafka-cluster:cluster2,Topic:*\"]",
//              "applyPolicies": "[\"ClusterManager of cluster2 Group\"]"
//            }
//          },
//          {
//            "name": "ClusterManager of cluster2 Group has full access to consumer groups on cluster2",
//            "type": "resource",
//            "logic": "POSITIVE",
//            "decisionStrategy": "UNANIMOUS",
//            "config": {
//              "resources": "[\"kafka-cluster:cluster2,Group:*\"]",
//              "applyPolicies": "[\"ClusterManager of cluster2 Group\"]"
//            }
//          },
//          {
//            "name": "ClusterManager of cluster2 Group has full access to cluster config on cluster2",
//            "type": "resource",
//            "logic": "POSITIVE",
//            "decisionStrategy": "UNANIMOUS",
//            "config": {
//              "resources": "[\"kafka-cluster:cluster2,Cluster:*\"]",
//              "applyPolicies": "[\"ClusterManager of cluster2 Group\"]"
//            }
//          }, {
//            "name" : "ClusterManager Group has full access to manage and affect groups",
//            "type" : "resource",
//            "logic" : "POSITIVE",
//            "decisionStrategy" : "UNANIMOUS",
//            "config" : {
//              "resources" : "[\"Group:*\"]",
//              "applyPolicies" : "[\"ClusterManager Group\"]"
//            }
//          }, {
//            "name" : "ClusterManager Group has full access to manage and affect topics",
//            "type" : "resource",
//            "logic" : "POSITIVE",
//            "decisionStrategy" : "UNANIMOUS",
//            "config" : {
//              "resources" : "[\"Topic:*\"]",
//              "applyPolicies" : "[\"ClusterManager Group\"]"
//            }
//          }, {
//            "name" : "ClusterManager Group has full access to cluster config",
//            "type" : "resource",
//            "logic" : "POSITIVE",
//            "decisionStrategy" : "UNANIMOUS",
//            "config" : {
//              "resources" : "[\"Cluster:*\"]",
//              "applyPolicies" : "[\"ClusterManager Group\"]"
//            }
//          },
//          {
//            "name": "Team A Client",
//            "type": "client",
//            "logic": "POSITIVE",
//            "decisionStrategy": "UNANIMOUS",
//            "config": {
//              "clients": "[\"team-a-client\"]"
//            }
//         },
//         {
//           "name": "Team A Client has IdempotentWrite permission on cluster",
//           "type": "scope",
//           "logic": "POSITIVE",
//           "decisionStrategy": "UNANIMOUS",
//           "config": {
//             "resources": "[\"Cluster:*\"]",
//             "scopes": "[\"IdempotentWrite\"]",
//             "applyPolicies": "[\"Team A Client\"]"
//           }
//         },
//         {
//           "name": "Team B Client",
//           "type": "client",
//           "logic": "POSITIVE",
//           "decisionStrategy": "UNANIMOUS",
//           "config": {
//             "clients": "[\"team-b-client\"]"
//           }
//         },
//         {
//           "name": "Team B Client has IdempotentWrite permission on cluster",
//           "type": "scope",
//           "logic": "POSITIVE",
//           "decisionStrategy": "UNANIMOUS",
//           "config": {
//             "resources": "[\"Cluster:*\"]",
//             "scopes": "[\"IdempotentWrite\"]",
//             "applyPolicies": "[\"Team B Client\"]"
//           }
//         }
//        ],
//        "scopes": [
//          {
//            "name": "Create"
//          },
//          {
//            "name": "Read"
//          },
//          {
//            "name": "Write"
//          },
//          {
//            "name": "Delete"
//          },
//          {
//            "name": "Alter"
//          },
//          {
//            "name": "Describe"
//          },
//          {
//            "name": "ClusterAction"
//          },
//          {
//            "name": "DescribeConfigs"
//          },
//          {
//            "name": "AlterConfigs"
//          },
//          {
//            "name": "IdempotentWrite"
//          }
//        ],
//        "decisionStrategy": "AFFIRMATIVE"
//      }
//    },
//    {
//      "clientId": "kafka-cli",
//      "enabled": true,
//      "clientAuthenticatorType": "client-secret",
//      "secret": "kafka-cli-secret",
//      "bearerOnly": false,
//      "consentRequired": false,
//      "standardFlowEnabled": false,
//      "implicitFlowEnabled": false,
//      "directAccessGrantsEnabled": true,
//      "serviceAccountsEnabled": false,
//      "publicClient": true,
//      "fullScopeAllowed": true
//    }
//  ]
//}
}
