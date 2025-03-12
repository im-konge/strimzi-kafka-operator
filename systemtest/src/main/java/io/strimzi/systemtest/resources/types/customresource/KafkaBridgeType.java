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
import io.strimzi.api.kafka.model.bridge.KafkaBridge;
import io.strimzi.api.kafka.model.bridge.KafkaBridgeList;
import io.strimzi.api.kafka.model.bridge.KafkaBridgeStatus;
import io.strimzi.api.kafka.model.common.Condition;
import io.strimzi.operator.common.model.Labels;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class KafkaBridgeType implements ResourceType<KafkaBridge> {
    private final MixedOperation<KafkaBridge, KafkaBridgeList, Resource<KafkaBridge>> client;

    public KafkaBridgeType() {
        client = Crds.kafkaBridgeOperation(KubeResourceManager.getKubeClient().getClient());
    }

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return client;
    }

    @Override
    public String getKind() {
        return KafkaBridge.RESOURCE_KIND;
    }

    @Override
    public void create(KafkaBridge kafkaBridge) {
        client.inNamespace(kafkaBridge.getMetadata().getNamespace()).resource(kafkaBridge).create();
    }

    @Override
    public void update(KafkaBridge kafkaBridge) {
        client.inNamespace(kafkaBridge.getMetadata().getNamespace()).resource(kafkaBridge).update();
    }

    @Override
    public void delete(KafkaBridge kafkaBridge) {
        client.inNamespace(kafkaBridge.getMetadata().getNamespace()).resource(kafkaBridge).delete();
    }

    @Override
    public void replace(KafkaBridge kafkaBridge, Consumer<KafkaBridge> consumer) {
        KafkaBridge toBeReplaced = client.inNamespace(kafkaBridge.getMetadata().getNamespace()).withName(kafkaBridge.getMetadata().getName()).get();
        consumer.accept(toBeReplaced);
        update(toBeReplaced);
    }

    @Override
    public boolean isReady(KafkaBridge kafkaBridge) {
        KafkaBridgeStatus kafkaBridgeStatus = client.inNamespace(kafkaBridge.getMetadata().getNamespace()).resource(kafkaBridge).get().getStatus();
        Optional<Condition> readyCondition = kafkaBridgeStatus.getConditions().stream().filter(condition -> condition.getType().equals("Ready")).findFirst();

        return readyCondition.map(condition -> condition.getStatus().equals("True")).orElse(false);
    }

    @Override
    public boolean isDeleted(KafkaBridge kafkaBridge) {
        return kafkaBridge == null;
    }

    public static LabelSelector getLabelSelector(String clusterName, String componentName) {
        Map<String, String> matchLabels = new HashMap<>();
        matchLabels.put(Labels.STRIMZI_CLUSTER_LABEL, clusterName);
        matchLabels.put(Labels.STRIMZI_KIND_LABEL, KafkaBridge.RESOURCE_KIND);
        matchLabels.put(Labels.STRIMZI_NAME_LABEL, componentName);

        return new LabelSelectorBuilder()
            .withMatchLabels(matchLabels)
            .build();
    }
}
