/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.bwc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.function.Factory;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.reactor.ssl.TlsDetails;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.opensearch.client.Response;
import org.opensearch.client.ResponseException;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.common.util.io.IOUtils;
import org.opensearch.security.bwc.helper.TestHelper;
import org.opensearch.test.rest.OpenSearchRestTestCase;

import org.opensearch.Version;

import static org.hamcrest.Matchers.hasItem;

import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;

import org.junit.Assert;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

public class SecurityBackwardsCompatibilityIT extends OpenSearchRestTestCase {

    private ClusterType CLUSTER_TYPE;
    private String CLUSTER_NAME;

    private static final String TEST_USER = "user";
    private static final String TEST_PASSWORD = UUID.randomUUID().toString();

    private static final String TEST_ROLE = "test-dls-fls-role";

    private RestClient testUserAuthClient;

    private int bulkIndexCounter = 1;

    @Before
    private void testSetup() {
        final String bwcsuiteString = System.getProperty("tests.rest.bwcsuite");
        Assume.assumeTrue("Test cannot be run outside the BWC gradle task 'bwcTestSuite' or its dependent tasks", bwcsuiteString != null);
        CLUSTER_TYPE = ClusterType.parse(bwcsuiteString);
        CLUSTER_NAME = System.getProperty("tests.clustername");
        testUserAuthClient = buildClient(restClientSettings(), getClusterHosts().toArray(new HttpHost[0]), TEST_USER, TEST_PASSWORD);
    }

    @Override
    protected final boolean preserveClusterUponCompletion() {
        return true;
    }

    @Override
    protected final boolean preserveIndicesUponCompletion() {
        return true;
    }

    @Override
    protected final boolean preserveReposUponCompletion() {
        return true;
    }

    @Override
    protected boolean preserveTemplatesUponCompletion() {
        return true;
    }

    @Override
    protected String getProtocol() {
        return "https";
    }

    @Override
    protected final Settings restClientSettings() {
        return Settings.builder()
            .put(super.restClientSettings())
            // increase the timeout here to 90 seconds to handle long waits for a green
            // cluster health. the waits for green need to be longer than a minute to
            // account for delayed shards
            .put(OpenSearchRestTestCase.CLIENT_SOCKET_TIMEOUT, "90s")
            .build();
    }

    protected RestClient buildClient(Settings settings, HttpHost[] hosts, String username, String password) {
        RestClientBuilder builder = RestClient.builder(hosts);
        configureHttpsClient(builder, settings, username, password);
        boolean strictDeprecationMode = settings.getAsBoolean("strictDeprecationMode", true);
        builder.setStrictDeprecationMode(strictDeprecationMode);
        return builder.build();
    }

    @Override
    protected RestClient buildClient(Settings settings, HttpHost[] hosts) {
        String userName = Optional.ofNullable(System.getProperty("tests.opensearch.username"))
            .orElseThrow(() -> new RuntimeException("user name is missing"));
        String password = Optional.ofNullable(System.getProperty("tests.opensearch.password"))
            .orElseThrow(() -> new RuntimeException("password is missing"));
        return buildClient(settings, hosts, userName, password);
    }

    protected static void configureHttpsClient(RestClientBuilder builder, Settings settings, String userName, String password) {
        Map<String, String> headers = ThreadContext.buildDefaultHeaders(settings);
        Header[] defaultHeaders = new Header[headers.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            defaultHeaders[i++] = new BasicHeader(entry.getKey(), entry.getValue());
        }
        builder.setDefaultHeaders(defaultHeaders);
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(null, -1), new UsernamePasswordCredentials(userName, password.toCharArray()));
            try {
                SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(null, (chains, authType) -> true).build();

                TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
                    .setSslContext(sslContext)
                    .setTlsVersions(new String[] { "TLSv1", "TLSv1.1", "TLSv1.2", "SSLv3" })
                    .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    // See please https://issues.apache.org/jira/browse/HTTPCLIENT-2219
                    .setTlsDetailsFactory(new Factory<SSLEngine, TlsDetails>() {
                        @Override
                        public TlsDetails create(final SSLEngine sslEngine) {
                            return new TlsDetails(sslEngine.getSession(), sslEngine.getApplicationProtocol());
                        }
                    })
                    .build();

                final AsyncClientConnectionManager cm = PoolingAsyncClientConnectionManagerBuilder.create()
                    .setTlsStrategy(tlsStrategy)
                    .build();
                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider).setConnectionManager(cm);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void testBasicBackwardsCompatibility() throws Exception {
        String round = System.getProperty("tests.rest.bwcsuite_round");

        if (round.equals("first") || round.equals("old")) {
            assertPluginUpgrade("_nodes/" + CLUSTER_NAME + "-0/plugins");
        } else if (round.equals("second")) {
            assertPluginUpgrade("_nodes/" + CLUSTER_NAME + "-1/plugins");
        } else if (round.equals("third")) {
            assertPluginUpgrade("_nodes/" + CLUSTER_NAME + "-2/plugins");
        }
    }

