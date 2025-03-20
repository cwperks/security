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

package org.opensearch.security.sampleextension;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import org.apache.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.opensearch.test.framework.TestSecurityConfig;
import org.opensearch.test.framework.cluster.ClusterManager;
import org.opensearch.test.framework.cluster.LocalCluster;
import org.opensearch.test.framework.cluster.TestRestClient;
import org.opensearch.test.framework.cluster.TestRestClient.HttpResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.opensearch.test.framework.TestSecurityConfig.AuthcDomain.AUTHC_HTTPBASIC_INTERNAL;
import static org.opensearch.test.framework.TestSecurityConfig.User.USER_ADMIN;

@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class SampleExtensionPluginTests {

    public final static TestSecurityConfig.User SHARED_WITH_USER = new TestSecurityConfig.User("resource_sharing_test_user").roles(
        new TestSecurityConfig.Role("customrole").indexPermissions("*").on("*").clusterPermissions("*")
    );

    @ClassRule
    public static LocalCluster cluster = new LocalCluster.Builder().clusterManager(ClusterManager.SINGLENODE)
        .plugin(SampleExtensionPlugin.class)
        .anonymousAuth(true)
        .authc(AUTHC_HTTPBASIC_INTERNAL)
        .users(USER_ADMIN, SHARED_WITH_USER)
        .build();

    @Test
    public void testSecurityRoles() throws Exception {
        try (TestRestClient client = cluster.getRestClient(USER_ADMIN)) {
            HttpResponse response = client.getAuthInfo();
            response.assertStatusCode(HttpStatus.SC_OK);

            // Check username
            assertThat(response.getTextFromJsonBody("/user_name"), equalTo("admin"));
            System.out.println("Response: " + response.getBody());
            HttpResponse pluginsResponse = client.get("_cat/plugins?s=component&h=name,component,version,description");
            System.out.println("pluginsResponse: " + pluginsResponse.getBody());
            assertThat(pluginsResponse.getBody(), containsString("org.opensearch.security.OpenSearchSecurityPlugin"));
            assertThat(pluginsResponse.getBody(), containsString("org.opensearch.security.sampleextension.SampleExtensionPlugin"));
        }
    }

    @Test
    public void testCreateAndUpdateOwnSampleResource() throws Exception {
        String resourceId;
        try (TestRestClient client = cluster.getRestClient(USER_ADMIN)) {
            String sampleResource = "{\"name\":\"sample\"}";
            HttpResponse response = client.postJson("_plugins/resource_sharing_example/resource", sampleResource);
            response.assertStatusCode(HttpStatus.SC_OK);
            System.out.println("Response: " + response.getBody());

            resourceId = response.getTextFromJsonBody("/resourceId");

            System.out.println("resourceId: " + resourceId);
            Thread.sleep(2000);
        }
        try (TestRestClient client = cluster.getRestClient(cluster.getAdminCertificate())) {
            // HttpResponse response = client.postJson(".resource-sharing/_search", "{\"query\" : {\"match_all\" : {}}}");
            // System.out.println("Resource sharing entries: " + response.getBody());

            HttpResponse response2 = client.postJson(".sample_extension_resources/_search", "{\"query\" : {\"match_all\" : {}}}");
            System.out.println("Sample resources: " + response2.getBody());
        }

        try (TestRestClient client = cluster.getRestClient(USER_ADMIN)) {
            Thread.sleep(1000);

            HttpResponse getResponse = client.get("_plugins/resource_sharing_example/resource/" + resourceId);
            getResponse.assertStatusCode(HttpStatus.SC_OK);
            System.out.println("Get Response: " + getResponse.getBody());

            String sampleResourceUpdated = "{\"name\":\"sampleUpdated\"}";
            HttpResponse updateResponse = client.putJson(
                "_plugins/resource_sharing_example/resource/update/" + resourceId,
                sampleResourceUpdated
            );
            updateResponse.assertStatusCode(HttpStatus.SC_OK);
            System.out.println("Update Response: " + updateResponse.getBody());
        }

        try (TestRestClient client = cluster.getRestClient(cluster.getAdminCertificate())) {

            HttpResponse response2 = client.postJson(".sample_extension_resources/_search", "{\"query\" : {\"match_all\" : {}}}");
            System.out.println("Sample resources: " + response2.getBody());
        }

        try (TestRestClient client = cluster.getRestClient(USER_ADMIN)) {
            String shareWithPayload = "{\"share_with\":{\"allowed_actions\": [\"unlimited\"], \"users\": [\""
                + SHARED_WITH_USER.getName()
                + "\"], \"backend_roles\": []}}";
            HttpResponse shareWithResponse = client.putJson(
                "_plugins/_security/resource/sample_resource/" + resourceId + "/share_with",
                shareWithPayload
            );
            shareWithResponse.assertStatusCode(HttpStatus.SC_OK);
            System.out.println("Share With Response: " + shareWithResponse.getBody());
        }

        try (TestRestClient client = cluster.getRestClient(cluster.getAdminCertificate())) {

            HttpResponse response2 = client.postJson(".sample_extension_resources/_search", "{\"query\" : {\"match_all\" : {}}}");
            System.out.println("Sample resources: " + response2.getBody());
        }

        try (TestRestClient client = cluster.getRestClient(SHARED_WITH_USER)) {

            HttpResponse listResponse = client.get("_plugins/resource_sharing_example/resource");
            listResponse.assertStatusCode(HttpStatus.SC_OK);
            System.out.println("List Response: " + listResponse.getBody());
        }
    }

    @Test
    public void testCreateResourceAndTryUpdateWithOtherUser() throws Exception {
        String resourceId;
        try (TestRestClient client = cluster.getRestClient(USER_ADMIN)) {
            String sampleResource = "{\"name\":\"sample\"}";
            HttpResponse response = client.postJson("_plugins/resource_sharing_example/resource", sampleResource);
            response.assertStatusCode(HttpStatus.SC_OK);
            System.out.println("Response: " + response.getBody());

            resourceId = response.getTextFromJsonBody("/resourceId");

            System.out.println("resourceId: " + resourceId);
            Thread.sleep(2000);
        }
        try (TestRestClient client = cluster.getRestClient(cluster.getAdminCertificate())) {
            // HttpResponse response = client.postJson(".resource-sharing/_search", "{\"query\" : {\"match_all\" : {}}}");
            // System.out.println("Resource sharing entries: " + response.getBody());

            HttpResponse response2 = client.postJson(".sample_extension_resources/_search", "{\"query\" : {\"match_all\" : {}}}");
            System.out.println("Sample resources: " + response2.getBody());
        }

        try (TestRestClient client = cluster.getRestClient(USER_ADMIN)) {
            Thread.sleep(1000);

            HttpResponse getResponse = client.get("_plugins/resource_sharing_example/resource/" + resourceId);
            getResponse.assertStatusCode(HttpStatus.SC_OK);
            System.out.println("Get Response: " + getResponse.getBody());
        }

        try (TestRestClient client = cluster.getRestClient(SHARED_WITH_USER)) {
            String sampleResourceUpdated = "{\"name\":\"sampleUpdated\"}";
            HttpResponse updateResponse = client.putJson(
                "_plugins/resource_sharing_example/resource/update/" + resourceId,
                sampleResourceUpdated
            );
            updateResponse.assertStatusCode(HttpStatus.SC_FORBIDDEN);
            System.out.println("Update Response: " + updateResponse.getBody());
        }
    }

}
