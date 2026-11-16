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

import org.opensearch.test.framework.TestSecurityConfig.Role;
import org.opensearch.test.framework.TestSecurityConfig.User;
import org.opensearch.test.framework.cluster.ClusterManager;
import org.opensearch.test.framework.cluster.LocalCluster;
import org.opensearch.test.framework.cluster.TestRestClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.opensearch.test.framework.TestSecurityConfig.AuthcDomain.AUTHC_HTTPBASIC_INTERNAL;

public class JoinFieldFlsTest {
    private static final User ADMIN = new User("admin").roles(Role.ALL_ACCESS);
    private static final User EXCLUDE = user("exclude", "~relation#name");
    private static final User INCLUDE = user("include", "public_field");

    private static User user(String name, String... fls) {
        return new User(name).roles(
            new Role(name).clusterPermissions("cluster_composite_ops_ro").indexPermissions("read").fls(fls).on("joins")
        );
    }

    @ClassRule
    public static final LocalCluster cluster = new LocalCluster.Builder().clusterManager(ClusterManager.SINGLENODE)
        .authc(AUTHC_HTTPBASIC_INTERNAL)
        .users(ADMIN, EXCLUDE, INCLUDE)
        .build();

    @BeforeClass
    public static void setupIndex() throws IOException {
        try (TestRestClient client = cluster.getRestClient(ADMIN)) {
            assertThat(client.putJson("joins", """
                {"mappings":{"properties":{
                  "relation#name":{"type":"join","relations":{"parent#type":"child"}},
                  "relation#name#ordinary":{"type":"keyword"},"public_field":{"type":"keyword"}
                }}}
                """).getStatusCode(), is(200));
            assertThat(client.putJson("joins/_doc/1?refresh=true", """
                {"relation#name":"parent#type","public_field":"public","relation#name#ordinary":"ordinary"}
                """).getStatusCode(), is(201));
            assertThat(client.putJson("joins/_doc/2?routing=1&refresh=true", """
                {"relation#name":{"name":"child","parent":"1"},"public_field":"public"}
                """).getStatusCode(), is(201));
        }
    }

    @Test
    public void hiddenJoinIdsCannotBeQueriedOrAggregated() throws IOException {
        for (User user : List.of(ADMIN, EXCLUDE, INCLUDE)) {
            try (TestRestClient client = cluster.getRestClient(user)) {
                var response = client.postJson("joins/_search", """
                    {"query":{"term":{"relation#name#parent#type":"1"}}}
                    """);
                assertThat(response.getStatusCode(), is(200));
                assertThat(response.getIntFromJsonBody("/_shards/failed"), is(0));
                assertThat(response.getIntFromJsonBody("/hits/total/value"), is(user == ADMIN ? 2 : 0));
                var agg = client.postJson("joins/_search", """
                    {"size":0,"aggs":{"ids":{"terms":{"field":"relation#name#parent#type"}}}}
                    """);
                assertThat(agg.getStatusCode(), is(200));
                assertThat(agg.getIntFromJsonBody("/_shards/failed"), is(0));
                assertThat(agg.getTextFromJsonBody("/aggregations/ids/buckets/0/key"), is(user == ADMIN ? "1" : ""));
                var publicSearch = client.postJson("joins/_search", "{\"query\":{\"term\":{\"public_field\":\"public\"}}}");
                assertThat(publicSearch.getStatusCode(), is(200));
                assertThat(publicSearch.getIntFromJsonBody("/hits/total/value"), is(2));
            }
        }
        try (TestRestClient client = cluster.getRestClient(EXCLUDE)) {
            var response = client.postJson("joins/_search", "{\"query\":{\"term\":{\"relation#name#ordinary\":\"ordinary\"}}}");
            assertThat(response.getStatusCode(), is(200));
            assertThat(response.getIntFromJsonBody("/hits/total/value"), is(1));
        }
    }
}
