/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.security.sandbox;

import java.util.List;
import java.util.Map;

import org.apache.hc.core5.http.HttpHost;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.ResponseException;
import org.opensearch.client.RestClient;
import org.opensearch.commons.rest.SecureRestClientBuilder;
import org.opensearch.security.sanity.tests.SecurityRestTestCase;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@SuppressWarnings("unchecked")
public class SandboxAnalyticsSecurityIT extends SecurityRestTestCase {

    private static final String ALLOWED_INDEX = "movies_allowed";
    private static final String DENIED_INDEX = "movies_denied";
    private static final String USER = "movies_reader";
    private static final String PASSWORD = "myStrongPassword123!";
    private static final String ROLE = "sandbox_analytics_movies_reader";

    @Override
    protected boolean preserveClusterUponCompletion() {
        return true;
    }

    @Test
    public void testPplIndexAuthorization() throws Exception {
        try (RestClient adminClient = basicAdminClient()) {
            configureLimitedUser(adminClient);
            createMovieIndex(adminClient, ALLOWED_INDEX);
            createMovieIndex(adminClient, DENIED_INDEX);
        }

        try (RestClient limitedClient = limitedClient()) {
            Map<String, Object> allowed = executePpl(limitedClient, "source = " + ALLOWED_INDEX + " | stats count() as c");

            List<List<Object>> rows = (List<List<Object>>) allowed.get("rows");
            MatcherAssert.assertThat(rows.size(), is(equalTo(1)));
            MatcherAssert.assertThat(((Number) rows.get(0).get(0)).intValue(), is(equalTo(2)));

            ResponseException denied = expectThrows(
                ResponseException.class,
                () -> executePpl(limitedClient, "source = " + DENIED_INDEX + " | stats count() as c")
            );
            MatcherAssert.assertThat(denied.getResponse().getStatusLine().getStatusCode(), is(equalTo(403)));
        }
    }

    private void configureLimitedUser(RestClient adminClient) throws Exception {
        putJson(adminClient, "/_plugins/_security/api/roles/" + ROLE, """
            {
              "cluster_permissions": ["cluster:internal/qe/unified_ppl_execute"],
              "index_permissions": [
                {
                  "index_patterns": ["%s"],
                  "allowed_actions": ["read"]
                }
              ],
              "tenant_permissions": []
            }
            """.formatted(ALLOWED_INDEX));
        putJson(adminClient, "/_plugins/_security/api/internalusers/" + USER, """
            {
              "password": "%s",
              "backend_roles": [],
              "attributes": {}
            }
            """.formatted(PASSWORD));
        putJson(adminClient, "/_plugins/_security/api/rolesmapping/" + ROLE, """
            {
              "users": ["%s"],
              "backend_roles": [],
              "hosts": []
            }
            """.formatted(USER));
    }

    private void createMovieIndex(RestClient adminClient, String indexName) throws Exception {
        ignoreNotFound(adminClient, new Request("DELETE", "/" + indexName));

        putJson(adminClient, "/" + indexName, """
            {
              "settings": {
                "number_of_shards": 1,
                "number_of_replicas": 0,
                "index.pluggable.dataformat.enabled": true,
                "index.pluggable.dataformat": "composite",
                "index.composite.primary_data_format": "parquet",
                "index.composite.secondary_data_formats": "lucene"
              },
              "mappings": {
                "properties": {
                  "title": {
                    "type": "keyword"
                  },
                  "rating": {
                    "type": "integer"
                  }
                }
              }
            }
            """);

        Request bulk = new Request("POST", "/_bulk");
        bulk.addParameter("refresh", "true");
        bulk.setJsonEntity("""
            {"index":{"_index":"%s","_id":"1"}}
            {"title":"Alien","rating":5}
            {"index":{"_index":"%s","_id":"2"}}
            {"title":"Arrival","rating":4}
            """.formatted(indexName, indexName));
        assertOK(adminClient.performRequest(bulk));
    }

    private Map<String, Object> executePpl(RestClient restClient, String query) throws Exception {
        Request request = new Request("POST", "/_analytics/ppl");
        request.setJsonEntity("""
            {
              "query": "%s"
            }
            """.formatted(query));
        Response response = restClient.performRequest(request);
        MatcherAssert.assertThat(response.getStatusLine().getStatusCode(), is(equalTo(200)));
        return entityAsMap(response);
    }

    private void putJson(RestClient restClient, String endpoint, String body) throws Exception {
        Request request = new Request("PUT", endpoint);
        request.setJsonEntity(body);
        Response response = restClient.performRequest(request);
        int statusCode = response.getStatusLine().getStatusCode();
        MatcherAssert.assertThat(statusCode >= 200 && statusCode < 300, is(true));
    }

    private void ignoreNotFound(RestClient restClient, Request request) throws Exception {
        try {
            restClient.performRequest(request);
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() != 404) {
                throw e;
            }
        }
    }

    private RestClient basicAdminClient() throws Exception {
        return secureClient(System.getProperty("user"), System.getProperty("password"));
    }

    private RestClient limitedClient() throws Exception {
        return secureClient(USER, PASSWORD);
    }

    private RestClient secureClient(String user, String password) throws Exception {
        List<HttpHost> hosts = getClusterHosts();
        boolean https = Boolean.parseBoolean(System.getProperty("https"));
        return new SecureRestClientBuilder(hosts.toArray(new HttpHost[0]), https, user, password).setSocketTimeout(60000)
            .setConnectionRequestTimeout(180000)
            .build();
    }
}
