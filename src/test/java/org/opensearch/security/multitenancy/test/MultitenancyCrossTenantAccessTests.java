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

package org.opensearch.security.multitenancy.test;

import org.apache.http.HttpStatus;
import org.apache.http.message.BasicHeader;
import org.junit.Test;

import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.support.WriteRequest.RefreshPolicy;
import org.opensearch.client.Client;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.test.DynamicSecurityConfig;
import org.opensearch.security.test.SingleClusterTest;
import org.opensearch.security.test.helper.rest.RestHelper;
import org.opensearch.security.test.helper.rest.RestHelper.HttpResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Tests that multi-document actions (_mget, _msearch, _bulk) cannot be used to
 * access another tenant's dashboards index directly.
 */
public class MultitenancyCrossTenantAccessTests extends SingleClusterTest {

    @Override
    protected String getResourceFolder() {
        return "multitenancy";
    }

    @Test
    public void testMgetCannotAccessOtherTenantIndex() throws Exception {
        final Settings settings = Settings.builder().put(ConfigConstants.SECURITY_ROLES_MAPPING_RESOLUTION, "BOTH").build();

        setup(Settings.EMPTY, new DynamicSecurityConfig(), settings);
        final RestHelper rh = nonSslRestHelper();

        // Create a tenant index for user_a and index a document
        try (Client tc = getClient()) {
            String tenantIndex = ".kibana_" + "user_a".hashCode() + "_usera";
            tc.index(
                new IndexRequest(tenantIndex).id("dashboard:secret")
                    .setRefreshPolicy(RefreshPolicy.IMMEDIATE)
                    .source("{\"type\":\"dashboard\",\"dashboard\":{\"title\":\"Secret Dashboard\"}}", XContentType.JSON)
            ).actionGet();
        }

        String tenantIndex = ".kibana_" + "user_a".hashCode() + "_usera";

        // user_b tries to _mget from user_a's tenant index directly
        String mgetBody = String.format("{\"docs\":[{\"_index\":\"%s\",\"_id\":\"dashboard:secret\"}]}", tenantIndex);
        HttpResponse response = rh.executePostRequest(
            "_mget",
            mgetBody,
            encodeBasicHeader("user_b", "user_b"),
            new BasicHeader("securitytenant", "__user__")
        );
        // Should be denied - either 403 or 200 with found=false
        if (response.getStatusCode() == HttpStatus.SC_OK) {
            assertThat(response.getBody(), not(containsString("\"found\":true")));
        }

        // user_b tries to _msearch across user_a's tenant index directly
        String msearchBody = "{\"index\":\"" + tenantIndex + "\"}\n" + "{\"query\":{\"match_all\":{}}}\n";
        response = rh.executePostRequest(
            "_msearch",
            msearchBody,
            encodeBasicHeader("user_b", "user_b"),
            new BasicHeader("securitytenant", "__user__")
        );
        // Should be denied
        if (response.getStatusCode() == HttpStatus.SC_OK) {
            assertThat(response.getBody(), not(containsString("Secret Dashboard")));
        }

        // user_b tries to _bulk write to user_a's tenant index directly
        String bulkBody = "{\"index\":{\"_index\":\""
            + tenantIndex
            + "\",\"_id\":\"dashboard:injected\"}}\n"
            + "{\"type\":\"dashboard\",\"dashboard\":{\"title\":\"Injected\"}}\n";
        response = rh.executePostRequest(
            "_bulk",
            bulkBody,
            encodeBasicHeader("user_b", "user_b"),
            new BasicHeader("securitytenant", "__user__")
        );
        // Should be denied
        if (response.getStatusCode() == HttpStatus.SC_OK) {
            assertThat(response.getBody(), not(containsString("\"result\":\"created\"")));
        }

        // Verify user_a can still access their own tenant normally via the alias
        response = rh.executeGetRequest(
            ".kibana/_search",
            encodeBasicHeader("user_a", "user_a"),
            new BasicHeader("securitytenant", "__user__")
        );
        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));
    }

    @Test
    public void testMgetDeniedWhenTargetingOtherTenantIndex() throws Exception {
        final Settings settings = Settings.builder().put(ConfigConstants.SECURITY_ROLES_MAPPING_RESOLUTION, "BOTH").build();

        setup(Settings.EMPTY, new DynamicSecurityConfig(), settings);
        final RestHelper rh = nonSslRestHelper();

        // Create victim's tenant index
        try (Client tc = getClient()) {
            String victimTenantIndex = ".kibana_" + "user_a".hashCode() + "_usera";
            tc.index(
                new IndexRequest(victimTenantIndex).id("search:private-query")
                    .setRefreshPolicy(RefreshPolicy.IMMEDIATE)
                    .source("{\"type\":\"search\",\"search\":{\"title\":\"Private Query\"}}", XContentType.JSON)
            ).actionGet();
        }

        String victimTenantIndex = ".kibana_" + "user_a".hashCode() + "_usera";

        // Attacker (user_b) with securitytenant header pointing to victim
        String mgetBody = String.format("{\"docs\":[{\"_index\":\"%s\",\"_id\":\"search:private-query\"}]}", victimTenantIndex);
        HttpResponse response = rh.executePostRequest(
            "_mget",
            mgetBody,
            encodeBasicHeader("user_b", "user_b"),
            new BasicHeader("securitytenant", "user_a")
        );

        // Must NOT return the document
        assertThat(
            "Cross-tenant _mget should not return documents from another tenant",
            response.getBody(),
            not(containsString("Private Query"))
        );
    }
}
