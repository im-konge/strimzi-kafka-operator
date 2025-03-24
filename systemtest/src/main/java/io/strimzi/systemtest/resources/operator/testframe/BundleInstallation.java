/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.resources.operator.testframe;

import io.skodjob.testframe.installation.InstallationMethod;
import io.strimzi.test.TestUtils;

public class BundleInstallation implements InstallationMethod {

    public static final String CO_INSTALL_DIR = TestUtils.USER_PATH + "/../packaging/install/cluster-operator";
    private ClusterOperatorConfiguration clusterOperatorConfiguration;

    public BundleInstallation(ClusterOperatorConfiguration clusterOperatorConfiguration) {
        this.clusterOperatorConfiguration = clusterOperatorConfiguration;
    }

    @Override
    public void install() {

    }

    @Override
    public void delete() {

    }
}
