/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.security;

import java.net.InetSocketAddress;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.opensearch.Version;
import org.opensearch.common.settings.Settings;
import org.opensearch.plugins.PluginInfo;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.test.LocalTestCluster;
import org.opensearch.test.framework.certificate.TestCertificates;
import org.opensearch.test.framework.cluster.MinimumSecuritySettingsSupplierFactory;
import org.opensearch.test.framework.cluster.OpenSearchClientProvider;
import org.opensearch.test.framework.cluster.TestRestClient;
import org.opensearch.transport.Netty4ModulePlugin;

/** Core owns node lifecycle; certificates, TLS configuration and clients stay in Security. */
public class SharedCoreClusterTest implements OpenSearchClientProvider {
    @Rule
    public TemporaryFolder home = new TemporaryFolder();

    private final TestCertificates certificates = new TestCertificates(1);
    private LocalTestCluster cluster;

    @Before
    public void startCluster() throws Exception {
        Settings settings = new MinimumSecuritySettingsSupplierFactory(certificates).minimumOpenSearchSettings(
            true,
            Settings.builder().put(ConfigConstants.SECURITY_SSL_ONLY, true).build()
        ).get(0);
        cluster = new LocalTestCluster(
            home.getRoot().toPath(),
            settings,
            List.of(plugin(Netty4ModulePlugin.class), plugin(OpenSearchSecurityPlugin.class))
        );
        cluster.start();
    }

    @After
    public void stopCluster() throws Exception {
        if (cluster != null) {
            cluster.close();
        }
    }

    @Test
    public void servesHttpsWithSecuritySslOnlyMode() {
        try (TestRestClient client = getRestClient()) {
            client.get("_cat/indices").assertStatusCode(200);
            client.getAuthInfo().assertStatusCode(400);
        }
    }

    private static PluginInfo plugin(Class<?> type) {
        return new PluginInfo(type.getName(), "test", "test", Version.CURRENT, "21", type.getName(), null, List.of(), false);
    }

    @Override
    public String getClusterName() {
        return "local-test-cluster";
    }

    @Override
    public TestCertificates getTestCertificates() {
        return certificates;
    }

    @Override
    public InetSocketAddress getHttpAddress() {
        return cluster.httpAddress().address();
    }

    @Override
    public InetSocketAddress getTransportAddress() {
        return cluster.transportAddress().address();
    }
}
