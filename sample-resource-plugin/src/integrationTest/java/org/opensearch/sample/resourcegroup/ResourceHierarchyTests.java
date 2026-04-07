/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sample.resourcegroup;

import java.time.Duration;

import com.carrotsearch.randomizedtesting.RandomizedRunner;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.opensearch.sample.resource.TestUtils;
import org.opensearch.test.framework.cluster.LocalCluster;
import org.opensearch.test.framework.cluster.TestRestClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.opensearch.sample.resource.TestUtils.FULL_ACCESS_USER;
import static org.opensearch.sample.resource.TestUtils.SAMPLE_GROUP_READ_ONLY;
import static org.opensearch.sample.resource.TestUtils.newCluster;
import static org.opensearch.security.api.AbstractApiIntegrationTest.forbidden;
import static org.opensearch.security.api.AbstractApiIntegrationTest.ok;
import static org.opensearch.test.framework.TestSecurityConfig.User.USER_ADMIN;

/**
 * Test resource access to a resource shared with mixed access-levels. Some users are shared at read_only, others at full_access.
 * All tests are against USER_ADMIN's resource created during setup.
 */
@RunWith(RandomizedRunner.class)
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class ResourceHierarchyTests {

    @ClassRule
    public static LocalCluster cluster = newCluster(true, true);

    private final TestUtils.ApiHelper api = new TestUtils.ApiHelper(cluster);
    private String resourceGroupId;
    private String resourceId;

    @Before
    public void setup() {
        resourceGroupId = api.createSampleResourceGroupAs(USER_ADMIN);
        api.awaitSharingEntry(resourceGroupId); // wait until sharing entry is created
        resourceId = api.createSampleResourceWithGroupAs(USER_ADMIN, resourceGroupId);
        api.awaitSharingEntry(resourceId); // wait until sharing entry is created
    }

    @After
    public void cleanup() {
        api.wipeOutResourceEntries();
    }

    @Test
    public void testShouldHaveAccessToResourceWithGroupLevelAccess() throws Exception {
        TestRestClient.HttpResponse response = ok(() -> api.getResource(resourceId, USER_ADMIN));
        assertThat(response.getBody(), containsString("sample"));

        forbidden(() -> api.getResourceGroup(resourceGroupId, FULL_ACCESS_USER));
        forbidden(() -> api.getResource(resourceGroupId, FULL_ACCESS_USER));

        ok(() -> api.shareResourceGroup(resourceGroupId, USER_ADMIN, FULL_ACCESS_USER, SAMPLE_GROUP_READ_ONLY));

        ok(() -> api.getResourceGroup(resourceGroupId, FULL_ACCESS_USER));
        ok(() -> api.getResource(resourceId, FULL_ACCESS_USER));
    }

    @Test
    public void testGroupReadOnlyShouldNotGrantWriteOnChild() throws Exception {
        ok(() -> api.shareResourceGroup(resourceGroupId, USER_ADMIN, FULL_ACCESS_USER, SAMPLE_GROUP_READ_ONLY));

        // read is allowed via parent
        ok(() -> api.getResource(resourceId, FULL_ACCESS_USER));

        // write/delete on child should still be forbidden — read_only only maps to get actions
        forbidden(() -> api.updateResource(resourceId, FULL_ACCESS_USER, "shouldFail"));
        forbidden(() -> api.deleteResource(resourceId, FULL_ACCESS_USER));
    }

    @Test
    public void testRevokingGroupAccessRemovesChildAccess() throws Exception {
        ok(() -> api.shareResourceGroup(resourceGroupId, USER_ADMIN, FULL_ACCESS_USER, SAMPLE_GROUP_READ_ONLY));

        ok(() -> api.getResource(resourceId, FULL_ACCESS_USER));

        ok(() -> api.revokeResourceGroup(resourceGroupId, USER_ADMIN, FULL_ACCESS_USER, SAMPLE_GROUP_READ_ONLY));

        forbidden(() -> api.getResourceGroup(resourceGroupId, FULL_ACCESS_USER));
        forbidden(() -> api.getResource(resourceId, FULL_ACCESS_USER));
    }

    @Test
    public void testDirectChildShareGrantsAccessWithoutGroupShare() throws Exception {
        // group is not shared with FULL_ACCESS_USER at all
        forbidden(() -> api.getResourceGroup(resourceGroupId, FULL_ACCESS_USER));
        forbidden(() -> api.getResource(resourceId, FULL_ACCESS_USER));

        // share the child directly at full_access
        ok(() -> api.shareResource(resourceId, USER_ADMIN, FULL_ACCESS_USER, TestUtils.SAMPLE_FULL_ACCESS));

        // child is now accessible
        ok(() -> api.getResource(resourceId, FULL_ACCESS_USER));
        ok(() -> api.updateResource(resourceId, FULL_ACCESS_USER, "directShareUpdate"));

        // group itself remains inaccessible
        forbidden(() -> api.getResourceGroup(resourceGroupId, FULL_ACCESS_USER));
    }

    @Test
    public void testSharingGroupCascadesToExistingChildren() throws Exception {
        // Before sharing the group, child is inaccessible to FULL_ACCESS_USER
        forbidden(() -> api.getResource(resourceId, FULL_ACCESS_USER));

        // Share the group — cascade should propagate to the child
        ok(() -> api.shareResourceGroup(resourceGroupId, USER_ADMIN, FULL_ACCESS_USER, SAMPLE_GROUP_READ_ONLY));

        // Child should now be accessible via cascaded sharing
        ok(() -> api.getResource(resourceId, FULL_ACCESS_USER));
    }

    @Test
    public void testMovingUngroupedResourceIntoSharedGroupInheritsAccess() throws Exception {
        // Create an ungrouped resource
        String ungroupedId = api.createSampleResourceAs(USER_ADMIN);
        api.awaitSharingEntry(ungroupedId);

        // Share the group with FULL_ACCESS_USER
        ok(() -> api.shareResourceGroup(resourceGroupId, USER_ADMIN, FULL_ACCESS_USER, SAMPLE_GROUP_READ_ONLY));

        // Ungrouped resource is not accessible to FULL_ACCESS_USER
        forbidden(() -> api.getResource(ungroupedId, FULL_ACCESS_USER));

        // Move the resource into the shared group
        ok(() -> api.moveResourceToGroup(ungroupedId, USER_ADMIN, resourceGroupId));

        // Wait for sharing to propagate via index listener
        Awaitility.await("Wait for ungrouped resource to inherit group sharing")
            .pollInterval(Duration.ofMillis(500))
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                ok(() -> api.getResource(ungroupedId, FULL_ACCESS_USER));
            });
    }

    @Test
    public void testMovingResourceBetweenGroupsReplacesSharing() throws Exception {
        // Create a second group and share it with a different user
        String groupB = api.createSampleResourceGroupAs(USER_ADMIN);
        api.awaitSharingEntry(groupB);

        // Share group A with FULL_ACCESS_USER, group B is not shared with anyone extra
        ok(() -> api.shareResourceGroup(resourceGroupId, USER_ADMIN, FULL_ACCESS_USER, SAMPLE_GROUP_READ_ONLY));

        // Child resource is accessible via group A
        ok(() -> api.getResource(resourceId, FULL_ACCESS_USER));

        // Move resource from group A to group B (which is not shared with FULL_ACCESS_USER)
        ok(() -> api.moveResourceToGroup(resourceId, USER_ADMIN, groupB));

        // Resource should lose group A's sharing
        Awaitility.await("Wait for resource to lose old group sharing")
            .pollInterval(Duration.ofMillis(500))
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                forbidden(() -> api.getResource(resourceId, FULL_ACCESS_USER));
            });
    }

    @Test
    public void testMovingResourceToUngroupedRevokesInheritedSharing() throws Exception {
        // Share the group with FULL_ACCESS_USER
        ok(() -> api.shareResourceGroup(resourceGroupId, USER_ADMIN, FULL_ACCESS_USER, SAMPLE_GROUP_READ_ONLY));

        // Child resource is accessible
        ok(() -> api.getResource(resourceId, FULL_ACCESS_USER));

        // Move resource to ungrouped (update without group_id)
        ok(() -> api.updateResource(resourceId, USER_ADMIN, "sample"));

        // Resource should revert to private
        Awaitility.await("Wait for resource to revert to private")
            .pollInterval(Duration.ofMillis(500))
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                forbidden(() -> api.getResource(resourceId, FULL_ACCESS_USER));
            });
    }

    @Test
    public void testSharingGrandparentCascadesToGrandchildren() throws Exception {
        // Create nested structure: grandparent group > child group > resource
        String grandparentId = api.createSampleResourceGroupAs(USER_ADMIN);
        api.awaitSharingEntry(grandparentId);
        String childGroupId = api.createNestedResourceGroupAs(USER_ADMIN, grandparentId);
        api.awaitSharingEntry(childGroupId);
        String grandchildId = api.createSampleResourceWithGroupAs(USER_ADMIN, childGroupId);
        api.awaitSharingEntry(grandchildId);

        // Nothing shared yet
        forbidden(() -> api.getResourceGroup(grandparentId, FULL_ACCESS_USER));
        forbidden(() -> api.getResourceGroup(childGroupId, FULL_ACCESS_USER));
        forbidden(() -> api.getResource(grandchildId, FULL_ACCESS_USER));

        // Share grandparent — should cascade to child group and grandchild resource
        ok(() -> api.shareResourceGroup(grandparentId, USER_ADMIN, FULL_ACCESS_USER, SAMPLE_GROUP_READ_ONLY));

        ok(() -> api.getResourceGroup(grandparentId, FULL_ACCESS_USER));
        ok(() -> api.getResourceGroup(childGroupId, FULL_ACCESS_USER));
        ok(() -> api.getResource(grandchildId, FULL_ACCESS_USER));
    }

    @Test
    public void testRevokingGrandparentCascadesToGrandchildren() throws Exception {
        // Create nested structure and share grandparent
        String grandparentId = api.createSampleResourceGroupAs(USER_ADMIN);
        api.awaitSharingEntry(grandparentId);
        String childGroupId = api.createNestedResourceGroupAs(USER_ADMIN, grandparentId);
        api.awaitSharingEntry(childGroupId);
        String grandchildId = api.createSampleResourceWithGroupAs(USER_ADMIN, childGroupId);
        api.awaitSharingEntry(grandchildId);

        ok(() -> api.shareResourceGroup(grandparentId, USER_ADMIN, FULL_ACCESS_USER, SAMPLE_GROUP_READ_ONLY));

        ok(() -> api.getResource(grandchildId, FULL_ACCESS_USER));

        // Revoke grandparent — should cascade down
        ok(() -> api.revokeResourceGroup(grandparentId, USER_ADMIN, FULL_ACCESS_USER, SAMPLE_GROUP_READ_ONLY));

        forbidden(() -> api.getResourceGroup(grandparentId, FULL_ACCESS_USER));
        forbidden(() -> api.getResourceGroup(childGroupId, FULL_ACCESS_USER));
        forbidden(() -> api.getResource(grandchildId, FULL_ACCESS_USER));
    }

    @Test
    public void testOwnerRetainsAccessAfterRevokeCascade() throws Exception {
        // Share group, then revoke — owner (admin) should still have access
        ok(() -> api.shareResourceGroup(resourceGroupId, USER_ADMIN, FULL_ACCESS_USER, SAMPLE_GROUP_READ_ONLY));
        ok(() -> api.getResource(resourceId, FULL_ACCESS_USER));

        ok(() -> api.revokeResourceGroup(resourceGroupId, USER_ADMIN, FULL_ACCESS_USER, SAMPLE_GROUP_READ_ONLY));

        // Owner always retains access
        ok(() -> api.getResourceGroup(resourceGroupId, USER_ADMIN));
        ok(() -> api.getResource(resourceId, USER_ADMIN));

        // Shared user loses access
        forbidden(() -> api.getResource(resourceId, FULL_ACCESS_USER));
    }

    @Test
    public void testCreatingResourceInSharedGroupInheritsAccess() throws Exception {
        // Share the group first
        ok(() -> api.shareResourceGroup(resourceGroupId, USER_ADMIN, FULL_ACCESS_USER, SAMPLE_GROUP_READ_ONLY));

        // Create a NEW resource in the shared group
        String newResourceId = api.createSampleResourceWithGroupAs(USER_ADMIN, resourceGroupId);
        api.awaitSharingEntry(newResourceId);

        // New resource should be accessible via parent walk-up
        ok(() -> api.getResource(newResourceId, FULL_ACCESS_USER));
    }

}
