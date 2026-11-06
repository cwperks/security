/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.security.http;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.ClassRule;
import org.junit.Test;

import org.opensearch.test.framework.TestSecurityConfig.Role;
import org.opensearch.test.framework.TestSecurityConfig.Tenant;
import org.opensearch.test.framework.TestSecurityConfig.User;
import org.opensearch.test.framework.cluster.ClusterManager;
import org.opensearch.test.framework.cluster.LocalCluster;

import io.netty.channel.Channel;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import static org.opensearch.test.framework.TestSecurityConfig.AuthcDomain.AUTHC_HTTPBASIC_INTERNAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class HttpKeepAliveTests {
    private static final String AUTH_INFO = "/_plugins/_security/authinfo";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final User ALICE = new User("alice").roles(
        new Role("alice_role").clusterPermissions("cluster:monitor/health").tenantPermissions("kibana_all_read").on("team")
    );
    private static final User BOB = new User("bob").roles(new Role("bob_role"));

    @ClassRule
    public static final LocalCluster cluster = new LocalCluster.Builder().clusterManager(ClusterManager.SINGLENODE)
        .anonymousAuth(false)
        .authc(AUTHC_HTTPBASIC_INTERNAL)
        .users(ALICE, BOB)
        .tenants(new Tenant("team"))
        .build();

    @Test
    public void rejectedCredentialsDoNotAffectNextRequest() throws Exception {
        try (KeepAliveClient client = new KeepAliveClient()) {
            client.get(AUTH_INFO, basicAuth(ALICE.getName(), "wrong-password"), null, 401);
            assertUser(client.authInfo(ALICE), ALICE);
        }
    }

    @Test
    public void missingCredentialsDoNotReusePreviousUser() throws Exception {
        try (KeepAliveClient client = new KeepAliveClient()) {
            assertUser(client.authInfo(ALICE), ALICE);
            client.get(AUTH_INFO, null, null, 401);
            assertUser(client.authInfo(ALICE), ALICE);
        }
    }

    @Test
    public void eachRequestUsesItsOwnUserAndRoles() throws Exception {
        try (KeepAliveClient client = new KeepAliveClient()) {
            assertUser(client.authInfo(ALICE), ALICE);
            assertUser(client.authInfo(BOB), BOB);
            assertUser(client.authInfo(ALICE), ALICE);
        }
    }

    @Test
    public void permissionsDoNotCarryOverBetweenUsers() {
        try (KeepAliveClient client = new KeepAliveClient()) {
            client.get("/_cluster/health", basicAuth(ALICE), null, 200);
            client.get("/_cluster/health", basicAuth(BOB), null, 403);
            client.get("/_cluster/health", basicAuth(ALICE), null, 200);
        }
    }

    @Test
    public void tenantDoesNotCarryOverToNextRequest() throws Exception {
        try (KeepAliveClient client = new KeepAliveClient()) {
            JsonNode withTenant = MAPPER.readTree(client.get(AUTH_INFO, basicAuth(ALICE), "team", 200));
            assertEquals("team", withTenant.get("user_requested_tenant").asText());
            JsonNode withoutTenant = client.authInfo(ALICE);
            assertUser(withoutTenant, ALICE);
            assertTrue(withoutTenant.get("user_requested_tenant").isNull());
        }
    }

    private static void assertUser(JsonNode response, User user) {
        assertEquals(user.getName(), response.get("user_name").asText());
        assertEquals(MAPPER.valueToTree(user.getRoleNames()), response.get("roles"));
    }

    private static String basicAuth(User user) {
        return basicAuth(user.getName(), user.getPassword());
    }

    private static String basicAuth(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private static class KeepAliveClient implements AutoCloseable {
        private final ConnectionProvider connections = ConnectionProvider.builder("keep-alive-isolation").maxConnections(1).build();
        private final AtomicReference<Channel> requestChannel = new AtomicReference<>();
        private final HttpClient client = HttpClient.create(connections)
            .protocol(HttpProtocol.HTTP11)
            .remoteAddress(cluster::getHttpAddress)
            .secure(
                ssl -> ssl.sslContext(
                    Http11SslContextSpec.forClient().configure(ctx -> ctx.trustManager(InsecureTrustManagerFactory.INSTANCE))
                )
            )
            .disableRetry(true)
            .doOnRequest((request, connection) -> requestChannel.set(connection.channel()));
        private Channel firstChannel;

        JsonNode authInfo(User user) throws Exception {
            return MAPPER.readTree(get(AUTH_INFO, basicAuth(user), null, 200));
        }

        String get(String path, String authorization, String tenant, int expectedStatus) {
            AtomicReference<Integer> status = new AtomicReference<>();
            // Consume the whole response before issuing the next request on this connection.
            String body = client.headers(headers -> {
                if (authorization != null) headers.set("Authorization", authorization);
                if (tenant != null) headers.set("securitytenant", tenant);
            }).get().uri(path).responseSingle((response, content) -> {
                status.set(response.status().code());
                return content.asString(StandardCharsets.UTF_8).defaultIfEmpty("");
            }).block(Duration.ofSeconds(30));
            assertEquals(expectedStatus, status.get().intValue());
            assertNotNull(requestChannel.get());
            if (firstChannel == null) {
                firstChannel = requestChannel.get();
            } else {
                // A pool size of one alone would still allow reconnects, hiding request-state leaks.
                assertSame("Requests must reuse the same physical connection", firstChannel, requestChannel.get());
            }
            return body;
        }

        @Override
        public void close() {
            connections.disposeLater().block(Duration.ofSeconds(30));
        }
    }
}
