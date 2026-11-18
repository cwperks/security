/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.dlsfls;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.opensearch.test.framework.TestSecurityConfig.Role;
import org.opensearch.test.framework.TestSecurityConfig.User;
import org.opensearch.test.framework.cluster.ClusterManager;
import org.opensearch.test.framework.cluster.LocalCluster;
import org.opensearch.test.framework.cluster.TestRestClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.opensearch.test.framework.TestSecurityConfig.AuthcDomain.AUTHC_HTTPBASIC_INTERNAL;
import static org.opensearch.test.framework.TestSecurityConfig.Role.ALL_ACCESS;
import static org.junit.Assert.assertEquals;

public class TermSuggestionTest {
    private static final String INDEX = "suggestions";
    private static final String UNRESTRICTED_INDEX = "unrestricted";
    private static final User ADMIN = new User("admin").roles(ALL_ACCESS);
    private static final User DLS = new User("dls").roles(
        new Role("dls").clusterPermissions("cluster_composite_ops_ro")
            .indexPermissions("read")
            .dls("{\"term\":{\"tenant\":\"visible\"}}")
            .on(INDEX)
            .indexPermissions("read")
            .on(UNRESTRICTED_INDEX)
    );
    private static final User FLS = new User("fls").roles(
        new Role("fls").clusterPermissions("cluster_composite_ops_ro")
            .indexPermissions("read")
            .fls("~title")
            .on(INDEX)
            .indexPermissions("read")
            .on(UNRESTRICTED_INDEX)
    );
    private static final User MASKED = new User("masked").roles(
        new Role("masked").clusterPermissions("cluster_composite_ops_ro")
            .indexPermissions("read")
            .maskedFields("title")
            .on(INDEX)
            .indexPermissions("read")
            .on(UNRESTRICTED_INDEX)
    );
    private static final User READER = new User("reader").roles(
        new Role("reader").clusterPermissions("cluster_composite_ops_ro")
            .indexPermissions("read")
            .on(INDEX)
            .indexPermissions("read")
            .on(UNRESTRICTED_INDEX)
    );

    @ClassRule
    public static final LocalCluster cluster = new LocalCluster.Builder().clusterManager(ClusterManager.SINGLENODE)
        .anonymousAuth(false)
        .authc(AUTHC_HTTPBASIC_INTERNAL)
        .users(ADMIN, DLS, FLS, MASKED, READER)
        .build();

    @BeforeClass
    public static void setupData() {
        try (TestRestClient client = cluster.getRestClient(ADMIN)) {
            assertEquals(
                200,
                client.putJson(UNRESTRICTED_INDEX, "{\"settings\":{\"number_of_shards\":1,\"number_of_replicas\":0}}").getStatusCode()
            );
            assertEquals(
                201,
                client.putJson(UNRESTRICTED_INDEX + "/_doc/1?refresh=true", "{\"title\":\"confidentialword\"}").getStatusCode()
            );
            assertEquals(200, client.putJson(INDEX, """
                {"settings":{"number_of_shards":1,"number_of_replicas":0},"mappings":{"properties":{
                  "tenant":{"type":"keyword"},"title":{"type":"text"},"public":{"type":"text"},
                  "title_alias":{"type":"alias","path":"title"}}}}
                """).getStatusCode());
            assertEquals(
                201,
                client.putJson(INDEX + "/_doc/1?refresh=true", "{\"tenant\":\"visible\",\"title\":\"ordinary\",\"public\":\"publicword\"}")
                    .getStatusCode()
            );
            assertEquals(
                201,
                client.putJson(
                    INDEX + "/_doc/2?refresh=true",
                    "{\"tenant\":\"hidden\",\"title\":\"confidentialword\",\"public\":\"ordinary\"}"
                ).getStatusCode()
            );
        }
    }

    private static String suggestion(String field) {
        return """
            {"size":0,"suggest":{"spelling":{"text":"confidentialwore","term":{
              "field":"%s","suggest_mode":"always","min_doc_freq":0,"max_term_freq":1}}}}
            """.formatted(field);
    }

    @Test
    public void dlsRejectsSuggestionsFromHiddenDocuments() {
        try (TestRestClient client = cluster.getRestClient(DLS)) {
            var hits = client.postJson(INDEX + "/_search", "{\"query\":{\"match_all\":{}}}");
            assertEquals(200, hits.getStatusCode());
            assertThat(hits.getBody(), not(containsString("confidentialword")));
            var response = client.postJson(INDEX + "/_search", suggestion("title"));
            assertEquals(response.getBody(), 403, response.getStatusCode());
            assertThat(response.getBody(), containsString("Term suggestions are not supported"));
        }
    }

    @Test
    public void unrestrictedUsersCanSuggestTerms() {
        for (User user : new User[] { ADMIN, READER }) {
            try (TestRestClient client = cluster.getRestClient(user)) {
                var response = client.postJson(INDEX + "/_search", suggestion("title"));
                assertEquals(response.getBody(), 200, response.getStatusCode());
                assertThat(response.getBody(), containsString("confidentialword"));
            }
        }
    }

    @Test
    public void protectedFieldsAndTheirAliasesRejectSuggestions() {
        for (User user : new User[] { FLS, MASKED }) {
            for (String field : new String[] { "title", "title_alias" }) {
                try (TestRestClient client = cluster.getRestClient(user)) {
                    var response = client.postJson(INDEX + "/_search", suggestion(field));
                    assertEquals(response.getBody(), 403, response.getStatusCode());
                    assertThat(response.getBody(), containsString("Term suggestions are not supported"));
                }
            }
        }
    }

    @Test
    public void fieldRestrictionsRejectSuggestionsEvenOnOtherFields() {
        for (User user : new User[] { FLS, MASKED }) {
            try (TestRestClient client = cluster.getRestClient(user)) {
                var response = client.postJson(INDEX + "/_search", suggestion("public"));
                assertEquals(response.getBody(), 403, response.getStatusCode());
                assertThat(response.getBody(), containsString("Term suggestions are not supported"));
            }
        }
    }

    @Test
    public void multiSearchRejectsOnlyTheRestrictedSuggestionRequest() {
        try (TestRestClient client = cluster.getRestClient(DLS)) {
            var response = client.postJson(
                INDEX + "/_msearch",
                "{}\n" + suggestion("title").replace("\n", "") + "\n{}\n{\"query\":{\"match_all\":{}}}\n"
            );
            assertEquals(response.getBody(), 200, response.getStatusCode());
            assertThat(response.getBody(), containsString("\"status\":403"));
            assertThat(response.getBody(), containsString("\"status\":200"));
            assertThat(response.getBody(), not(containsString("confidentialword")));
        }
    }

    @Test
    public void restrictionsAreScopedToTheRequestedIndices() {
        for (User user : new User[] { DLS, FLS, MASKED }) {
            try (TestRestClient client = cluster.getRestClient(user)) {
                var allowed = client.postJson(UNRESTRICTED_INDEX + "/_search", suggestion("title"));
                assertEquals(allowed.getBody(), 200, allowed.getStatusCode());
                assertThat(allowed.getBody(), containsString("confidentialword"));
                var mixed = client.postJson(
                    INDEX + "," + UNRESTRICTED_INDEX + "/_search?allow_partial_search_results=true",
                    suggestion("title")
                );
                assertEquals(mixed.getBody(), 403, mixed.getStatusCode());
                assertThat(mixed.getBody(), containsString("Term suggestions are not supported"));
            }
        }
    }

}