    public void testDataIngestionAndSearchBackwardsCompatibility() throws Exception {
        String round = System.getProperty("tests.rest.bwcsuite_round");
        String index = String.format("test_index-%s", round);
        logger.info("Test details - Cluster Type {} Cluster Name {} Round {}", CLUSTER_TYPE, CLUSTER_NAME, round);
        logger.info("before : user exists: {}, role exists: {}", userExists(), roleExists());
        if (round.equals("first") || round.equals("old")) {
            createDLSFLSTestRole();
            createUser();
            createIndex(index);
        }
        logger.info("after : user exists: {}, role exists: {}", userExists(), roleExists());
        ingestData(index);
        searchMatchAll(index);
    }

    @SuppressWarnings("unchecked")
    public void testWhoAmI() throws Exception {
        Map<String, Object> responseMap = (Map<String, Object>) getAsMap("_plugins/_security/whoami");
        Assert.assertTrue(responseMap.containsKey("dn"));
    }

    private enum ClusterType {
        OLD,
        MIXED,
        UPGRADED;

        public static ClusterType parse(String value) {
            switch (value) {
                case "old_cluster":
                    return OLD;
                case "mixed_cluster":
                    return MIXED;
                case "upgraded_cluster":
                    return UPGRADED;
                default:
                    throw new AssertionError("unknown cluster type: " + value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void assertPluginUpgrade(String uri) throws Exception {
        Map<String, Map<String, Object>> responseMap = (Map<String, Map<String, Object>>) getAsMap(uri).get("nodes");
        for (Map<String, Object> response : responseMap.values()) {
            List<Map<String, Object>> plugins = (List<Map<String, Object>>) response.get("plugins");
            Set<String> pluginNames = plugins.stream().map(map -> (String) map.get("name")).collect(Collectors.toSet());

            final Version minNodeVersion = this.minimumNodeVersion();

            if (minNodeVersion.major <= 1) {
                assertThat(pluginNames, hasItem("opensearch_security"));
            } else {
                assertThat(pluginNames, hasItem("opensearch-security"));
            }

        }
    }

    private void createIndex(String index) throws IOException {
        // String settings = "{\n" +
        // " \"settings\": {\n" +
        // " \"index\": {\n" +
        // " \"number_of_shards\": 3,\n" +
        // " \"number_of_replicas\": 1\n" +
        // " }\n" +
        // " },\n" +
        // " \"mappings\": {\n" +
        // " \"properties\": {\n" +
        // " \"age\": {\n" +
        // " \"type\": \"integer\"\n" +
        // " }\n" +
        // " }\n" +
        // " },\n" +
        // " \"aliases\": {\n" +
        // " \"sample-alias1\": {}\n" +
        // " }\n" +
        // "}";
        Response response = TestHelper.makeRequest(client(), "PUT", index, null, TestHelper.toHttpEntity("{}"), null, false);
        logger.info(response.getStatusLine());
    }

    private void ingestData(String index) throws IOException {
        StringBuilder bulkRequestBody = new StringBuilder();
        ObjectMapper objectMapper = new ObjectMapper();
        for (Song song : Song.SONGS) {
            Map<String, Map<String, String>> indexRequest = new HashMap<>();
            indexRequest.put("index", new HashMap<>() {
                {
                    put("_index", index);
                    put("_id", "" + bulkIndexCounter++);
                }
            });
            bulkRequestBody.append(String.format("%s\n", objectMapper.writeValueAsString(indexRequest)));
            bulkRequestBody.append(String.format("%s\n", objectMapper.writeValueAsString(song.asJson())));
        }

        Response response = TestHelper.makeRequest(
            testUserAuthClient,
            "POST",
            "_bulk",
            null,
            TestHelper.toHttpEntity(bulkRequestBody.toString()),
            // Collections.singletonList(encodeBasicHeader(TEST_USER, TEST_PASSWORD)),
            null,
            false
        );

        Assert.assertEquals(200, response.getStatusLine().getStatusCode());

        logger.info("Ingested data");
    }

    private void searchMatchAll(String index) throws IOException {
        String matchAllQuery = "{\n" + "    \"query\": {\n" + "        \"match_all\": {}\n" + "    }\n" + "}";

        String url = String.format("%s/_search", index);

        Response response = TestHelper.makeRequest(
            testUserAuthClient,
            "POST",
            url,
            null,
            TestHelper.toHttpEntity(matchAllQuery),
            // Collections.singletonList(encodeBasicHeader(TEST_USER, TEST_PASSWORD)),
            null,
            false
        );

        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    }

    private void createDLSFLSTestRole() throws IOException {
        String url = String.format("_plugins/_security/api/roles/%s", TEST_ROLE);
        String roleSettings =
            "{\"cluster_permissions\":[\"unlimited\"],\"index_permissions\":[{\"index_patterns\":[\"test_index*\"],\"dls\":\"{\\n        \\\"bool\\\": {\\n                \\\"must\\\": {\\n                        \\\"match\\\": {\\n                                \\\"genre\\\": \\\"rock\\\"\\n                        }\\n                }\\n        }\\n }\",\"fls\":[\"~lyrics\"],\"masked_fields\":[\"artist\"],\"allowed_actions\":[\"read\", \"write\"]}],\"tenant_permissions\":[]}";
        Response response = TestHelper.makeRequest(adminClient(), "PUT", url, null, TestHelper.toHttpEntity(roleSettings), null, false);

        int statusCode = response.getStatusLine().getStatusCode();
        Assert.assertTrue(statusCode == 200 || statusCode == 201);
    }

    private boolean resourceExists(String url) throws IOException {
        try {
            Response r = TestHelper.get(adminClient(), url);
            logger.info("GET {} \n {}", url, new String(r.getEntity().getContent().readAllBytes()));
            return true;
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() == 404) {
                return false;
            } else {
                throw e;
            }
        }
    }

    private boolean roleExists() throws IOException {
        return resourceExists(String.format("_plugins/_security/api/roles/%s", TEST_ROLE));
    }

    private boolean userExists() throws IOException {
        return resourceExists(String.format("_plugins/_security/api/internalusers/%s", TEST_USER));
    }

    private void createUser() throws IOException {
        if (!userExists()) {
            String url = String.format("_plugins/_security/api/internalusers/%s", TEST_USER);
            String userSettings = String.format(
                "{\n" + "  \"password\": \"%s\",\n" + "  \"opendistro_security_roles\": [\"%s\"],\n" + "  \"backend_roles\": []\n" + "}",
                TEST_PASSWORD,
                TEST_ROLE
            );
            Response response = TestHelper.makeRequest(adminClient(), "PUT", url, null, TestHelper.toHttpEntity(userSettings), null, false);
            assertEquals(201, response.getStatusLine().getStatusCode());
        }
    }

    @After
    public void cleanUp() throws IOException {
        IOUtils.close(testUserAuthClient);
    }

    public static Header encodeBasicHeader(final String username, final String password) {
        return new BasicHeader(
            "Authorization",
            "Basic "
                + Base64.getEncoder().encodeToString((username + ":" + Objects.requireNonNull(password)).getBytes(StandardCharsets.UTF_8))
        );
    }
}