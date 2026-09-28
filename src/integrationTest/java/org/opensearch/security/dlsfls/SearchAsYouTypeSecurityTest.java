/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.dlsfls;

import java.io.IOException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.opensearch.index.mapper.MapperExtrasModulePlugin;
import org.opensearch.test.framework.TestSecurityConfig.Role;
import org.opensearch.test.framework.TestSecurityConfig.User;
import org.opensearch.test.framework.cluster.ClusterManager;
import org.opensearch.test.framework.cluster.LocalCluster;
import org.opensearch.test.framework.cluster.TestRestClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.opensearch.test.framework.TestSecurityConfig.AuthcDomain.AUTHC_HTTPBASIC_INTERNAL;

public class SearchAsYouTypeSecurityTest {
    private static final String INDEX = "autocomplete";
    private static final User ADMIN = new User("admin").roles(Role.ALL_ACCESS);
    private static final User FLS_PARENT = new User("fls_parent").roles(
        role("fls_parent").indexPermissions("read").fls("~title").on(INDEX)
    );
    private static final User FLS_ALL = new User("fls_all").roles(
        role("fls_all").indexPermissions("read").fls("~title", "~title.*").on(INDEX)
    );
    private static final User MASK_PARENT = new User("mask_parent").roles(
        role("mask_parent").indexPermissions("read").maskedFields("title").on(INDEX)
    );
    private static final User MASK_ALL = new User("mask_all").roles(
        role("mask_all").indexPermissions("read").maskedFields("title", "title.*").on(INDEX)
    );
    private static final User DLS = new User("dls").roles(
        role("dls").indexPermissions("read").dls("{\"term\":{\"visible\":true}}").on(INDEX)
    );

    private static Role role(String name) {
        return new Role(name).clusterPermissions("cluster_composite_ops_ro");
    }

    @ClassRule
    public static final LocalCluster cluster = new LocalCluster.Builder().clusterManager(ClusterManager.SINGLENODE)
        .plugin(MapperExtrasModulePlugin.class)
        .anonymousAuth(false)
        .authc(AUTHC_HTTPBASIC_INTERNAL)
        .users(ADMIN, FLS_PARENT, FLS_ALL, MASK_PARENT, MASK_ALL, DLS)
        .build();

    @BeforeClass
    public static void setupIndex() throws IOException {
        try (TestRestClient client = cluster.getRestClient(ADMIN)) {
            assertThat(client.putJson(INDEX, """
                {"mappings":{"properties":{"title":{"type":"search_as_you_type","max_shingle_size":4},"visible":{"type":"boolean"}}}}
                """).getStatusCode(), is(200));
            assertThat(
                client.putJson(INDEX + "/_doc/1?refresh=true", "{\"title\":\"quick brown fox jumps\",\"visible\":true}").getStatusCode(),
                is(201)
            );
            assertThat(
                client.putJson(INDEX + "/_doc/2?refresh=true", "{\"title\":\"quick brown fox jumps\",\"visible\":false}").getStatusCode(),
                is(201)
            );
        }
    }

    private static final List<String> QUERIES = List.of(
        "{\"match\":{\"title\":\"quick\"}}",
        "{\"prefix\":{\"title\":\"qui\"}}",
        "{\"multi_match\":{\"query\":\"quick br\",\"type\":\"bool_prefix\",\"fields\":[\"title\",\"title._2gram\",\"title._3gram\",\"title._4gram\"]}}",
        "{\"match_phrase\":{\"title\":\"quick brown\"}}",
        "{\"term\":{\"title._2gram\":\"quick brown\"}}",
        "{\"term\":{\"title._3gram\":\"quick brown fox\"}}",
        "{\"term\":{\"title._4gram\":\"quick brown fox jumps\"}}",
        "{\"term\":{\"title._index_prefix\":\"qui\"}}"
    );

    private void assertSearches(User user, int expected) throws IOException {
        try (TestRestClient client = cluster.getRestClient(user)) {
            for (String query : QUERIES) {
                var response = client.postJson(INDEX + "/_search", "{\"query\":" + query + "}");
                assertThat(query, response.getStatusCode(), is(200));
                assertThat(query, response.getIntFromJsonBody("/_shards/failed"), is(0));
                assertThat(user.getName() + ": " + query, response.getIntFromJsonBody("/hits/total/value"), is(expected));
            }
        }
    }

    @Test
    public void restrictionsPreserveOtherSearchableFieldsAndMaskSources() throws IOException {
        for (User user : List.of(FLS_PARENT, FLS_ALL, MASK_PARENT, MASK_ALL)) {
            try (TestRestClient client = cluster.getRestClient(user)) {
                var response = client.postJson(INDEX + "/_search", "{\"query\":{\"term\":{\"visible\":true}}}");
                assertThat(response.getStatusCode(), is(200));
                assertThat(response.getIntFromJsonBody("/_shards/failed"), is(0));
                assertThat(response.getIntFromJsonBody("/hits/total/value"), is(1));
                String title = response.getTextFromJsonBody("/hits/hits/0/_source/title");
                if (user == FLS_PARENT || user == FLS_ALL) {
                    assertThat(title, is(emptyString()));
                } else {
                    assertThat(title, not(emptyString()));
                    assertThat(title, not("quick brown fox jumps"));
                }
            }
        }
    }

    @Test
    public void unrestrictedSearchesMatchBothDocuments() throws IOException {
        assertSearches(ADMIN, 2);
    }

    @Test
    public void flsOnParentBlocksSearches() throws IOException {
        assertSearches(FLS_PARENT, 0);
    }

    @Test
    public void flsOnParentAndSubfieldsBlocksSearches() throws IOException {
        assertSearches(FLS_ALL, 0);
    }

    @Test
    public void maskingParentBlocksSearches() throws IOException {
        assertSearches(MASK_PARENT, 0);
    }

    @Test
    public void maskingParentAndSubfieldsBlocksSearches() throws IOException {
        assertSearches(MASK_ALL, 0);
    }

    @Test
    public void dlsRestrictsDocumentsWithoutDisablingSearches() throws IOException {
        assertSearches(DLS, 1);
    }
}
