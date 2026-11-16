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
import static org.hamcrest.Matchers.is;
import static org.opensearch.test.framework.TestSecurityConfig.AuthcDomain.AUTHC_HTTPBASIC_INTERNAL;

public class RankFeatureFlsTest {
    private static final User ADMIN = new User("admin").roles(Role.ALL_ACCESS);
    private static final User EXCLUDE = user("exclude", "~secret");
    private static final User INCLUDE = user("include", "public_rank");

    private static User user(String name, String... fls) {
        return new User(name).roles(
            new Role(name).clusterPermissions("cluster_composite_ops_ro").indexPermissions("read").fls(fls).on("features")
        );
    }

    @ClassRule
    public static final LocalCluster cluster = new LocalCluster.Builder().clusterManager(ClusterManager.SINGLENODE)
        .plugin(MapperExtrasModulePlugin.class)
        .authc(AUTHC_HTTPBASIC_INTERNAL)
        .users(ADMIN, EXCLUDE, INCLUDE)
        .build();

    @BeforeClass
    public static void setupIndex() throws IOException {
        try (TestRestClient client = cluster.getRestClient(ADMIN)) {
            assertThat(client.putJson("features", """
                {"mappings":{"properties":{"secret":{"type":"rank_feature"},"public_rank":{"type":"rank_feature"}}}}
                """).getStatusCode(), is(200));
            assertThat(client.putJson("features/_doc/1?refresh=true", "{\"secret\":10,\"public_rank\":20}").getStatusCode(), is(201));
        }
    }

    @Test
    public void hiddenFeaturesCannotBeQueriedButPublicFeaturesRemainUsable() throws IOException {
        for (User user : List.of(ADMIN, EXCLUDE, INCLUDE)) {
            try (TestRestClient client = cluster.getRestClient(user)) {
                for (String field : List.of("secret", "public_rank")) {
                    int expected = user == ADMIN || field.equals("public_rank") ? 1 : 0;
                    for (String query : List.of(
                        "{\"rank_feature\":{\"field\":\"" + field + "\"}}",
                        "{\"exists\":{\"field\":\"" + field + "\"}}"
                    )) {
                        var response = client.postJson("features/_search", "{\"query\":" + query + "}");
                        assertThat(response.getStatusCode(), is(200));
                        assertThat(response.getIntFromJsonBody("/_shards/failed"), is(0));
                        assertThat(user.getName() + query, response.getIntFromJsonBody("/hits/total/value"), is(expected));
                    }
                }
                if (user != ADMIN) {
                    var response = client.get("features/_doc/1");
                    assertThat(response.getStatusCode(), is(200));
                    assertThat(response.getTextFromJsonBody("/_source/secret"), is(""));
                }
            }
        }
    }
}
