/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sample.resource.feature.enabled;

import java.util.List;
import java.util.Map;

import com.carrotsearch.randomizedtesting.RandomizedRunner;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.opensearch.sample.SampleResourcePlugin;
import org.opensearch.sample.resource.TestUtils;
import org.opensearch.test.framework.TestSecurityConfig;
import org.opensearch.test.framework.cluster.LocalCluster;
import org.opensearch.test.framework.cluster.TestRestClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.opensearch.sample.resource.TestUtils.FULL_ACCESS_USER;
import static org.opensearch.sample.resource.TestUtils.LIMITED_ACCESS_USER;
import static org.opensearch.sample.resource.TestUtils.NO_ACCESS_USER;
import static org.opensearch.sample.resource.TestUtils.SAMPLE_FULL_ACCESS;
import static org.opensearch.sample.resource.TestUtils.SAMPLE_READ_ONLY;
import static org.opensearch.sample.resource.TestUtils.newCluster;
import static org.opensearch.sample.utils.Constants.RESOURCE_GROUP_TYPE;
import static org.opensearch.sample.utils.Constants.RESOURCE_INDEX_CONCRETE;
import static org.opensearch.sample.utils.Constants.RESOURCE_INDEX_NAME;
import static org.opensearch.sample.utils.Constants.RESOURCE_TYPE;
import static org.opensearch.security.api.AbstractApiIntegrationTest.forbidden;
import static org.opensearch.security.api.AbstractApiIntegrationTest.ok;
import static org.opensearch.test.framework.TestSecurityConfig.User.USER_ADMIN;

/**
 * Tests resource sharing when the plugin uses an alias (.sample_resource) backed by
 * a concrete index (.sample_resource_1) with a wildcard pattern (.sample_resource_*).
 * This validates the wildcard index pattern support end-to-end.
 */
