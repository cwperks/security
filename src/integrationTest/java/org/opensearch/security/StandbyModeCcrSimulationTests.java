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

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.support.WriteRequest.RefreshPolicy;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.security.securityconf.impl.CType;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.test.framework.TestSecurityConfig.Role;
import org.opensearch.test.framework.TestSecurityConfig.User;
import org.opensearch.test.framework.cluster.ClusterManager;
import org.opensearch.test.framework.cluster.ContextHeaderDecoratorClient;
import org.opensearch.test.framework.cluster.LocalCluster;
import org.opensearch.test.framework.cluster.TestRestClient;
import org.opensearch.test.framework.cluster.TestRestClient.HttpResponse;
import org.opensearch.transport.client.Client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.opensearch.security.support.ConfigConstants.SECURITY_RESTAPI_ROLES_ENABLED;
import static org.opensearch.test.framework.TestSecurityConfig.AuthcDomain.AUTHC_HTTPBASIC_INTERNAL;
import static org.opensearch.test.framework.TestSecurityConfig.Role.ALL_ACCESS;

/**
 * Tests standby mode behavior simulating a CCR follower cluster.
 *
 * The "leader" cluster has a normal security configuration.
 * The "standby" cluster starts with standby_mode=true and loadConfigurationIntoIndex=false,
 * meaning it does NOT bootstrap its own security index. Instead, we manually write security
 * config to its security index (simulating what CCR replication would do), and verify that:
 *
 * 1. The standby cluster picks up the replicated config via polling
 * 2. Authentication works using the replicated config
 * 3. Config mutation APIs are blocked
 */
public class StandbyModeCcrSimulationTests {

    private static final User ADMIN_USER = new User("admin").roles(ALL_ACCESS);

    private static final Role READONLY_ROLE = new Role("readonly_role").clusterPermissions("cluster_monitor")
        .indexPermissions("indices:data/read/*")
        .on("*");

    private static final User READONLY_USER = new User("readonly_user").roles(READONLY_ROLE);

    /**
     * Leader cluster — normal active cluster with security config
     */
    @ClassRule
    public static final LocalCluster leaderCluster = new LocalCluster.Builder().clusterManager(ClusterManager.SINGLENODE)
        .anonymousAuth(false)
        .clusterName("leader")
        .authc(AUTHC_HTTPBASIC_INTERNAL)
        .users(ADMIN_USER, READONLY_USER)
        .roles(READONLY_ROLE)
        .nodeSettings(Map.of(SECURITY_RESTAPI_ROLES_ENABLED, List.of("user_" + ADMIN_USER.getName() + "__" + ALL_ACCESS.getName())))
        .build();

    /**
     * Standby cluster — starts in standby mode with NO security index.
     * Security config will be "replicated" (manually written) from the leader.
     */
    @ClassRule
    public static final LocalCluster standbyCluster = new LocalCluster.Builder().clusterManager(ClusterManager.SINGLENODE)
        .anonymousAuth(false)
        .clusterName("standby")
        .authc(AUTHC_HTTPBASIC_INTERNAL)
        .users(ADMIN_USER, READONLY_USER)
        .roles(READONLY_ROLE)
        .loadConfigurationIntoIndex(false)
        .nodeSettings(
            Map.of(
                SECURITY_RESTAPI_ROLES_ENABLED,
                List.of("user_" + ADMIN_USER.getName() + "__" + ALL_ACCESS.getName()),
                ConfigConstants.SECURITY_STANDBY_MODE,
                true,
                ConfigConstants.SECURITY_ALLOW_DEFAULT_INIT_SECURITYINDEX,
                false
            )
        )
        .build();

    /**
     * Simulate CCR replication: copy security config from leader to standby.
     * In a real setup, CCR would replicate the .opendistro_security index.
     * Here we manually create the index and write the config documents.
     */
    @BeforeClass
    public static void simulateCcrReplication() throws Exception {
        // Read config from leader and write it to standby's security index
        try (
            Client standbyClient = new ContextHeaderDecoratorClient(
                standbyCluster.getInternalNodeClient(),
                Map.of(ConfigConstants.OPENDISTRO_SECURITY_CONF_REQUEST_HEADER, "true")
            )
        ) {
            // Create the security index on standby (CCR would do this)
            standbyClient.admin()
                .indices()
                .create(
                    new CreateIndexRequest(ConfigConstants.OPENDISTRO_SECURITY_DEFAULT_CONFIG_INDEX).settings(
                        Map.of("index.number_of_shards", 1, "index.auto_expand_replicas", "0-all", "index.hidden", true)
                    )
                )
                .actionGet();

            // Copy each config type from leader to standby
            try (
                Client leaderClient = new ContextHeaderDecoratorClient(
                    leaderCluster.getInternalNodeClient(),
                    Map.of(ConfigConstants.OPENDISTRO_SECURITY_CONF_REQUEST_HEADER, "true")
                )
            ) {
                for (String configType : CType.lcStringValues()) {
                    var getResponse = leaderClient.prepareGet(ConfigConstants.OPENDISTRO_SECURITY_DEFAULT_CONFIG_INDEX, configType).get();

                    if (getResponse.isExists()) {
                        standbyClient.prepareIndex(ConfigConstants.OPENDISTRO_SECURITY_DEFAULT_CONFIG_INDEX)
                            .setId(configType)
                            .setSource(getResponse.getSourceAsMap())
                            .setRefreshPolicy(RefreshPolicy.IMMEDIATE)
                            .get();
                    }
                }
            }
        }

        // Wait for the standby's config polling to pick up the replicated config
        Thread.sleep(10_000);
    }

    @Test
    public void testAuthenticationWorksOnStandbyWithReplicatedConfig() {
        try (TestRestClient client = standbyCluster.getRestClient(ADMIN_USER)) {
            HttpResponse response = client.get("_cluster/health");
            assertThat(response.getStatusCode(), equalTo(RestStatus.OK.getStatus()));
        }
    }

    @Test
    public void testConfigMutationBlockedOnStandby() {
        try (TestRestClient client = standbyCluster.getRestClient(ADMIN_USER)) {
            HttpResponse response = client.putJson(
                "_plugins/_security/api/roles/new_role",
                "{\"cluster_permissions\": [\"cluster_monitor\"]}"
            );
            assertThat(response.getStatusCode(), equalTo(RestStatus.FORBIDDEN.getStatus()));
            assertThat(response.getBody(), containsString("standby mode"));
        }
    }

    @Test
    public void testConfigReadAllowedOnStandby() {
        try (TestRestClient client = standbyCluster.getRestClient(ADMIN_USER)) {
            HttpResponse response = client.get("_plugins/_security/api/roles");
            assertThat(response.getStatusCode(), equalTo(RestStatus.OK.getStatus()));
        }
    }

    @Test
    public void testLeaderClusterOperatesNormally() {
        try (TestRestClient client = leaderCluster.getRestClient(ADMIN_USER)) {
            // Leader should allow config mutations
            HttpResponse response = client.putJson(
                "_plugins/_security/api/roles/leader_test_role",
                "{\"cluster_permissions\": [\"cluster_monitor\"]}"
            );
            assertThat(response.getStatusCode(), equalTo(RestStatus.CREATED.getStatus()));

            // Clean up
            client.delete("_plugins/_security/api/roles/leader_test_role");
        }
    }
}
