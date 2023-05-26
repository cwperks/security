/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.ccstest;

import org.apache.hc.core5.http.HttpStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.support.WriteRequest.RefreshPolicy;
import org.opensearch.client.Client;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.security.test.AbstractSecurityUnitTest;
import org.opensearch.security.test.helper.cluster.ClusterConfiguration;
import org.opensearch.security.test.helper.cluster.ClusterHelper;
import org.opensearch.security.test.helper.cluster.ClusterInfo;
import org.opensearch.security.test.helper.rest.RestHelper;
import org.opensearch.security.test.helper.rest.RestHelper.HttpResponse;

public class RemoteReindexTests extends AbstractSecurityUnitTest {

    private final ClusterHelper cl1 = new ClusterHelper(
        "crl1_n" + num.incrementAndGet() + "_f" + System.getProperty("forkno") + "_t" + System.nanoTime()
    );
    private final ClusterHelper cl2 = new ClusterHelper(
        "crl2_n" + num.incrementAndGet() + "_f" + System.getProperty("forkno") + "_t" + System.nanoTime()
    );
    private ClusterInfo cl1Info;
    private ClusterInfo cl2Info;

    private void setupReindex() throws Exception {

        System.setProperty("security.display_lic_none", "true");

        cl2Info = cl2.startCluster(minimumSecuritySettings(Settings.EMPTY), ClusterConfiguration.DEFAULT);
        initialize(cl2, cl2Info);

        cl1Info = cl1.startCluster(minimumSecuritySettings(crossClusterNodeSettings(cl2Info)), ClusterConfiguration.DEFAULT);
        initialize(cl1, cl1Info);
    }

    @After
    public void tearDown() throws Exception {
        cl1.stopCluster();
        cl2.stopCluster();
    }

    private Settings crossClusterNodeSettings(ClusterInfo remote) {
        Settings.Builder builder = Settings.builder().putList("reindex.remote.whitelist", remote.httpHost + ":" + remote.httpPort);
        return builder.build();
    }

    // TODO add ssl tests
    // https://github.com/elastic/elasticsearch/issues/27267

    @Test
    public void testNonSSLReindex() throws Exception {
        setupReindex();

        final String cl1BodyMain = new RestHelper(cl1Info, false, false, getResourceFolder()).executeGetRequest(
            "",
            encodeBasicHeader("nagilum", "nagilum")
        ).getBody();
        Assert.assertTrue(cl1BodyMain.contains("crl1"));

        try (Client tc = cl1.nodeClient()) {
            tc.admin().indices().create(new CreateIndexRequest("twutter")).actionGet();
        }

        final String cl2BodyMain = new RestHelper(cl2Info, false, false, getResourceFolder()).executeGetRequest(
            "",
            encodeBasicHeader("nagilum", "nagilum")
        ).getBody();
        Assert.assertTrue(cl2BodyMain.contains("crl2"));

        try (Client tc = cl2.nodeClient()) {
            tc.index(
                new IndexRequest("twitter").setRefreshPolicy(RefreshPolicy.IMMEDIATE)
                    .id("0")
                    .source("{\"cluster\": \"" + cl1Info.clustername + "\"}", XContentType.JSON)
            ).actionGet();
        }

        String reindex = "{"
            + "\"source\": {"
            + "\"remote\": {"
            + "\"host\": \"http://"
            + cl2Info.httpHost
            + ":"
            + cl2Info.httpPort
            + "\","
            + "\"username\": \"nagilum\","
            + "\"password\": \"nagilum\""
            + "},"
            + "\"index\": \"twitter\","
            + "\"size\": 10"
            + "},"
            + "\"dest\": {"
            + "\"index\": \"twutter\""
            + "}"
            + "}";

        System.out.println(reindex);

        HttpResponse ccs = null;

        System.out.println("###################### reindex");
        ccs = new RestHelper(cl1Info, false, false, getResourceFolder()).executePostRequest(
            "_reindex?pretty",
            reindex,
            encodeBasicHeader("nagilum", "nagilum")
        );
        System.out.println(ccs.getBody());
        Assert.assertEquals(HttpStatus.SC_OK, ccs.getStatusCode());
        Assert.assertTrue(ccs.getBody().contains("created\" : 1"));
    }
}
