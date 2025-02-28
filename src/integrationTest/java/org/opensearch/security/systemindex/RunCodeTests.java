/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 */
package org.opensearch.security.systemindex;

import java.util.List;
import java.util.Map;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.opensearch.core.rest.RestStatus;
import org.opensearch.security.systemindex.sampleplugin.SystemIndexPlugin1;
import org.opensearch.security.systemindex.sampleplugin.SystemIndexPlugin2;
import org.opensearch.test.framework.TestSecurityConfig.AuthcDomain;
import org.opensearch.test.framework.cluster.ClusterManager;
import org.opensearch.test.framework.cluster.LocalCluster;
import org.opensearch.test.framework.cluster.TestRestClient;
import org.opensearch.test.framework.cluster.TestRestClient.HttpResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.opensearch.security.support.ConfigConstants.SECURITY_RESTAPI_ROLES_ENABLED;
import static org.opensearch.security.support.ConfigConstants.SECURITY_SYSTEM_INDICES_ENABLED_KEY;
import static org.opensearch.test.framework.TestSecurityConfig.Role.ALL_ACCESS;
import static org.opensearch.test.framework.TestSecurityConfig.User.USER_ADMIN;

@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class RunCodeTests {

    public static final AuthcDomain AUTHC_DOMAIN = new AuthcDomain("basic", 0).httpAuthenticatorWithChallenge("basic").backend("internal");

    @ClassRule
    public static final LocalCluster cluster = new LocalCluster.Builder().clusterManager(ClusterManager.SINGLENODE)
        .anonymousAuth(false)
        .authc(AUTHC_DOMAIN)
        .users(USER_ADMIN)
        .plugin(SystemIndexPlugin1.class, SystemIndexPlugin2.class)
        .nodeSettings(
            Map.of(
                SECURITY_RESTAPI_ROLES_ENABLED,
                List.of("user_" + USER_ADMIN.getName() + "__" + ALL_ACCESS.getName()),
                SECURITY_SYSTEM_INDICES_ENABLED_KEY,
                true
            )
        )
        .build();

    @Test
    public void testRunCode() {
        // Define the policy file location
        String policyFile = "integration-test.policy";

        // Set the system property for security policy
        System.setProperty("java.security.policy", RunCodeTests.class.getClassLoader().getResource(policyFile).getPath());

        // Enable the Security Manager (Deprecated in Java 17+)
        System.setSecurityManager(new SecurityManager());
        System.out.println("Security manager: " + System.getSecurityManager());
        try (TestRestClient client = cluster.getRestClient(USER_ADMIN)) {
            // TODO write a test that calls POST /run-code with a simple System.out.println("Hello, world!")
            // String javaCode = "System.setProperty(\\\"test.key\\\",\\\"Hello, world!\\\");";
            // String javaCode = "System.out.println(\\\"Hello, world!\\\");";
            String javaCode =
                "java.nio.file.Path filePath = java.nio.file.Paths.get(\\\"/Users/cwperx/Projects/opensearch/OpenSearch/build/distribution/local/opensearch-3.0.0-SNAPSHOT/config/opensearch.yml\\\");"
                    + "String content = new String(java.nio.file.Files.readAllBytes(filePath), java.nio.charset.StandardCharsets.UTF_8);"
                    + "System.out.println(\\\"content: \\\" + content);";
            String requestBody = "{\"code\": \"" + javaCode + "\"}";

            System.out.println("Calling run-code");

            HttpResponse response = client.postJson("run-code", requestBody);

            System.out.println("Finished run-code");

            // Verify response
            assertThat(response.getStatusCode(), equalTo(RestStatus.OK.getStatus()));
            assertThat(response.getBody(), response.getBody().contains("\"acknowledged\":true"));
        }
    }
}