@RunWith(RandomizedRunner.class)
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class AliasIndexPatternTests {

    @ClassRule
    public static LocalCluster cluster = newCluster(
        true,
        true,
        List.of(RESOURCE_TYPE, RESOURCE_GROUP_TYPE),
        Map.of(SampleResourcePlugin.ALIAS_MODE_SETTING_KEY, true)
    );

    private static final TestUtils.ApiHelper staticApi = new TestUtils.ApiHelper(cluster);
    private final TestUtils.ApiHelper api = new TestUtils.ApiHelper(cluster);
    private String resourceId;

    @BeforeClass
    public static void setupAlias() {
        // Create concrete index with alias, mimicking the .kibana pattern
        staticApi.createIndexWithAlias(RESOURCE_INDEX_CONCRETE, RESOURCE_INDEX_NAME);
    }

    @Before
    public void setup() {
        resourceId = api.createSampleResourceAs(USER_ADMIN);
        api.awaitSharingEntry(resourceId);
    }

    @After
    public void cleanup() {
        api.wipeOutResourceEntries();
    }

    // --- Helper methods mirroring normal-mode test patterns ---

    private void assertNoAccess(TestSecurityConfig.User user) throws Exception {
        forbidden(() -> api.getResource(resourceId, user));
        forbidden(() -> api.updateResource(resourceId, user, "nope"));
        forbidden(() -> api.deleteResource(resourceId, user));
        forbidden(() -> api.shareResource(resourceId, user, user, SAMPLE_FULL_ACCESS));
        forbidden(() -> api.revokeResource(resourceId, user, user, SAMPLE_FULL_ACCESS));
    }

    private void assertReadOnly(TestSecurityConfig.User user) throws Exception {
        TestRestClient.HttpResponse response = ok(() -> api.getResource(resourceId, user));
        assertThat(response.getBody(), containsString("sample"));
        forbidden(() -> api.updateResource(resourceId, user, "nope"));
        forbidden(() -> api.deleteResource(resourceId, user));
        forbidden(() -> api.shareResource(resourceId, user, user, SAMPLE_FULL_ACCESS));
        forbidden(() -> api.revokeResource(resourceId, user, user, SAMPLE_FULL_ACCESS));
    }

    private void assertFullAccess(TestSecurityConfig.User user) throws Exception {
        TestRestClient.HttpResponse response = ok(() -> api.getResource(resourceId, user));
        assertThat(response.getBody(), containsString("sample"));
        ok(() -> api.updateResource(resourceId, user, "updated"));
        ok(() -> api.shareResource(resourceId, user, user, SAMPLE_FULL_ACCESS));
        ok(() -> api.revokeResource(resourceId, user, USER_ADMIN, SAMPLE_FULL_ACCESS));
        ok(() -> api.deleteResource(resourceId, user));
    }

    // --- Owner CRUD ---

    @Test
    public void owner_canCRUD() throws Exception {
        TestRestClient.HttpResponse response = ok(() -> api.getResource(resourceId, USER_ADMIN));
        assertThat(response.getBody(), containsString("sample"));
        ok(() -> api.updateResource(resourceId, USER_ADMIN, "updated"));
        ok(() -> api.deleteResource(resourceId, USER_ADMIN));
    }

    // --- Full access sharing ---

    @Test
    public void fullAccess_sharing_and_revoke() throws Exception {
        assertNoAccess(FULL_ACCESS_USER);

        ok(() -> api.shareResource(resourceId, USER_ADMIN, FULL_ACCESS_USER, SAMPLE_FULL_ACCESS));

        // Shared user can share transitively
        ok(() -> api.shareResource(resourceId, FULL_ACCESS_USER, LIMITED_ACCESS_USER, SAMPLE_FULL_ACCESS));

        // Revoke and verify
        ok(() -> api.revokeResource(resourceId, USER_ADMIN, FULL_ACCESS_USER, SAMPLE_FULL_ACCESS));
        forbidden(() -> api.getResource(resourceId, FULL_ACCESS_USER));
    }

    @Test
    public void fullAccess_user_canCRUD() throws Exception {
        assertNoAccess(FULL_ACCESS_USER);
        ok(() -> api.shareResource(resourceId, USER_ADMIN, FULL_ACCESS_USER, SAMPLE_FULL_ACCESS));
        assertFullAccess(FULL_ACCESS_USER);
    }

    // --- Read-only access ---

    @Test
    public void readOnly_fullAccessUser() throws Exception {
        assertNoAccess(FULL_ACCESS_USER);
        ok(() -> api.shareResource(resourceId, USER_ADMIN, FULL_ACCESS_USER, SAMPLE_READ_ONLY));
        assertReadOnly(FULL_ACCESS_USER);
    }

    @Test
    public void readOnly_limitedAccessUser() throws Exception {
        assertNoAccess(LIMITED_ACCESS_USER);
        ok(() -> api.shareResource(resourceId, USER_ADMIN, LIMITED_ACCESS_USER, SAMPLE_READ_ONLY));
        assertReadOnly(LIMITED_ACCESS_USER);
    }

    @Test
    public void readOnly_noAccessUser() throws Exception {
        assertNoAccess(NO_ACCESS_USER);
        ok(() -> api.shareResource(resourceId, USER_ADMIN, NO_ACCESS_USER, SAMPLE_READ_ONLY));
        assertReadOnly(NO_ACCESS_USER);
    }

    // --- Mixed access levels ---

    @Test
    public void mixed_readOnly_then_upgraded_to_fullAccess() throws Exception {
        assertNoAccess(LIMITED_ACCESS_USER);

        // Start with read-only
        ok(() -> api.shareResource(resourceId, USER_ADMIN, LIMITED_ACCESS_USER, SAMPLE_READ_ONLY));
        assertReadOnly(LIMITED_ACCESS_USER);

        // Upgrade to full access
        ok(() -> api.shareResource(resourceId, USER_ADMIN, LIMITED_ACCESS_USER, SAMPLE_FULL_ACCESS));
        assertFullAccess(LIMITED_ACCESS_USER);
    }

    @Test
    public void mixed_multipleUsers_differentLevels() throws Exception {
        assertNoAccess(FULL_ACCESS_USER);
        assertNoAccess(LIMITED_ACCESS_USER);

        // Share read-only to one, full-access to another
        ok(() -> api.shareResource(resourceId, USER_ADMIN, FULL_ACCESS_USER, SAMPLE_READ_ONLY));
        ok(() -> api.shareResource(resourceId, USER_ADMIN, LIMITED_ACCESS_USER, SAMPLE_FULL_ACCESS));

        assertReadOnly(FULL_ACCESS_USER);

        // Full-access user upgrades read-only user
        ok(() -> api.shareResource(resourceId, LIMITED_ACCESS_USER, FULL_ACCESS_USER, SAMPLE_FULL_ACCESS));
        assertFullAccess(FULL_ACCESS_USER);
    }

    // --- DLS filtering through alias ---

    @Test
    public void dls_owner_sees_own_resource_via_plugin_search() throws Exception {
        // Owner should see their resource via the plugin search API
        TestRestClient.HttpResponse response = ok(() -> api.searchResources(USER_ADMIN));
        assertThat(response.getBody(), containsString("sample"));
    }

    @Test
    public void dls_nonShared_user_sees_nothing_via_plugin_search() throws Exception {
        // Non-shared user should see no resources
        TestRestClient.HttpResponse response = ok(() -> api.searchResources(FULL_ACCESS_USER));
        assertThat(response.getBody(), containsString("\"hits\":{\"total\":{\"value\":0"));
    }
}
