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

public class RestrictedSuggestionTest {
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
    private static final User NAME_FLS = new User("name_fls").roles(
        new Role("name_fls").clusterPermissions("cluster_composite_ops_ro")
            .indexPermissions("read")
            .fls("~name")
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
        .users(ADMIN, DLS, FLS, NAME_FLS, MASKED, READER)
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
                  "name":{"type":"completion"},"title_alias":{"type":"alias","path":"title"}}}}
                """).getStatusCode());
            assertEquals(
                201,
                client.putJson(
                    INDEX + "/_doc/1?refresh=true",
                    "{\"tenant\":\"visible\",\"title\":\"ordinary\",\"public\":\"publicword\",\"name\":\"ordinaryname\"}"
                ).getStatusCode()
            );
            assertEquals(
                201,
                client.putJson(
                    INDEX + "/_doc/2?refresh=true",
                    "{\"tenant\":\"hidden\",\"title\":\"confidentialword\",\"public\":\"ordinary\",\"name\":\"confidentialname\"}"
                ).getStatusCode()
            );
        }
    }

    private static String termSuggestion(String field) {
        return """
            {"size":0,"suggest":{"spelling":{"text":"confidentialwore","term":{
              "field":"%s","suggest_mode":"always","min_doc_freq":0,"max_term_freq":1}}}}
            """.formatted(field);
    }

    private static String phraseSuggestion(String field) {
        return """
            {"size":0,"suggest":{"spelling":{"text":"confidentialwore","phrase":{"field":"%s",
              "direct_generator":[{"field":"%s","suggest_mode":"always","min_doc_freq":0}]}}}}
            """.formatted(field, field);
    }

    private static String completionSuggestion() {
        return "{\"size\":0,\"suggest\":{\"completion\":{\"prefix\":\"confid\",\"completion\":{\"field\":\"name\"}}}}";
    }

    @Test
    public void dlsRejectsSuggestionsFromHiddenDocuments() {
        try (TestRestClient client = cluster.getRestClient(DLS)) {
            var hits = client.postJson(INDEX + "/_search", "{\"query\":{\"match_all\":{}}}");
            assertEquals(200, hits.getStatusCode());
            assertThat(hits.getBody(), not(containsString("confidentialword")));
            var response = client.postJson(INDEX + "/_search", termSuggestion("title"));
            assertEquals(response.getBody(), 403, response.getStatusCode());
            assertThat(response.getBody(), containsString("Term and phrase suggestions are not supported"));
        }
    }

    @Test
    public void unrestrictedUsersCanSuggestTerms() {
        for (User user : new User[] { ADMIN, READER }) {
            try (TestRestClient client = cluster.getRestClient(user)) {
                var response = client.postJson(INDEX + "/_search", termSuggestion("title"));
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
                    var response = client.postJson(INDEX + "/_search", termSuggestion(field));
                    assertEquals(response.getBody(), 403, response.getStatusCode());
                    assertThat(response.getBody(), containsString("Term and phrase suggestions are not supported"));
                }
            }
        }
    }

    @Test
    public void fieldRestrictionsRejectSuggestionsEvenOnOtherFields() {
        for (User user : new User[] { FLS, MASKED }) {
            try (TestRestClient client = cluster.getRestClient(user)) {
                var response = client.postJson(INDEX + "/_search", termSuggestion("public"));
                assertEquals(response.getBody(), 403, response.getStatusCode());
                assertThat(response.getBody(), containsString("Term and phrase suggestions are not supported"));
            }
        }
    }

    @Test
    public void multiSearchRejectsOnlyTheRestrictedSuggestionRequest() {
        try (TestRestClient client = cluster.getRestClient(DLS)) {
            var response = client.postJson(
                INDEX + "/_msearch",
                "{}\n" + termSuggestion("title").replace("\n", "") + "\n{}\n{\"query\":{\"match_all\":{}}}\n"
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
                var allowed = client.postJson(UNRESTRICTED_INDEX + "/_search", termSuggestion("title"));
                assertEquals(allowed.getBody(), 200, allowed.getStatusCode());
                assertThat(allowed.getBody(), containsString("confidentialword"));
                var mixed = client.postJson(
                    INDEX + "," + UNRESTRICTED_INDEX + "/_search?allow_partial_search_results=true",
                    termSuggestion("title")
                );
                assertEquals(mixed.getBody(), 403, mixed.getStatusCode());
                assertThat(mixed.getBody(), containsString("Term and phrase suggestions are not supported"));
            }
        }
    }

    @Test
    public void phraseSuggestionsAreRejectedForRestrictedUsers() {
        for (User user : new User[] { DLS, FLS, MASKED }) {
            try (TestRestClient client = cluster.getRestClient(user)) {
                var response = client.postJson(INDEX + "/_search", phraseSuggestion("title"));
                assertEquals(response.getBody(), 403, response.getStatusCode());
                assertThat(response.getBody(), containsString("Term and phrase suggestions are not supported"));
                assertThat(response.getBody(), not(containsString("confidentialword")));
            }
        }
    }

    @Test
    public void unrestrictedUsersCanSuggestPhrases() {
        for (User user : new User[] { ADMIN, READER }) {
            try (TestRestClient client = cluster.getRestClient(user)) {
                var response = client.postJson(INDEX + "/_search", phraseSuggestion("title"));
                assertEquals(response.getBody(), 200, response.getStatusCode());
                assertThat(response.getBody(), containsString("confidentialword"));
            }
        }
    }

    @Test
    public void uriSearchTermSuggestionsAreRejectedForRestrictedUsers() {
        try (TestRestClient client = cluster.getRestClient(DLS)) {
            var response = client.get(
                INDEX + "/_search?size=0&suggest_field=title&suggest_text=confidentialwore&suggest_mode=always&suggest_size=5"
            );
            assertEquals(response.getBody(), 403, response.getStatusCode());
            assertThat(response.getBody(), containsString("Term and phrase suggestions are not supported"));
        }
    }

    @Test
    public void completionSuggestionsDoNotExposeRestrictedValues() {
        try (TestRestClient client = cluster.getRestClient(ADMIN)) {
            var response = client.postJson(INDEX + "/_search", completionSuggestion());
            assertEquals(response.getBody(), 200, response.getStatusCode());
            assertThat(response.getBody(), containsString("confidentialname"));
        }
        for (User user : new User[] { DLS, NAME_FLS }) {
            try (TestRestClient client = cluster.getRestClient(user)) {
                var response = client.postJson(INDEX + "/_search", completionSuggestion());
                assertEquals(response.getBody(), 200, response.getStatusCode());
                assertThat(response.getBody(), not(containsString("confidentialname")));
            }
        }
    }

}
