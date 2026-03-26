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

package org.opensearch.security.api;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.ClassRule;
import org.junit.Test;

import org.opensearch.test.framework.TestSecurityConfig;
import org.opensearch.test.framework.TestSecurityConfig.Role;
import org.opensearch.test.framework.cluster.LocalCluster;
import org.opensearch.test.framework.cluster.TestRestClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.opensearch.security.OpenSearchSecurityPlugin.PLUGINS_PREFIX;
import static org.opensearch.test.framework.matcher.RestMatchers.isOk;

public class ApplicationPermissionsInfoTest extends AbstractApiIntegrationTest {

    private static final String ENDPOINT = PLUGINS_PREFIX + "/applicationpermissions";

    static final Role ISM_READ_ROLE = new Role("ism_read_access").applicationId("index-management")
        .clusterPermissions("cluster:admin/opendistro/ism/get");

    static final Role ISM_FULL_ROLE = new Role("ism_full_access").applicationId("index-management")
        .clusterPermissions("cluster:admin/opendistro/ism/*");

    static final Role ALERTING_READ_ROLE = new Role("alerting_read_access").applicationId("alerting")
        .clusterPermissions("cluster:admin/opendistro/alerting/get");

    static final Role NO_APP_ROLE = new Role("custom_role_no_app").clusterPermissions("cluster:monitor/health");

    static final TestSecurityConfig.User ISM_USER = new TestSecurityConfig.User("ism_user").roles(ISM_READ_ROLE);

    static final TestSecurityConfig.User MULTI_APP_USER = new TestSecurityConfig.User("multi_app_user").roles(
        ISM_FULL_ROLE,
        ALERTING_READ_ROLE
    );

    static final TestSecurityConfig.User NO_APP_USER = new TestSecurityConfig.User("no_app_user").roles(NO_APP_ROLE);

    @ClassRule
    public static LocalCluster localCluster = clusterBuilder().users(ISM_USER, MULTI_APP_USER, NO_APP_USER)
        .roles(ISM_READ_ROLE, ISM_FULL_ROLE, ALERTING_READ_ROLE, NO_APP_ROLE)
        .build();

    @Test
    public void allAccessUserGetsWildcard() throws Exception {
        try (TestRestClient client = localCluster.getRestClient(ADMIN_USER)) {
            TestRestClient.HttpResponse response = client.get(ENDPOINT);
            assertThat(response, isOk());

            JsonNode appIds = response.bodyAsJsonNode().get("application_ids");
            assertThat(appIds.size(), equalTo(1));
            assertThat(appIds.get(0).asText(), equalTo("*"));
        }
    }

    @Test
    public void userWithSingleAppRole() throws Exception {
        try (TestRestClient client = localCluster.getRestClient(ISM_USER)) {
            TestRestClient.HttpResponse response = client.get(ENDPOINT);
            assertThat(response, isOk());

            JsonNode appIds = response.bodyAsJsonNode().get("application_ids");
            List<String> ids = new java.util.ArrayList<>();
            appIds.forEach(n -> ids.add(n.asText()));
            assertThat(ids, containsInAnyOrder("index-management"));
        }
    }

    @Test
    public void userWithMultipleAppRoles() throws Exception {
        try (TestRestClient client = localCluster.getRestClient(MULTI_APP_USER)) {
            TestRestClient.HttpResponse response = client.get(ENDPOINT);
            assertThat(response, isOk());

            JsonNode appIds = response.bodyAsJsonNode().get("application_ids");
            List<String> ids = new java.util.ArrayList<>();
            appIds.forEach(n -> ids.add(n.asText()));
            assertThat(ids, containsInAnyOrder("index-management", "alerting"));
        }
    }

    @Test
    public void userWithNoAppRolesGetsEmptyList() throws Exception {
        try (TestRestClient client = localCluster.getRestClient(NO_APP_USER)) {
            TestRestClient.HttpResponse response = client.get(ENDPOINT);
            assertThat(response, isOk());

            JsonNode appIds = response.bodyAsJsonNode().get("application_ids");
            assertThat(appIds.size(), equalTo(0));
        }
    }

    @Test
    public void responseIncludesUserName() throws Exception {
        try (TestRestClient client = localCluster.getRestClient(ISM_USER)) {
            TestRestClient.HttpResponse response = client.get(ENDPOINT);
            assertThat(response, isOk());
            assertThat(response.bodyAsJsonNode().get("user_name").asText(), equalTo("ism_user"));
        }
    }
}
