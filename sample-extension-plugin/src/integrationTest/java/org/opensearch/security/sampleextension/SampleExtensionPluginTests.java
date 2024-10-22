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

    @ClassRule
    public static LocalCluster cluster = new LocalCluster.Builder().clusterManager(ClusterManager.THREE_CLUSTER_MANAGERS)
        .plugin(SampleExtensionPlugin.class)
        .anonymousAuth(true)
        .authc(AUTHC_HTTPBASIC_INTERNAL)
        .users(USER_ADMIN)
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
            assertThat(pluginsResponse.getBody(), containsString("org.opensearch.security.OpenSearchSecurityPlugin"));
            assertThat(pluginsResponse.getBody(), containsString("org.opensearch.security.sampleextension.SampleExtensionPlugin"));
        }
    }

}
