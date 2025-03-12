/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.resources.types.customresource;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.interfaces.ResourceType;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePoolList;
import io.strimzi.api.kafka.model.nodepool.ProcessRoles;
import io.strimzi.operator.common.model.Labels;
import io.strimzi.systemtest.TestConstants;
import io.strimzi.systemtest.utils.kubeUtils.objects.PersistentVolumeClaimUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static io.strimzi.operator.common.Util.hashStub;

public class KafkaNodePoolType implements ResourceType<KafkaNodePool> {
    private final MixedOperation<KafkaNodePool, KafkaNodePoolList, Resource<KafkaNodePool>> client;

    public KafkaNodePoolType() {
        client = Crds.kafkaNodePoolOperation(KubeResourceManager.getKubeClient().getClient());
    }

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return client;
    }

    @Override
    public String getKind() {
        return KafkaNodePool.RESOURCE_KIND;
    }

    @Override
    public void create(KafkaNodePool kafkaNodePool) {
        client.inNamespace(kafkaNodePool.getMetadata().getNamespace()).resource(kafkaNodePool).create();
    }

    @Override
    public void update(KafkaNodePool kafkaNodePool) {
        client.inNamespace(kafkaNodePool.getMetadata().getNamespace()).resource(kafkaNodePool).update();
    }

    @Override
    public void delete(KafkaNodePool kafkaNodePool) {
        String namespaceName = kafkaNodePool.getMetadata().getNamespace();
        String clusterName = kafkaNodePool.getMetadata().getLabels().get(Labels.STRIMZI_CLUSTER_LABEL);
        String podsetName = String.join("-", clusterName, kafkaNodePool.getMetadata().getName());

        client.inNamespace(kafkaNodePool.getMetadata().getNamespace()).resource(kafkaNodePool).delete();

        // once KafkaNodePool is deleted, we should delete PVCs and wait for the deletion to be completed
        // the Kafka pods will not be deleted during Kafka resource deletion -> so if we would delete PVCs during Kafka deletion
        // they will not be deleted too
        // additional deletion of pvcs with specification deleteClaim set to false which were not deleted prior this method
        PersistentVolumeClaimUtils.deletePvcsByPrefixWithWait(namespaceName, podsetName);
    }

    @Override
    public void replace(KafkaNodePool kafkaNodePool, Consumer<KafkaNodePool> consumer) {
        KafkaNodePool toBeReplaced = client.inNamespace(kafkaNodePool.getMetadata().getNamespace()).withName(kafkaNodePool.getMetadata().getName()).get();
        consumer.accept(toBeReplaced);
        update(toBeReplaced);
    }

    @Override
    public boolean isReady(KafkaNodePool kafkaNodePool) {
        return client.inNamespace(kafkaNodePool.getMetadata().getNamespace()).withName(kafkaNodePool.getMetadata().getName()).get() != null;
    }

    @Override
    public boolean isDeleted(KafkaNodePool kafkaNodePool) {
        return client.inNamespace(kafkaNodePool.getMetadata().getNamespace()).withName(kafkaNodePool.getMetadata().getName()).get() == null;
    }

    public static LabelSelector getLabelSelector(String clusterName, String poolName, ProcessRoles processRole) {
        Map<String, String> matchLabels = new HashMap<>();

        matchLabels.put(Labels.STRIMZI_CLUSTER_LABEL, clusterName);
        matchLabels.put(Labels.STRIMZI_KIND_LABEL, Kafka.RESOURCE_KIND);
        matchLabels.put(Labels.STRIMZI_POOL_NAME_LABEL, poolName);

        switch (processRole) {
            case BROKER -> matchLabels.put(Labels.STRIMZI_BROKER_ROLE_LABEL, "true");
            case CONTROLLER -> matchLabels.put(Labels.STRIMZI_CONTROLLER_ROLE_LABEL, "true");
            default -> throw new RuntimeException("No role for KafkaNodePool specified");
        }

        return new LabelSelectorBuilder()
            .withMatchLabels(matchLabels)
            .build();
    }

    public static String getBrokerPoolName(String clusterName) {
        return TestConstants.BROKER_ROLE_PREFIX + hashStub(clusterName);
    }

    public static String getControllerPoolName(String clusterName) {
        return TestConstants.CONTROLLER_ROLE_PREFIX + hashStub(clusterName);
    }

    public static String getMixedPoolName(String clusterName) {
        return TestConstants.MIXED_ROLE_PREFIX + hashStub(clusterName);
    }
}
