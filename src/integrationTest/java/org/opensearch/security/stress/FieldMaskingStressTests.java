/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 */
package org.opensearch.security.stress;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.metrics.ParsedCardinality;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.test.framework.AsyncActions;
import org.opensearch.test.framework.TestSecurityConfig;
import org.opensearch.test.framework.cluster.ClusterManager;
import org.opensearch.test.framework.cluster.LocalCluster;
import org.opensearch.test.framework.cluster.TestRestClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.opensearch.client.RequestOptions.DEFAULT;
import static org.opensearch.security.Song.FIELD_TITLE;
import static org.opensearch.test.framework.TestSecurityConfig.AuthcDomain.AUTHC_HTTPBASIC_INTERNAL;
import static org.opensearch.test.framework.TestSecurityConfig.Role.ALL_ACCESS;

@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class FieldMaskingStressTests {
    private final static Logger LOG = LogManager.getLogger(FieldMaskingStressTests.class);
    private static final long BYTE_TO_MB_CONVERSION_VALUE = 1024 * 1024;
    private static final TestSecurityConfig.User ADMIN_USER = new TestSecurityConfig.User("admin").roles(ALL_ACCESS);

    /**
     * User who is allowed to see all fields on indices songs except artist
     * <ul>
     *     <li>values of the artist fields should be masked on index songs</li>
     * </ul>
     */
    static final TestSecurityConfig.User MASKED_TITLE_READER = new TestSecurityConfig.User("masked_title_reader").roles(
        new TestSecurityConfig.Role("masked_title_reader").clusterPermissions("cluster_composite_ops_ro")
            .indexPermissions("read")
            .maskedFields(FIELD_TITLE)
            .on("songs")
    );

    private String bar = "bar".repeat(100);
    private String TEST_DOC = "{\"artist\": \"foo\", \"title\": \"" + bar + "\"}";

    @ClassRule
    public static LocalCluster cluster = new LocalCluster.Builder().clusterManager(ClusterManager.THREE_CLUSTER_MANAGERS)
        .authc(AUTHC_HTTPBASIC_INTERNAL)
        .users(ADMIN_USER, MASKED_TITLE_READER)
        .anonymousAuth(false)
        .doNotFailOnForbidden(true)
        .build();

    @Test
    public void testCardinalityAggregationWithFieldMaskingOnLargeIndex() throws IOException {
        final int totalNumberOfDocs = 10000;

        try (TestRestClient client = cluster.getRestClient(ADMIN_USER)) {
            final var requests = AsyncActions.generate(() -> {
                TestRestClient.HttpResponse indexDocResponse = client.postJson("songs/_doc", TEST_DOC);
                return indexDocResponse.getStatusCode();
            }, 10, totalNumberOfDocs);

            AsyncActions.getAll(requests, 2, TimeUnit.MINUTES)
                .forEach((responseCode) -> { assertThat(responseCode, equalTo(HttpStatus.SC_CREATED)); });
        }

        System.gc();
        long memoryUsageBeforeLoadingData = getCurrentlyUsedMemory();
        System.out.println("Used memory before loading some data: " + memoryUsageBeforeLoadingData + " MB");
        try (RestHighLevelClient restHighLevelClient = cluster.getRestHighLevelClient(MASKED_TITLE_READER)) {
            SearchRequest searchRequest = new SearchRequest("songs");
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.aggregation(AggregationBuilders.cardinality("unique_titles_agg").field("title.keyword").precisionThreshold(100));
            sourceBuilder.size(1);
            searchRequest.source(sourceBuilder);
            SearchResponse response = restHighLevelClient.search(searchRequest, DEFAULT);
            System.out.println("SearchResponse: " + response);
            ParsedCardinality parsedCardinality = response.getAggregations().get("unique_titles_agg");
            long cardinality = parsedCardinality.getValue();
            // assertThat(cardinality, equalTo(1L));
        }
        long memoryUsageAfterLoadingData = getCurrentlyUsedMemory();
        System.out.println("Used memory after loading some data: " + memoryUsageAfterLoadingData + " MB");
        System.out.println("Difference: " + (memoryUsageAfterLoadingData - memoryUsageBeforeLoadingData) + " MB");
    }

    private long getCurrentlyUsedMemory() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / BYTE_TO_MB_CONVERSION_VALUE;
    }
}
