/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.dlsfls;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.opensearch.test.framework.TestSecurityConfig.AuthcDomain.AUTHC_HTTPBASIC_INTERNAL;

/**
 * Multi-fields and copy_to targets hold the values of the field they are derived from, so masking a field has to cover them too.
 */
public class MaskedDerivedFieldTest {
    private static final String INDEX = "derived";
    private static final String SECRET = "Alice Smith";
    private static final String VISIBLE = "Visible Value";
    private static final User ADMIN = new User("admin").roles(Role.ALL_ACCESS);
    private static final User MASKED = new User("masked").roles(
        new Role("masked").clusterPermissions("cluster_composite_ops_ro").indexPermissions("read").maskedFields("name").on(INDEX)
    );

    @ClassRule
    public static final LocalCluster cluster = new LocalCluster.Builder().clusterManager(ClusterManager.SINGLENODE)
        .anonymousAuth(false)
        .authc(AUTHC_HTTPBASIC_INTERNAL)
        .users(ADMIN, MASKED)
        .build();

    @BeforeClass
    public static void setupIndex() {
        try (TestRestClient client = cluster.getRestClient(ADMIN)) {
            var created = client.putJson(INDEX, """
                {"settings":{"number_of_shards":1,"number_of_replicas":0},"mappings":{"properties":{
                  "name":{"type":"text","copy_to":["everything"],
                    "fields":{"raw":{"type":"keyword"},"keyword":{"type":"keyword"},"words":{"type":"text"}}},
                  "everything":{"type":"keyword"},
                  "plain":{"type":"text","fields":{"raw":{"type":"keyword"}}}
                }}}
                """);
            assertThat(created.getBody(), created.getStatusCode(), is(200));
            assertThat(
                client.putJson(INDEX + "/_doc/1?refresh=true", "{\"name\":\"" + SECRET + "\",\"plain\":\"" + VISIBLE + "\"}")
                    .getStatusCode(),
                is(201)
            );
        }
    }

    private static String search(User user, String body) {
        try (TestRestClient client = cluster.getRestClient(user)) {
            var response = client.postJson(INDEX + "/_search", body);
            assertThat(response.getBody(), response.getStatusCode(), is(200));
            return response.getBody();
        }
    }

    private static String termsAggKey(User user, String field) {
        String body = search(user, "{\"size\":0,\"aggs\":{\"a\":{\"terms\":{\"field\":\"" + field + "\"}}}}");
        Matcher matcher = Pattern.compile("\"buckets\":\\[\\{\"key\":\"([^\"]+)\"").matcher(body);
        assertThat(body, matcher.find(), is(true));
        return matcher.group(1);
    }

    private static void assertNoHits(User user, String query) {
        assertThat(query, search(user, "{\"size\":0,\"query\":" + query + "}"), containsString("\"total\":{\"value\":0"));
    }

    @Test
    public void adminSeesPlaintext() {
        assertThat(termsAggKey(ADMIN, "name.raw"), equalTo(SECRET));
        assertThat(termsAggKey(ADMIN, "everything"), equalTo(SECRET));
    }

    @Test
    public void multiFieldAggregationsAreHashedLikeTheKeywordSubfield() {
        String hashed = termsAggKey(MASKED, "name.keyword");
        assertThat(hashed, not(equalTo(SECRET)));
        assertThat(termsAggKey(MASKED, "name.raw"), equalTo(hashed));
    }

    @Test
    public void copyToTargetAggregationsAreHashed() {
        assertThat(termsAggKey(MASKED, "everything"), equalTo(termsAggKey(MASKED, "name.keyword")));
    }

    @Test
    public void multiFieldDocValuesAreHashed() {
        String body = search(MASKED, "{\"query\":{\"match_all\":{}},\"_source\":false,\"docvalue_fields\":[\"name.raw\",\"everything\"]}");
        assertThat(body, not(containsString(SECRET)));
        assertThat(body, containsString("name.raw"));
    }

    @Test
    public void multiFieldAndCopyToTermsAreNotSearchable() {
        assertNoHits(MASKED, "{\"term\":{\"name.raw\":\"" + SECRET + "\"}}");
        assertNoHits(MASKED, "{\"prefix\":{\"name.raw\":\"Alice\"}}");
        assertNoHits(MASKED, "{\"term\":{\"everything\":\"" + SECRET + "\"}}");
        assertNoHits(MASKED, "{\"match\":{\"name.words\":\"alice\"}}");
    }

    @Test
    public void multiFieldTermSuggestionsAreEmpty() {
        String body = search(
            MASKED,
            "{\"size\":0,\"suggest\":{\"s\":{\"text\":\"alic\",\"term\":{\"field\":\"name.words\",\"suggest_mode\":\"always\"}}}}"
        );
        assertThat(body, containsString("\"options\":[]"));
    }

    @Test
    public void unrelatedFieldsAndTheirMultiFieldsAreNotMasked() {
        assertThat(termsAggKey(MASKED, "plain.raw"), equalTo(VISIBLE));
    }
}
