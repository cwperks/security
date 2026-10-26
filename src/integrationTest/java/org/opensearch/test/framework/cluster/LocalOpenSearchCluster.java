/*
* Copyright 2015-2021 floragunn GmbH
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
*/

/*
* SPDX-License-Identifier: Apache-2.0
*
* The OpenSearch Contributors require contributions made to
* this file be licensed under the Apache-2.0 license or a
* compatible open source license.
*
* Modifications Copyright OpenSearch Contributors. See
* GitHub history for details.
*/

package org.opensearch.test.framework.cluster;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.opensearch.Version;
import org.opensearch.cluster.health.ClusterHealthStatus;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.plugins.Plugin;
import org.opensearch.plugins.PluginInfo;
import org.opensearch.test.cluster.LocalTestCluster;
import org.opensearch.test.framework.certificate.TestCertificates;
import org.opensearch.transport.client.Client;

/** Security-specific clients and certificates around core's shared cluster lifecycle. */
public class LocalOpenSearchCluster {
    static {
        System.setProperty("opensearch.enforce.bootstrap.checks", "true");
    }

    private final LocalTestCluster cluster;
    private final TestCertificates testCertificates;
    private final Random random = new Random();

    public LocalOpenSearchCluster(
        String clusterName,
        ClusterManager clusterManager,
        NodeSettingsSupplier nodeSettingsSupplier,
        List<Class<? extends Plugin>> plugins,
        List<PluginInfo> additionalPlugins,
        TestCertificates testCertificates,
        Integer expectedNodeStartCount
    ) {
        this.testCertificates = testCertificates;
        List<LocalTestCluster.NodeSpec> topology = new ArrayList<>();
        for (ClusterManager.NodeSettings settings : clusterManager.getNodeSettings()) {
            List<String> roles = new ArrayList<>();
            for (NodeRole role : NodeRole.values()) {
                if (settings.containRole(role)) {
                    roles.add(role.name().toLowerCase(Locale.ROOT));
                }
            }
            // Preserve Security's plugin defaults and explicit descriptor precedence.
            Map<String, PluginInfo> descriptors = new LinkedHashMap<>();
            for (Class<? extends Plugin> plugin : settings.pluginsWithAddition(plugins)) {
                descriptors.put(
                    plugin.getName(),
                    new PluginInfo(
                        plugin.getName(),
                        "classpath plugin",
                        "1.0",
                        Version.CURRENT,
                        "21",
                        plugin.getName(),
                        null,
                        List.of(),
                        false
                    )
                );
            }
            for (PluginInfo plugin : additionalPlugins) {
                descriptors.put(plugin.getClassname(), plugin);
            }
            topology.add(new LocalTestCluster.NodeSpec(roles, List.copyOf(descriptors.values())));
        }
        cluster = new LocalTestCluster(
            clusterName,
            topology,
            number -> Settings.builder()
                .put("gateway.auto_import_dangling_indices", true)
                .put(nodeSettingsSupplier == null ? Settings.EMPTY : nodeSettingsSupplier.get(number))
                .build(),
            expectedNodeStartCount
        );
    }

    public void start() throws Exception {
        cluster.start();
    }

    public void stop() {
        cluster.stop();
    }

    public void destroy() {
        cluster.close();
    }

    public boolean isStarted() {
        return cluster.isStarted();
    }

    public String getClusterName() {
        return cluster.getClusterName();
    }

    public String getSnapshotDirPath() {
        return cluster.getSnapshotDirPath();
    }

    public Random getRandom() {
        return random;
    }

    public Node clientNode() {
        return wrap(cluster.clientNode());
    }

    public Node clusterManagerNode() {
        return wrap(cluster.clusterManagerNode());
    }

    public Node getNodeByName(String name) {
        return wrap(cluster.getNodeByName(name));
    }

    public List<Node> getNodes() {
        return cluster.getNodes().stream().map(this::wrap).toList();
    }

    public void waitForCluster(ClusterHealthStatus status, TimeValue timeout, int expectedNodeCount) throws IOException {
        cluster.waitForCluster(status, timeout, expectedNodeCount);
    }

    private Node wrap(LocalTestCluster.Node node) {
        return node == null ? null : new Node(node);
    }

    @Override
    public String toString() {
        return cluster.toString();
    }

    /** Adds Security's authenticated client factories to a core node handle. */
    public class Node implements OpenSearchClientProvider {
        private final LocalTestCluster.Node node;

        private Node(LocalTestCluster.Node node) {
            this.node = node;
        }

        public int nodeNumber() {
            return node.nodeNumber();
        }

        public Client getInternalNodeClient() {
            return node.getInternalNodeClient();
        }

        public org.opensearch.node.Node esNode() {
            return node.esNode();
        }

        public boolean isRunning() {
            return node.isRunning();
        }

        public void restart() throws Exception {
            node.restart();
        }

        public <X> X getInjectable(Class<X> type) {
            return node.getInjectable(type);
        }

        public CompletableFuture<Boolean> stop(long timeout, TimeUnit unit) {
            return node.stop(timeout, unit);
        }

        public String getNodeName() {
            return node.getNodeName();
        }

        @Override
        public InetSocketAddress getHttpAddress() {
            return node.getHttpAddress();
        }

        @Override
        public InetSocketAddress getTransportAddress() {
            return node.getTransportAddress();
        }

        @Override
        public String getClusterName() {
            return cluster.getClusterName();
        }

        @Override
        public TestCertificates getTestCertificates() {
            return testCertificates;
        }

        @Override
        public String toString() {
            return node.toString();
        }
    }
}
