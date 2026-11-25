/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security;

import org.apache.hc.core5.http.message.BasicHeader;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.opensearch.common.settings.Settings;
import org.opensearch.security.auditlog.impl.AuditCategory;
import org.opensearch.security.auditlog.impl.AuditMessage;
import org.opensearch.test.framework.AuditConfiguration;
import org.opensearch.test.framework.AuditFilters;
import org.opensearch.test.framework.audit.AuditLogsRule;
import org.opensearch.test.framework.cluster.ClusterManager;
import org.opensearch.test.framework.cluster.LocalCluster;
import org.opensearch.test.framework.cluster.TestRestClient;
import org.opensearch.test.framework.cluster.TestRestClient.HttpResponse;
import org.opensearch.transport.client.Client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.opensearch.test.framework.TestSecurityConfig.AuthcDomain.AUTHC_HTTPBASIC_INTERNAL;
import static org.opensearch.test.framework.TestSecurityConfig.User.USER_ADMIN;

public class CircuitBreakerAuditIntegrationTest {
    @ClassRule
    public static final LocalCluster cluster = new LocalCluster.Builder().clusterManager(ClusterManager.SINGLENODE)
        .anonymousAuth(false)
        .authc(AUTHC_HTTPBASIC_INTERNAL)
        .users(USER_ADMIN)
        .audit(new AuditConfiguration(true).filters(new AuditFilters().enabledRest(true).enabledTransport(true)))
        .build();

    @Rule
    public AuditLogsRule auditLogsRule = new AuditLogsRule();

    @Test
    public void auditsHttpInflightTripBeforeSecurityHandler() throws Exception {
        try (TestRestClient client = cluster.getRestClient(USER_ADMIN); Client nodeClient = cluster.getInternalNodeClient()) {
            setLimit(nodeClient, "network.breaker.inflight_requests.limit", "1b");
            try {
                HttpResponse response = client.postJson(
                    "_search",
                    "{\"query\":{\"match_all\":{}}}",
                    new BasicHeader("X-Request-Id", "trip")
                );
                assertThat(response.getBody(), response.getStatusCode(), equalTo(429));
                assertThat(response.getBody(), containsString("circuit_breaking_exception"));
            } finally {
                setLimit(nodeClient, "network.breaker.inflight_requests.limit", null);
            }

            auditLogsRule.assertAtLeast(
                1,
                message -> message.getCategory() == AuditCategory.CIRCUIT_BREAKER_TRIPPED
                    && "in_flight_requests".equals(message.getAsMap().get(AuditMessage.BREAKER_NAME))
                    && Long.valueOf(1).equals(message.getAsMap().get(AuditMessage.BREAKER_BYTES_LIMIT))
            );
        }
    }

    @Test
    public void auditsRequestBreakerTripAfterAuthorization() throws Exception {
        try (TestRestClient client = cluster.getRestClient(USER_ADMIN); Client nodeClient = cluster.getInternalNodeClient()) {
            HttpResponse indexed = client.putJson("breaker-investigation/_doc/1?refresh=true", "{\"value\":\"a\"}");
            assertThat(indexed.getBody(), indexed.getStatusCode(), equalTo(201));
            setLimit(nodeClient, "indices.breaker.request.limit", "0b");
            try {
                HttpResponse response = client.postJson(
                    "breaker-investigation/_search?allow_partial_search_results=false",
                    "{\"size\":0,\"aggs\":{\"values\":{\"terms\":{\"field\":\"value.keyword\"}}}}",
                    new BasicHeader("X-Request-Id", "query-trip")
                );
                assertThat(response.getBody(), response.getStatusCode(), greaterThanOrEqualTo(400));
                assertThat(response.getBody(), containsString("circuit_breaking_exception"));
            } finally {
                setLimit(nodeClient, "indices.breaker.request.limit", null);
            }
            auditLogsRule.assertAtLeast(
                1,
                message -> message.getCategory() == AuditCategory.CIRCUIT_BREAKER_TRIPPED
                    && "request".equals(message.getAsMap().get(AuditMessage.BREAKER_NAME))
                    && "query-trip".equals(message.getAsMap().get(AuditMessage.REQUEST_ID))
            );
            auditLogsRule.assertAtLeast(
                1,
                message -> message.getCategory() == AuditCategory.GRANTED_PRIVILEGES
                    && "query-trip".equals(message.getAsMap().get(AuditMessage.REQUEST_ID))
            );
        }
    }

    private static void setLimit(Client client, String key, String limit) {
        Settings.Builder settings = Settings.builder();
        if (limit == null) {
            settings.putNull(key);
        } else {
            settings.put(key, limit);
        }
        client.admin().cluster().prepareUpdateSettings().setTransientSettings(settings).get();
    }
}
