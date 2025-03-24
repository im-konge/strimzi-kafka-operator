/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.resources.operator.testframe;

import io.skodjob.testframe.installation.InstallationMethod;
import io.strimzi.systemtest.Environment;

public class SetupClusterOperator {

    private ClusterOperatorConfiguration clusterOperatorConfiguration = new ClusterOperatorConfiguration();

    public SetupClusterOperator withDefaultConfiguration() {
        this.clusterOperatorConfiguration = new ClusterOperatorConfiguration();
        return this;
    }

    public SetupClusterOperator withCustomConfiguration(ClusterOperatorConfiguration clusterOperatorConfiguration) {
        this.clusterOperatorConfiguration = clusterOperatorConfiguration;
        return this;
    }

    public void install() {
        install(getInstallationMethodFromEnvVariable());
    }

    private void install(InstallationMethod installationMethod) {
        installationMethod.install();
    }

    public void installUsingOlm() {
        install(new OlmInstallation(clusterOperatorConfiguration));
    }

    public void installUsingHelm() {
        install(new HelmInstallation(clusterOperatorConfiguration));
    }

    public void installUsingYamlManifests() {
        install(new BundleInstallation(clusterOperatorConfiguration));
    }

    private InstallationMethod getInstallationMethodFromEnvVariable() {
        return switch (Environment.CLUSTER_OPERATOR_INSTALL_TYPE_TEST_FRAME) {
            case Yaml -> new BundleInstallation(clusterOperatorConfiguration);
            case Helm -> new HelmInstallation(clusterOperatorConfiguration);
            case Olm -> new OlmInstallation(clusterOperatorConfiguration);
            case Unknown -> throw new RuntimeException("Unknown installation type");
        };
    }
}
