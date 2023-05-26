/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.node;

import java.util.Arrays;
import java.util.Collections;

import org.opensearch.common.settings.Settings;
import org.opensearch.plugins.Plugin;

public class PluginAwareNode extends Node {

    private final boolean clusterManagerEligible;

    @SafeVarargs
    public PluginAwareNode(boolean clusterManagerEligible, final Settings preparedSettings, final Class<? extends Plugin>... plugins) {
        super(
            InternalSettingsPreparer.prepareEnvironment(preparedSettings, Collections.emptyMap(), null, () -> System.getenv("HOSTNAME")),
            Arrays.asList(plugins),
            true
        );
        this.clusterManagerEligible = clusterManagerEligible;
    }

    public boolean isClusterManagerEligible() {
        return clusterManagerEligible;
    }
}
