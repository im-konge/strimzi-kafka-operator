/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.assembly;

import io.strimzi.api.kafka.model.kafka.KafkaClusterSpec;
import io.strimzi.api.kafka.model.kafka.KafkaResources;
import io.strimzi.api.kafka.model.kafka.KafkaStatus;
import io.strimzi.api.kafka.model.kafka.features.KafkaFeature;
import io.strimzi.operator.cluster.model.KafkaCluster;
import io.strimzi.operator.common.AdminClientProvider;
import io.strimzi.operator.common.Reconciliation;
import io.strimzi.operator.common.ReconciliationLogger;
import io.strimzi.operator.common.auth.Identity;
import io.strimzi.operator.common.model.StatusUtils;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.FeatureMetadata;
import org.apache.kafka.clients.admin.FeatureUpdate;
import org.apache.kafka.clients.admin.UpdateFeaturesOptions;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Class that handles updates of the Kafka features.
 */
public class KafkaFeaturesManager {
    private static final ReconciliationLogger LOGGER = ReconciliationLogger.create(KafkaFeaturesManager.class.getName());

    private KafkaFeaturesManager() { }

    /**
     * Method that is responsible for updating Kafka with features in the Kafka CR's spec.
     * In case that there are no Kafka features set by user or the versions are same as in Kafka, it does nothing.
     *
     * @param reconciliation            Reconciliation marker
     * @param coIdentity                Trust set and identity for authentication for connecting to the Kafka cluster
     * @param adminClientProvider       Kafka Admin client provider
     * @param kafkaFeatures             User provided list of Kafka features.
     * @param kafkaStatus               Kafka status
     *
     * @return  CompletionStage that completes when the features configuration is finished (or was not needed) and the status is updated
     */
    public static CompletionStage<Void> maybeUpdateKafkaFeatures(
        Reconciliation reconciliation,
        Identity coIdentity,
        AdminClientProvider adminClientProvider,
        List<KafkaFeature> kafkaFeatures,
        KafkaStatus kafkaStatus
    ) {
        if (kafkaFeatures != null && !kafkaFeatures.isEmpty()) {
            String bootstrapHostname = KafkaResources.bootstrapServiceName(reconciliation.name()) + "." + reconciliation.namespace() + ".svc:" + KafkaCluster.REPLICATION_PORT;
            LOGGER.debugCr(reconciliation, "Creating AdminClient for Kafka cluster in namespace {}", reconciliation.namespace());
            Admin kafkaAdmin = adminClientProvider.createAdminClient(bootstrapHostname, coIdentity.trustSet(), coIdentity.authIdentity());

            return kafkaAdmin.describeFeatures().featureMetadata().toCompletionStage()
                .thenCompose(featureMetadata -> CompletableFuture.allOf(kafkaFeatures.stream()
                        .map(feature -> maybeUpdateKafkaFeature(reconciliation, kafkaAdmin, featureMetadata, feature, kafkaStatus)
                            .exceptionally(ex -> {
                                kafkaStatus.addCondition(StatusUtils.buildWarningCondition("KafkaFeatureUpdateFail", feature.getName() + ": " + ex.getMessage()));
                                return null;
                            })
                            .toCompletableFuture()
                        )
                        .toArray(CompletableFuture[]::new)
                    )
                )
                .thenCompose(v -> kafkaAdmin.describeFeatures().featureMetadata().toCompletionStage())
                .thenAccept(featureMetadata -> {
                    List<KafkaFeature> featureStatuses = featureMetadata.supportedFeatures().keySet().stream()
                        .filter(featureName -> !KafkaClusterSpec.FORBIDDEN_KAFKA_FEATURES.contains(featureName))
                        .map(featureName -> {
                            KafkaFeature kafkaFeature = new KafkaFeature();
                            kafkaFeature.setName(featureName);
                            kafkaFeature.setVersion(featureMetadata.finalizedFeatures().containsKey(featureName) ?
                                (int) featureMetadata.finalizedFeatures().get(featureName).maxVersionLevel() : 0);

                            return kafkaFeature;
                        })
                        .toList();

                    kafkaStatus.setKafkaFeatures(featureStatuses);
                })
                .exceptionally(ex -> {
                    LOGGER.warnCr(reconciliation, "Failed to describe Kafka features for status update: {}", ex.getMessage());
                    kafkaStatus.addCondition(StatusUtils.buildWarningCondition("KafkaFeatureDescribeFailed", ex.getMessage()));

                    return null;
                })
                .whenComplete((result, error) -> {
                    // Close the Admin client and return the original result
                    LOGGER.debugCr(reconciliation, "Closing the Kafka Admin API connection");
                    kafkaAdmin.close();
                });
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static CompletionStage<Void> maybeUpdateKafkaFeature(
        Reconciliation reconciliation,
        Admin kafkaAdmin,
        FeatureMetadata featureMetadata,
        KafkaFeature feature,
        KafkaStatus status
    ) {
        if (!featureMetadata.supportedFeatures().containsKey(feature.getName())) {
            LOGGER.warnCr(reconciliation, "Feature {} is not supported by this Kafka version", feature.getName());
            status.addCondition(StatusUtils.buildWarningCondition("KafkaFeatureUnsupported", feature.getName() + " is not supported by this Kafka version"));
            return CompletableFuture.completedFuture(null);
        }

        short desiredFeatureLevel = feature.getVersion().shortValue();
        short currentFeatureLevel = currentVersionFromMetadata(featureMetadata, feature.getName());

        if (currentFeatureLevel == desiredFeatureLevel)  {
            LOGGER.debugCr(reconciliation, "{} is already set to the desired version {}", feature.getName(), currentFeatureLevel);
            return CompletableFuture.completedFuture(null);
        }

        return updateVersion(reconciliation, kafkaAdmin, feature.getName(), currentFeatureLevel, desiredFeatureLevel)
            .thenCompose(voidResult -> {
                LOGGER.infoCr(reconciliation, "Successfully updated {} to {}", feature.getName(), desiredFeatureLevel);
                return CompletableFuture.<Void>completedFuture(null);
            })
            .exceptionallyCompose(error -> {
                // We failed to update the feature version, but we do not want to break the
                // reconciliation. We log the error and update the feature version status.
                LOGGER.warnCr(reconciliation, "Failed to update {} to {} (the current version is {})", feature.getName(), desiredFeatureLevel, currentFeatureLevel, error);
                status.addCondition(StatusUtils.buildWarningCondition("KafkaFeatureUpdateFailed", "Failed to update " + feature.getName()  + " to " + desiredFeatureLevel));
                return CompletableFuture.completedFuture(null);
            });
    }

    private static short currentVersionFromMetadata(
        FeatureMetadata featureMetadata,
        String kafkaFeature
    ) {
        if (featureMetadata.finalizedFeatures().get(kafkaFeature) != null)  {
            return featureMetadata.finalizedFeatures().get(kafkaFeature).maxVersionLevel();
        } else {
            // the finalizedFeatures doesn't list disabled features (that has version set to 0)
            return (short) 0;
        }
    }

    /* test */ static CompletionStage<Void> updateVersion(
        Reconciliation reconciliation,
        Admin kafkaAdmin,
        String featureName,
        short currentKafkaFeatureLevel,
        short desiredKafkaFeatureLevel
    )  {
        UpdateFeaturesOptions options = new UpdateFeaturesOptions().validateOnly(false);
        FeatureUpdate.UpgradeType upgradeType = desiredKafkaFeatureLevel > currentKafkaFeatureLevel ? FeatureUpdate.UpgradeType.UPGRADE : FeatureUpdate.UpgradeType.SAFE_DOWNGRADE;
        FeatureUpdate featureUpdate = new FeatureUpdate(desiredKafkaFeatureLevel, upgradeType);

        LOGGER.infoCr(reconciliation, "Updating {} from {} to {}", featureName, currentKafkaFeatureLevel, desiredKafkaFeatureLevel);

        return kafkaAdmin.updateFeatures(Map.of(featureName, featureUpdate), options).values().get(featureName).toCompletionStage();
    }
}
