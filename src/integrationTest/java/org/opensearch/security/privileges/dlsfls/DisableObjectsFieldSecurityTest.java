/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.privileges.dlsfls;

import java.util.List;

import org.junit.ClassRule;
import org.junit.Test;

import org.opensearch.security.DefaultObjectMapper;
import org.opensearch.test.framework.TestSecurityConfig;
import org.opensearch.test.framework.cluster.ClusterManager;
import org.opensearch.test.framework.cluster.LocalCluster;

import tools.jackson.databind.JsonNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class DisableObjectsFieldSecurityTest {
    private static final String SECRET = "private-value";
    private static final String SECRET_FIELD = "metrics.private.secret";
    private static final TestSecurityConfig.User FULL = new TestSecurityConfig.User("full").roles(
        new TestSecurityConfig.Role("full").indexPermissions("read").on("disabled-objects-*")
    );
    private static final TestSecurityConfig.User EXCLUDED = new TestSecurityConfig.User("excluded").roles(
        new TestSecurityConfig.Role("excluded").indexPermissions("read").fls("~" + SECRET_FIELD).on("disabled-objects-*")
    );
    private static final TestSecurityConfig.User INCLUDED = new TestSecurityConfig.User("included").roles(
        new TestSecurityConfig.Role("included").indexPermissions("read").fls("metrics.visible", "public").on("disabled-objects-*")
    );
    private static final TestSecurityConfig.User MASKED = new TestSecurityConfig.User("masked").roles(
        new TestSecurityConfig.Role("masked").indexPermissions("read").maskedFields(SECRET_FIELD).on("disabled-objects-*")
    );
    private static final TestSecurityConfig.User WILDCARD_MASKED = new TestSecurityConfig.User("wildcard-masked").roles(
        new TestSecurityConfig.Role("wildcard-masked").indexPermissions("read").maskedFields("metrics.private.*").on("disabled-objects-*")
    );

    @ClassRule
    public static final LocalCluster CLUSTER = new LocalCluster.Builder().clusterManager(ClusterManager.SINGLENODE)
        .anonymousAuth(false)
        .authc(TestSecurityConfig.AuthcDomain.AUTHC_HTTPBASIC_INTERNAL)
        .users(FULL, EXCLUDED, INCLUDED, MASKED, WILDCARD_MASKED)
        .build();

    @Test
    public void testRootLevelDisabledObjects() throws Exception {
        checkFieldSecurity("disabled-objects-root", """
            {"disable_objects":true,"properties":{
              "metrics.private.secret":{"type":"keyword","store":true},
              "metrics.visible":{"type":"keyword"},"public":{"type":"keyword"}
            }}
            """);
    }

    @Test
    public void testObjectLevelDisabledObjects() throws Exception {
        checkFieldSecurity("disabled-objects-object", """
            {"properties":{
              "metrics":{"type":"object","disable_objects":true,"properties":{
                "private.secret":{"type":"keyword","store":true},"visible":{"type":"keyword"}
              }},"public":{"type":"keyword"}
            }}
            """);
    }

    @Test
    public void testDefaultObjectExpansion() throws Exception {
        checkFieldSecurity("disabled-objects-default", """
            {"properties":{
              "metrics":{"properties":{
                "private":{"properties":{"secret":{"type":"keyword","store":true}}},
                "visible":{"type":"keyword"}
              }},"public":{"type":"keyword"}
            }}
            """);
    }

    private void checkFieldSecurity(String index, String mapping) throws Exception {
        try (var admin = CLUSTER.getAdminCertRestClient()) {
            assertEquals(200, admin.putJson(index, "{\"mappings\":" + mapping + "}").getStatusCode());
            assertEquals(201, admin.putJson(index + "/_doc/flat?refresh=true", """
                {"metrics.private.secret":"private-value","metrics.visible":"visible-value","public":"public-value"}
                """).getStatusCode());
            assertEquals(201, admin.putJson(index + "/_doc/object?refresh=true", """
                {"metrics":{"private":{"secret":"private-value"},"visible":"visible-value"},"public":"public-value"}
                """).getStatusCode());
        }

        for (var user : List.of(FULL, EXCLUDED, INCLUDED, MASKED, WILDCARD_MASKED)) {
            boolean hidden = user == EXCLUDED || user == INCLUDED;
            try (var client = CLUSTER.getRestClient(user)) {
                String maskedValue = null;
                for (String id : List.of("flat", "object")) {
                    var response = client.get(index + "/_doc/" + id);
                    assertEquals(200, response.getStatusCode());
                    JsonNode source = DefaultObjectMapper.objectMapper().readTree(response.getBody()).path("_source");
                    assertSource(source, user, hidden);
                    if (user == MASKED || user == WILDCARD_MASKED) {
                        String value = field(source, SECRET_FIELD).asText();
                        if (maskedValue != null) assertEquals(maskedValue, value);
                        maskedValue = value;
                    }
                }
                for (String retrieval : List.of("stored_fields", "docvalue_fields")) {
                    var search = client.postJson(index + "/_search", """
                        {"query":{"match_all":{}},"_source":true,"%s":["metrics.private.secret"]}
                        """.formatted(retrieval));
                    assertEquals(200, search.getStatusCode());
                    JsonNode hits = DefaultObjectMapper.objectMapper().readTree(search.getBody()).path("hits").path("hits");
                    assertEquals(2, hits.size());
                    for (JsonNode hit : hits) {
                        assertSource(hit.path("_source"), user, hidden);
                        JsonNode values = hit.path("fields").path(SECRET_FIELD);
                        if (hidden) {
                            assertTrue(values.isMissingNode());
                        } else {
                            assertTrue(values.isArray());
                            assertEquals(1, values.size());
                            for (JsonNode value : values) {
                                if (user == FULL) assertEquals(SECRET, value.asText());
                                else assertEquals(maskedValue, value.asText());
                            }
                        }
                    }
                }
                var query = client.postJson(index + "/_search", """
                    {"query":{"term":{"metrics.private.secret":"private-value"}}}
                    """);
                assertEquals(200, query.getStatusCode());
                assertEquals(
                    user == FULL ? 2 : 0,
                    DefaultObjectMapper.objectMapper().readTree(query.getBody()).at("/hits/total/value").asInt()
                );
            }
        }
    }

    private void assertSource(JsonNode source, TestSecurityConfig.User user, boolean hidden) {
        assertEquals("public-value", source.path("public").asText());
        assertEquals("visible-value", field(source, "metrics.visible").asText());
        JsonNode secret = field(source, SECRET_FIELD);
        if (hidden) {
            assertTrue(secret.isMissingNode());
        } else if (user == FULL) {
            assertEquals(SECRET, secret.asText());
        } else {
            assertTrue(secret.isTextual());
            assertFalse(secret.asText().isEmpty());
            assertNotEquals(SECRET, secret.asText());
        }
        if (user != FULL) assertFalse(source.toString().contains(SECRET));
    }

    // _source preserves the input shape, independently of how the mapper represents dotted fields.
    private JsonNode field(JsonNode source, String name) {
        if (source.has(name)) return source.path(name);
        int dot = name.indexOf('.');
        return dot < 0 ? source.path(name) : field(source.path(name.substring(0, dot)), name.substring(dot + 1));
    }
}
