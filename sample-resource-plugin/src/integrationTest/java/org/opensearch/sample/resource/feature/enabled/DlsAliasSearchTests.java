/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sample.resource.feature.enabled;

import com.carrotsearch.randomizedtesting.RandomizedRunner;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.opensearch.sample.resource.TestUtils;
import org.opensearch.test.framework.cluster.LocalCluster;
import org.opensearch.test.framework.cluster.TestRestClient;

import static org.opensearch.sample.resource.TestUtils.FULL_ACCESS_USER;
import static org.opensearch.sample.resource.TestUtils.newCluster;
import static org.opensearch.sample.utils.Constants.RESOURCE_INDEX_NAME;
import static org.opensearch.test.framework.TestSecurityConfig.User.USER_ADMIN;

/**
 * Tests that DLS (Document Level Security) is correctly applied when searching
 * a resource index via an alias through the PluginClient.
 *
 * Regression test for: DLS filter not applied when alias name differs from concrete index name.
 * The DLS restriction map must include both concrete index names and alias names so that
 * DlsFilterLevelActionHandler.modifyQuery() can find the restriction regardless of which
 * name is used for iteration.
 */
@RunWith(RandomizedRunner.class)
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class DlsAliasSearchTests {

    private static final String RESOURCE_INDEX_ALIAS = ".sample_resource_alias";

    @ClassRule
    public static LocalCluster cluster = newCluster(true, true);

    private final TestUtils.ApiHelper api = new TestUtils.ApiHelper(cluster);
    private String adminResourceId;
    private String userResourceId;

    @Before
    public void setup() {
        // Create resources as two different users
        adminResourceId = api.createSampleResourceAs(USER_ADMIN);
        api.awaitSharingEntry(adminResourceId);

        userResourceId = api.createSampleResourceAs(FULL_ACCESS_USER);
        api.awaitSharingEntry(userResourceId, FULL_ACCESS_USER.getName());

        // Create an alias for the resource index
        try (TestRestClient client = cluster.getRestClient(cluster.getAdminCertificate())) {
            String aliasPayload = """
                {
                  "actions": [
                    { "add": { "index": "%s", "alias": "%s" } }
                  ]
                }
                """.formatted(RESOURCE_INDEX_NAME, RESOURCE_INDEX_ALIAS);
            TestRestClient.HttpResponse response = client.postJson("_aliases", aliasPayload);
            response.assertStatusCode(HttpStatus.SC_OK);
        }
    }

    @After
    public void cleanup() {
        // Remove alias
        try (TestRestClient client = cluster.getRestClient(cluster.getAdminCertificate())) {
            String aliasPayload = """
                {
                  "actions": [
                    { "remove": { "index": "%s", "alias": "%s" } }
                  ]
                }
                """.formatted(RESOURCE_INDEX_NAME, RESOURCE_INDEX_ALIAS);
            client.postJson("_aliases", aliasPayload);
        }
        api.wipeOutResourceEntries();
    }

    @Test
    public void searchViaAlias_shouldApplyDls_userSeesOnlyOwnResources() {
        // Search via concrete index name — DLS should filter to only user's own resources
        TestRestClient.HttpResponse directResponse = api.searchResources(FULL_ACCESS_USER);
        directResponse.assertStatusCode(HttpStatus.SC_OK);
        int directHits = getHitCount(directResponse);

        // Search via alias — DLS should also be applied, returning the same results
        TestRestClient.HttpResponse aliasResponse = api.searchResourcesByAlias(RESOURCE_INDEX_ALIAS, FULL_ACCESS_USER);
        aliasResponse.assertStatusCode(HttpStatus.SC_OK);
        int aliasHits = getHitCount(aliasResponse);

        // Both searches should return the same number of hits (DLS applied in both cases)
        assert directHits == aliasHits : "DLS not applied when searching via alias: direct=" + directHits + " alias=" + aliasHits;
    }

    @SuppressWarnings("unchecked")
    private int getHitCount(TestRestClient.HttpResponse response) {
        java.util.Map<String, Object> hits = (java.util.Map<String, Object>) response.bodyAsMap().get("hits");
        return ((java.util.List<?>) hits.get("hits")).size();
    }
}
