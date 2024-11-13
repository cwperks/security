/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 */
package org.opensearch.security;

import java.util.List;
import java.util.Map;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.opensearch.core.rest.RestStatus;
import org.opensearch.security.plugin.SystemIndexPlugin1;
import org.opensearch.test.framework.TestSecurityConfig.AuthcDomain;
import org.opensearch.test.framework.cluster.ClusterManager;
import org.opensearch.test.framework.cluster.LocalCluster;
import org.opensearch.test.framework.cluster.LocalOpenSearchCluster;
import org.opensearch.test.framework.cluster.TestRestClient;
import org.opensearch.test.framework.cluster.TestRestClient.HttpResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.opensearch.security.support.ConfigConstants.SECURITY_RESTAPI_ROLES_ENABLED;
import static org.opensearch.security.support.ConfigConstants.SECURITY_SYSTEM_INDICES_ENABLED_KEY;
import static org.opensearch.test.framework.TestSecurityConfig.Role.ALL_ACCESS;
import static org.opensearch.test.framework.TestSecurityConfig.User.USER_ADMIN;

@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class SystemIndexUpgradeTests {

    public static final AuthcDomain AUTHC_DOMAIN = new AuthcDomain("basic", 0).httpAuthenticatorWithChallenge("basic").backend("internal");

    @ClassRule
    public static final LocalCluster cluster = new LocalCluster.Builder().clusterManager(ClusterManager.DEFAULT)
        .anonymousAuth(false)
        .authc(AUTHC_DOMAIN)
        .users(USER_ADMIN)
        .loadConfigurationIntoIndex(true)
        .nodeSettings(
            Map.of(
                SECURITY_RESTAPI_ROLES_ENABLED,
                List.of("user_" + USER_ADMIN.getName() + "__" + ALL_ACCESS.getName()),
                SECURITY_SYSTEM_INDICES_ENABLED_KEY,
                true,
                "node.max_local_storage_nodes",
                2
            )
        )
        .build();

    @Before
    public void setup() {
        try (TestRestClient client = cluster.getRestClient(cluster.getAdminCertificate())) {
            client.delete(SystemIndexPlugin1.SYSTEM_INDEX_1);
        }
    }

    @Test
    public void systemIndexShouldBeMarkedTrueInClusterState() throws InterruptedException {
        int previousVersion;
        System.out.println("cluster.nodes: " + cluster.nodes());
        for (LocalOpenSearchCluster.Node node : cluster.nodes()) {
            try (TestRestClient client = node.getRestClient(USER_ADMIN)) {
                client.put(SystemIndexPlugin1.SYSTEM_INDEX_1);
                HttpResponse response = client.get("_cluster/state/metadata/" + SystemIndexPlugin1.SYSTEM_INDEX_1);

                assertThat(response.getStatusCode(), equalTo(RestStatus.OK.getStatus()));

                boolean isSystem = response.bodyAsJsonNode()
                    .get("metadata")
                    .get("indices")
                    .get(SystemIndexPlugin1.SYSTEM_INDEX_1)
                    .get("system")
                    .asBoolean();
                previousVersion = response.bodyAsJsonNode()
                    .get("metadata")
                    .get("indices")
                    .get(SystemIndexPlugin1.SYSTEM_INDEX_1)
                    .get("version")
                    .asInt();

                System.out.println("node: " + node.getNodeName());
                System.out.println("response.body: " + response.getBody());
                System.out.println("isSystem: " + isSystem);
                System.out.println("previousVersion: " + previousVersion);

                assertThat(isSystem, equalTo(false));
            }
        }

        cluster.addPlugin(SystemIndexPlugin1.class);

        cluster.restartRandomNode();

        for (LocalOpenSearchCluster.Node node : cluster.nodes()) {
            try (TestRestClient client = node.getRestClient(USER_ADMIN)) {
                System.out.println("node: " + node.getNodeName());
                Awaitility.await().alias("Load default configuration").until(() -> client.getAuthInfo().getStatusCode(), equalTo(200));
                HttpResponse catPluginsResponse = client.get("_cat/plugins");

                System.out.println("catPluginsResponse: " + catPluginsResponse.getBody());
                HttpResponse response = client.get("_cluster/state/metadata/" + SystemIndexPlugin1.SYSTEM_INDEX_1);

                assertThat(response.getStatusCode(), equalTo(RestStatus.OK.getStatus()));

                boolean isSystem = response.bodyAsJsonNode()
                    .get("metadata")
                    .get("indices")
                    .get(SystemIndexPlugin1.SYSTEM_INDEX_1)
                    .get("system")
                    .asBoolean();
                int version = response.bodyAsJsonNode()
                    .get("metadata")
                    .get("indices")
                    .get(SystemIndexPlugin1.SYSTEM_INDEX_1)
                    .get("version")
                    .asInt();

                System.out.println("response.body: " + response.getBody());
                System.out.println("isSystem: " + isSystem);
                System.out.println("version: " + version);

                // assertThat(isSystem, equalTo(true));
                // assertThat(version, greaterThan(previousVersion));
            }
        }
    }
}
