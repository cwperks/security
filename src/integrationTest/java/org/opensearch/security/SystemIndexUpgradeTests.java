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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.opensearch.core.rest.RestStatus;
import org.opensearch.security.plugin.SystemIndexPlugin1;
import org.opensearch.test.framework.TestSecurityConfig.AuthcDomain;
import org.opensearch.test.framework.cluster.ClusterManager;
import org.opensearch.test.framework.cluster.LocalCluster;
import org.opensearch.test.framework.cluster.TestRestClient;
import org.opensearch.test.framework.cluster.TestRestClient.HttpResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.opensearch.security.support.ConfigConstants.SECURITY_ALLOW_DEFAULT_INIT_SECURITYINDEX;
import static org.opensearch.security.support.ConfigConstants.SECURITY_RESTAPI_ROLES_ENABLED;
import static org.opensearch.security.support.ConfigConstants.SECURITY_SYSTEM_INDICES_ENABLED_KEY;
import static org.opensearch.test.framework.TestSecurityConfig.Role.ALL_ACCESS;
import static org.opensearch.test.framework.TestSecurityConfig.User.USER_ADMIN;

@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class SystemIndexUpgradeTests {

    public static final AuthcDomain AUTHC_DOMAIN = new AuthcDomain("basic", 0).httpAuthenticatorWithChallenge("basic").backend("internal");

    @ClassRule
    public static final LocalCluster cluster = new LocalCluster.Builder().clusterManager(ClusterManager.SINGLENODE)
        .anonymousAuth(false)
        .authc(AUTHC_DOMAIN)
        .users(USER_ADMIN)
        .nodeSettings(
            Map.of(
                SECURITY_RESTAPI_ROLES_ENABLED,
                List.of("user_" + USER_ADMIN.getName() + "__" + ALL_ACCESS.getName()),
                SECURITY_SYSTEM_INDICES_ENABLED_KEY,
                true,
                SECURITY_ALLOW_DEFAULT_INIT_SECURITYINDEX,
                true
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
        try (TestRestClient client = cluster.getRestClient(USER_ADMIN)) {
            client.put(SystemIndexPlugin1.SYSTEM_INDEX_1);
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
        }

        Thread.sleep(2000);

        cluster.stop();

        Thread.sleep(2000);

        cluster.addPlugin(SystemIndexPlugin1.class);

        cluster.start();

        try (TestRestClient client = cluster.getRestClient(USER_ADMIN)) {
            HttpResponse catResponse = client.get("_cat/indices");
            System.out.println("catResponse.body: " + catResponse.getBody());
            HttpResponse putResponse = client.put(SystemIndexPlugin1.SYSTEM_INDEX_1);
            System.out.println("putResponse.body: " + putResponse.getBody());
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

            assertThat(isSystem, equalTo(true));
        }
    }
}
