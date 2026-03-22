/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sample.resource.securityapis;

import java.util.List;
import java.util.Map;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.opensearch.sample.resource.TestUtils;
import org.opensearch.test.framework.cluster.LocalCluster;
import org.opensearch.test.framework.cluster.TestRestClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.opensearch.sample.resource.TestUtils.RESOURCE_SHARING_INDEX;
import static org.opensearch.sample.resource.TestUtils.SAMPLE_FULL_ACCESS;
import static org.opensearch.sample.resource.TestUtils.SAMPLE_GROUP_FULL_ACCESS;
import static org.opensearch.sample.resource.TestUtils.SECURITY_LIST_ENDPOINT;
import static org.opensearch.sample.utils.Constants.RESOURCE_INDEX_NAME;
import static org.opensearch.sample.utils.Constants.RESOURCE_TYPE;
import static org.opensearch.test.framework.TestSecurityConfig.User.USER_ADMIN;

/**
 * Tests that a user with access to a parent resource group can list/search child resources
 * they were not directly shared with — i.e. parent-inherited access via the sharing index.
 */
@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class ParentHierarchyAccessTests {

    @ClassRule
    public static final LocalCluster cluster = TestUtils.newCluster(true, true);

    private final TestUtils.ApiHelper api = new TestUtils.ApiHelper(cluster);

    @After
    public void clearIndices() {
        try (TestRestClient client = cluster.getRestClient(cluster.getAdminCertificate())) {
            client.delete(RESOURCE_INDEX_NAME);
            client.delete(RESOURCE_SHARING_INDEX);
        }
    }

    /**
     * Scenario:
     *  1. Admin creates a resource group and a child resource that references it (group_id = groupId)
     *  2. Admin shares the resource group with NO_ACCESS_USER at full access
     *  3. NO_ACCESS_USER was never directly shared the child resource
     *  4. Calling list API for "sample-resource" should still return the child resource
     *     because the user has access to its parent group
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testListChildResources_viaParentGroupAccess() {
        // 1. Create a resource group and a child resource
        String groupId = api.createSampleResourceGroupAs(USER_ADMIN);
        api.awaitSharingEntry(groupId);

        String childId = api.createSampleResourceWithGroupAs(USER_ADMIN, groupId);
        api.awaitSharingEntry(childId);

        // 2. Share the group with NO_ACCESS_USER — child is NOT directly shared
        TestRestClient.HttpResponse shareResp = api.shareResourceGroup(
            groupId,
            USER_ADMIN,
            TestUtils.NO_ACCESS_USER,
            SAMPLE_GROUP_FULL_ACCESS
        );
        shareResp.assertStatusCode(HttpStatus.SC_OK);

        // 3. NO_ACCESS_USER lists child resources — should see the child via parent inheritance
        try (TestRestClient client = cluster.getRestClient(TestUtils.NO_ACCESS_USER)) {
            TestRestClient.HttpResponse response = client.get(SECURITY_LIST_ENDPOINT + "?resource_type=" + RESOURCE_TYPE);
            response.assertStatusCode(HttpStatus.SC_OK);

            List<Object> resources = (List<Object>) response.bodyAsMap().get("resources");
            assertThat("Expected 1 child resource via parent inheritance", resources.size(), equalTo(1));

            Map<String, Object> resource = (Map<String, Object>) resources.getFirst();
            assertThat(resource.get("resource_id"), equalTo(childId));
        }
    }

    /**
     * Scenario:
     *  1. Admin creates a resource group and two child resources referencing it
     *  2. Admin directly shares one child with NO_ACCESS_USER
     *  3. Admin shares the group with NO_ACCESS_USER
     *  4. List API should return both children (union of direct + inherited)
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testListChildResources_unionOfDirectAndInherited() {
        // 1. Create group and two children
        String groupId = api.createSampleResourceGroupAs(USER_ADMIN);
        api.awaitSharingEntry(groupId);

        String child1Id = api.createSampleResourceWithGroupAs(USER_ADMIN, groupId);
        api.awaitSharingEntry(child1Id);

        String child2Id = api.createSampleResourceWithGroupAs(USER_ADMIN, groupId);
        api.awaitSharingEntry(child2Id);

        // 2. Directly share child1 with NO_ACCESS_USER
        TestRestClient.HttpResponse directShare = api.shareResource(child1Id, USER_ADMIN, TestUtils.NO_ACCESS_USER, SAMPLE_FULL_ACCESS);
        directShare.assertStatusCode(HttpStatus.SC_OK);

        // 3. Share the group with NO_ACCESS_USER (gives inherited access to both children)
        TestRestClient.HttpResponse groupShare = api.shareResourceGroup(
            groupId,
            USER_ADMIN,
            TestUtils.NO_ACCESS_USER,
            SAMPLE_GROUP_FULL_ACCESS
        );
        groupShare.assertStatusCode(HttpStatus.SC_OK);

        // 4. List should return both children
        try (TestRestClient client = cluster.getRestClient(TestUtils.NO_ACCESS_USER)) {
            TestRestClient.HttpResponse response = client.get(SECURITY_LIST_ENDPOINT + "?resource_type=" + RESOURCE_TYPE);
            response.assertStatusCode(HttpStatus.SC_OK);

            List<Object> resources = (List<Object>) response.bodyAsMap().get("resources");
            assertThat("Expected 2 child resources (direct + inherited)", resources.size(), equalTo(2));

            List<String> ids = resources.stream().map(r -> (String) ((Map<String, Object>) r).get("resource_id")).toList();
            assertThat(ids, hasItem(child1Id));
            assertThat(ids, hasItem(child2Id));
        }
    }

    /**
     * Scenario:
     *  1. Admin creates a resource group and a child resource
     *  2. NO_ACCESS_USER has NO access to the group
     *  3. List API for child resources should return empty
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testListChildResources_noAccessToParent_returnsEmpty() {
        String groupId = api.createSampleResourceGroupAs(USER_ADMIN);
        api.awaitSharingEntry(groupId);

        String childId = api.createSampleResourceWithGroupAs(USER_ADMIN, groupId);
        api.awaitSharingEntry(childId);

        // NO_ACCESS_USER has no access to group or child
        try (TestRestClient client = cluster.getRestClient(TestUtils.NO_ACCESS_USER)) {
            TestRestClient.HttpResponse response = client.get(SECURITY_LIST_ENDPOINT + "?resource_type=" + RESOURCE_TYPE);
            response.assertStatusCode(HttpStatus.SC_OK);

            List<Object> resources = (List<Object>) response.bodyAsMap().get("resources");
            assertThat("Expected 0 resources when user has no parent access", resources.size(), equalTo(0));
        }
    }
}
