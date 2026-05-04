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

import org.junit.ClassRule;
import org.junit.Test;

import org.opensearch.core.rest.RestStatus;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.test.framework.TestSecurityConfig.AuthcDomain;
import org.opensearch.test.framework.TestSecurityConfig.User;
import org.opensearch.test.framework.cluster.ClusterManager;
import org.opensearch.test.framework.cluster.LocalCluster;
import org.opensearch.test.framework.cluster.TestRestClient;
import org.opensearch.test.framework.cluster.TestRestClient.HttpResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.opensearch.security.support.ConfigConstants.SECURITY_RESTAPI_ROLES_ENABLED;
import static org.opensearch.test.framework.TestSecurityConfig.Role.ALL_ACCESS;

public class StandbyModeTests {

    public static final AuthcDomain AUTHC_DOMAIN = new AuthcDomain("basic", 0).httpAuthenticatorWithChallenge("basic").backend("internal");

    private static final User ADMIN_USER = new User("admin").roles(ALL_ACCESS);

    @ClassRule
    public static final LocalCluster cluster = new LocalCluster.Builder().clusterManager(ClusterManager.SINGLENODE)
        .anonymousAuth(false)
        .authc(AUTHC_DOMAIN)
        .users(ADMIN_USER)
        .nodeSettings(
            Map.of(
                SECURITY_RESTAPI_ROLES_ENABLED,
                List.of("user_" + ADMIN_USER.getName() + "__" + ALL_ACCESS.getName()),
                ConfigConstants.SECURITY_STANDBY_MODE,
                true
            )
        )
        .build();

    @Test
    public void testAuthenticationWorksInStandbyMode() {
        try (TestRestClient client = cluster.getRestClient(ADMIN_USER)) {
            HttpResponse response = client.get("_cluster/health");
            assertThat(response.getStatusCode(), equalTo(RestStatus.OK.getStatus()));
        }
    }

    @Test
    public void testSecurityConfigMutationBlockedInStandbyMode() {
        try (TestRestClient client = cluster.getRestClient(ADMIN_USER)) {
            // Try to create a role — should be blocked
            HttpResponse response = client.putJson(
                "_plugins/_security/api/roles/test_role",
                "{\"cluster_permissions\": [\"cluster_monitor\"]}"
            );
            assertThat(response.getStatusCode(), equalTo(RestStatus.FORBIDDEN.getStatus()));
            assertThat(response.getBody(), containsString("standby mode"));
        }
    }

    @Test
    public void testSecurityConfigReadAllowedInStandbyMode() {
        try (TestRestClient client = cluster.getRestClient(ADMIN_USER)) {
            // Reading roles should still work
            HttpResponse response = client.get("_plugins/_security/api/roles");
            assertThat(response.getStatusCode(), equalTo(RestStatus.OK.getStatus()));
        }
    }

    @Test
    public void testInternalUsersApiBlockedInStandbyMode() {
        try (TestRestClient client = cluster.getRestClient(ADMIN_USER)) {
            HttpResponse response = client.putJson(
                "_plugins/_security/api/internalusers/new_user",
                "{\"password\": \"testPassword123!\", \"backend_roles\": [\"admin\"]}"
            );
            assertThat(response.getStatusCode(), equalTo(RestStatus.FORBIDDEN.getStatus()));
            assertThat(response.getBody(), containsString("standby mode"));
        }
    }
}
