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

package org.opensearch.security.dlic.dlsfls;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.support.WriteRequest.RefreshPolicy;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.test.helper.rest.RestHelper.HttpResponse;
import org.opensearch.transport.client.Client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class FieldMaskedTest extends AbstractDlsFlsTest {

    protected void populateData(Client tc) {

        tc.index(
            new IndexRequest("deals").id("0")
                .setRefreshPolicy(RefreshPolicy.IMMEDIATE)
                .source(
                    "{\"customer\": {\"name\":\"cust1\"}, \"ip_source\": \"100.100.1.1\",\"ip_dest\": \"123.123.1.1\",\"amount\": 10}",
                    XContentType.JSON
                )
        ).actionGet();
        tc.index(
            new IndexRequest("deals").id("2")
                .setRefreshPolicy(RefreshPolicy.IMMEDIATE)
                .source(
                    "{\"customer\": {\"name\":\"cust2\"}, \"ip_source\": \"100.100.2.2\",\"ip_dest\": \"123.123.2.2\",\"amount\": 20}",
                    XContentType.JSON
                )
        ).actionGet();

        for (int i = 0; i < 30; i++) {
            tc.index(
                new IndexRequest("deals").id("a" + i)
                    .setRefreshPolicy(RefreshPolicy.IMMEDIATE)
                    .source(
                        "{\"customer\": {\"name\":\"cust1\"}, \"ip_source\": \"200.100.1.1\",\"ip_dest\": \"123.123.1.1\",\"amount\": 10}",
                        XContentType.JSON
                    )
            ).actionGet();
        }

    }

    @Test
    public void testMaskedAggregations() throws Exception {

        setup();
        String query;
        HttpResponse res;

        query = "{"
            + "\"query\" : {"
            + "\"match_all\": {}"
            + "},"
            + "\"aggs\" : {"
            + "\"ips\" : { \"terms\" : { \"field\" : \"ip_source.keyword\" } }"
            + "}"
            + "}";

        // assertThat(HttpStatus.SC_OK, (res = rh.executePostRequest("/deals/_search?pretty&size=0", query,
        // encodeBasicHeader("admin", "admin"))).getStatusCode());
        // Asseis(rt.assertTrue(res.getBody().contains("100.100"));)

        assertThat(
            HttpStatus.SC_OK,
            is(
                (res = rh.executePostRequest("/deals/_search?pretty&size=0", query, encodeBasicHeader("user_masked", "password")))
                    .getStatusCode()
            )
        );
        Assert.assertFalse(res.getBody().contains("100.100"));

        query = "{"
            + "\"query\" : {"
            + "\"match_all\": {"
            + "}"
            + "},"
            + "\"aggs\": {"
            + "\"ips\" : {"
            + "\"terms\" : {"
            + "\"field\" : \"ip_source.keyword\","
            + "\"order\": {"
            + "\"_term\" : \"asc\""
            + "}"
            + "}"
            + "}"
            + "}"
            + "}";

        assertThat(
            HttpStatus.SC_OK,
            is(
                (res = rh.executePostRequest("/deals/_search?pretty&size=0", query, encodeBasicHeader("user_masked", "password")))
                    .getStatusCode()
            )
        );
        Assert.assertFalse(res.getBody().contains("100.100"));

        query = "{"
            + "\"query\" : {"
            + "\"match_all\": {"
            + "}"
            + "},"
            + "\"aggs\": {"
            + "\"ips\" : {"
            + "\"terms\" : {"
            + "\"field\" : \"ip_source.keyword\","
            + "\"order\": {"
            + "\"_term\" : \"desc\""
            + "}"
            + "}"
            + "}"
            + "}"
            + "}";

        assertThat(
            HttpStatus.SC_OK,
            is(
                (res = rh.executePostRequest("/deals/_search?pretty&size=0", query, encodeBasicHeader("user_masked", "password")))
                    .getStatusCode()
            )
        );
        Assert.assertFalse(res.getBody().contains("100.100"));
    }

    @Test
    public void testMaskedAggregationsRace() throws Exception {

        setup();

        String query = "{"
            + "\"aggs\" : {"
            + "\"ips\" : { \"terms\" : { \"field\" : \"ip_source.keyword\", \"size\": 1002, \"show_term_doc_count_error\": true } }"
            + "}"
            + "}";

        HttpResponse res;
        assertThat(
            HttpStatus.SC_OK,
            is((res = rh.executePostRequest("/deals/_search?pretty&size=0", query, encodeBasicHeader("admin", "admin"))).getStatusCode())
        );
        Assert.assertTrue(res.getBody().contains("100.100"));
        Assert.assertTrue(res.getBody().contains("200.100"));
        Assert.assertTrue(res.getBody().contains("\"doc_count\" : 30"));
        Assert.assertTrue(res.getBody().contains("\"doc_count\" : 1"));
        Assert.assertFalse(res.getBody().contains("e1623afebfa505884e249a478640ec98094d19a72ac7a89dd0097e28955bb5ae"));
        Assert.assertFalse(res.getBody().contains("26a8671e57fefc13504f8c61ced67ac98338261ace1e5bf462038b2f2caae16e"));
        Assert.assertFalse(res.getBody().contains("87873bdb698e5f0f60e0b02b76dad1ec11b2787c628edbc95b7ff0e82274b140"));

        assertThat(
            HttpStatus.SC_OK,
            is(
                (res = rh.executePostRequest("/deals/_search?pretty&size=0", query, encodeBasicHeader("user_masked", "password")))
                    .getStatusCode()
            )
        );
        Assert.assertTrue(res.getBody().contains("\"doc_count\" : 30"));
        Assert.assertTrue(res.getBody().contains("\"doc_count\" : 1"));
        Assert.assertFalse(res.getBody().contains("100.100"));
        Assert.assertFalse(res.getBody().contains("200.100"));
        Assert.assertTrue(res.getBody().contains("e1623afebfa505884e249a478640ec98094d19a72ac7a89dd0097e28955bb5ae"));
        Assert.assertTrue(res.getBody().contains("26a8671e57fefc13504f8c61ced67ac98338261ace1e5bf462038b2f2caae16e"));
        Assert.assertTrue(res.getBody().contains("87873bdb698e5f0f60e0b02b76dad1ec11b2787c628edbc95b7ff0e82274b140"));

        for (int i = 0; i < 10; i++) {
            assertThat(
                HttpStatus.SC_OK,
                is(
                    (res = rh.executePostRequest("/deals/_search?pretty&size=0", query, encodeBasicHeader("admin", "admin")))
                        .getStatusCode()
                )
            );
            Assert.assertTrue(res.getBody().contains("100.100"));
            Assert.assertTrue(res.getBody().contains("200.100"));
            Assert.assertTrue(res.getBody().contains("\"doc_count\" : 30"));
            Assert.assertTrue(res.getBody().contains("\"doc_count\" : 1"));
            Assert.assertFalse(res.getBody().contains("e1623afebfa505884e249a478640ec98094d19a72ac7a89dd0097e28955bb5ae"));
            Assert.assertFalse(res.getBody().contains("26a8671e57fefc13504f8c61ced67ac98338261ace1e5bf462038b2f2caae16e"));
            Assert.assertFalse(res.getBody().contains("87873bdb698e5f0f60e0b02b76dad1ec11b2787c628edbc95b7ff0e82274b140"));
        }

    }

    @Test
    public void testMaskedSearch() throws Exception {

        setup();

        HttpResponse res;

        assertThat(
            HttpStatus.SC_OK,
            is((res = rh.executeGetRequest("/deals/_search?pretty&size=100", encodeBasicHeader("admin", "admin"))).getStatusCode())
        );
        Assert.assertTrue(res.getBody().contains("\"value\" : 32,\n      \"relation"));
        Assert.assertTrue(res.getBody().contains("\"failed\" : 0"));
        Assert.assertTrue(res.getBody().contains("cust1"));
        Assert.assertTrue(res.getBody().contains("cust2"));
        Assert.assertTrue(res.getBody().contains("100.100.1.1"));
        Assert.assertTrue(res.getBody().contains("100.100.2.2"));
        Assert.assertFalse(res.getBody().contains("87873bdb698e5f0f60e0b02b76dad1ec11b2787c628edbc95b7ff0e82274b140"));

        assertThat(
            HttpStatus.SC_OK,
            is((res = rh.executeGetRequest("/deals/_search?pretty&size=100", encodeBasicHeader("user_masked", "password"))).getStatusCode())
        );
        Assert.assertTrue(res.getBody().contains("\"value\" : 32,\n      \"relation"));
        Assert.assertTrue(res.getBody().contains("\"failed\" : 0"));
        Assert.assertTrue(res.getBody().contains("cust1"));
        Assert.assertTrue(res.getBody().contains("cust2"));
        Assert.assertFalse(res.getBody().contains("100.100.1.1"));
        Assert.assertFalse(res.getBody().contains("100.100.2.2"));
        Assert.assertTrue(res.getBody().contains("87873bdb698e5f0f60e0b02b76dad1ec11b2787c628edbc95b7ff0e82274b140"));

    }

    @Test
    public void testMaskedSearchWithClusterDefaultSHA512() throws Exception {

        final Settings settings = Settings.builder().put(ConfigConstants.SECURITY_MASKED_FIELDS_ALGORITHM_DEFAULT, "SHA-512").build();
        setup(settings);

        HttpResponse res;

        assertThat(
            HttpStatus.SC_OK,
            is((res = rh.executeGetRequest("/deals/_search?pretty&size=100", encodeBasicHeader("admin", "admin"))).getStatusCode())
        );
        Assert.assertTrue(res.getBody().contains("\"value\" : 32,\n      \"relation"));
        Assert.assertTrue(res.getBody().contains("\"failed\" : 0"));
        Assert.assertTrue(res.getBody().contains("cust1"));
        Assert.assertTrue(res.getBody().contains("cust2"));
        Assert.assertTrue(res.getBody().contains("100.100.1.1"));
        Assert.assertTrue(res.getBody().contains("100.100.2.2"));
        Assert.assertFalse(res.getBody().contains("87873bdb698e5f0f60e0b02b76dad1ec11b2787c628edbc95b7ff0e82274b140"));
        Assert.assertFalse(res.getBody().contains(DigestUtils.sha512Hex("100.100.1.1")));

        assertThat(
            HttpStatus.SC_OK,
            is((res = rh.executeGetRequest("/deals/_search?pretty&size=100", encodeBasicHeader("user_masked", "password"))).getStatusCode())
        );
        Assert.assertTrue(res.getBody().contains("\"value\" : 32,\n      \"relation"));
        Assert.assertTrue(res.getBody().contains("\"failed\" : 0"));
        Assert.assertTrue(res.getBody().contains("cust1"));
        Assert.assertTrue(res.getBody().contains("cust2"));
        Assert.assertFalse(res.getBody().contains("100.100.1.1"));
        Assert.assertFalse(res.getBody().contains("100.100.2.2"));
        Assert.assertFalse(res.getBody().contains("87873bdb698e5f0f60e0b02b76dad1ec11b2787c628edbc95b7ff0e82274b140"));
        Assert.assertTrue(res.getBody().contains(DigestUtils.sha512Hex("100.100.1.1")));

    }

    @Test
    public void testMaskedGet() throws Exception {

        setup();

        HttpResponse res;

        assertThat(
            HttpStatus.SC_OK,
            is((res = rh.executeGetRequest("/deals/_doc/0?pretty", encodeBasicHeader("admin", "admin"))).getStatusCode())
        );
        Assert.assertTrue(res.getBody().contains("\"found\" : true"));
        Assert.assertTrue(res.getBody().contains("cust1"));
        Assert.assertFalse(res.getBody().contains("cust2"));
        Assert.assertTrue(res.getBody().contains("100.100.1.1"));
        Assert.assertFalse(res.getBody().contains("100.100.2.2"));
        Assert.assertFalse(res.getBody().contains("87873bdb698e5f0f60e0b02b76dad1ec11b2787c628edbc95b7ff0e82274b140"));

        assertThat(
            HttpStatus.SC_OK,
            is((res = rh.executeGetRequest("/deals/_doc/0?pretty", encodeBasicHeader("user_masked", "password"))).getStatusCode())
        );
        Assert.assertTrue(res.getBody().contains("\"found\" : true"));
        Assert.assertTrue(res.getBody().contains("cust1"));
        Assert.assertFalse(res.getBody().contains("cust2"));
        Assert.assertFalse(res.getBody().contains("100.100.1.1"));
        Assert.assertFalse(res.getBody().contains("100.100.2.2"));
        Assert.assertTrue(res.getBody().contains("87873bdb698e5f0f60e0b02b76dad1ec11b2787c628edbc95b7ff0e82274b140"));
    }

    @Test
    public void testMaskedGetWithClusterDefaultSHA512() throws Exception {

        final Settings settings = Settings.builder().put(ConfigConstants.SECURITY_MASKED_FIELDS_ALGORITHM_DEFAULT, "SHA-512").build();
        setup(settings);

        HttpResponse res;

        assertThat(
            HttpStatus.SC_OK,
            is((res = rh.executeGetRequest("/deals/_doc/0?pretty", encodeBasicHeader("admin", "admin"))).getStatusCode())
        );
        Assert.assertTrue(res.getBody().contains("\"found\" : true"));
        Assert.assertTrue(res.getBody().contains("cust1"));
        Assert.assertFalse(res.getBody().contains("cust2"));
        Assert.assertTrue(res.getBody().contains("100.100.1.1"));
        Assert.assertFalse(res.getBody().contains("100.100.2.2"));
        Assert.assertFalse(res.getBody().contains("87873bdb698e5f0f60e0b02b76dad1ec11b2787c628edbc95b7ff0e82274b140"));
        Assert.assertFalse(res.getBody().contains(DigestUtils.sha3_224Hex("100.100.1.1")));
        Assert.assertFalse(res.getBody().contains(DigestUtils.sha512Hex("100.100.1.1")));

        assertThat(
            HttpStatus.SC_OK,
            is((res = rh.executeGetRequest("/deals/_doc/0?pretty", encodeBasicHeader("user_masked", "password"))).getStatusCode())
        );

        Assert.assertTrue(res.getBody().contains("\"found\" : true"));
        Assert.assertTrue(res.getBody().contains("cust1"));
        Assert.assertFalse(res.getBody().contains("cust2"));
        Assert.assertFalse(res.getBody().contains("100.100.1.1"));
        Assert.assertFalse(res.getBody().contains("100.100.2.2"));
        Assert.assertFalse(res.getBody().contains("87873bdb698e5f0f60e0b02b76dad1ec11b2787c628edbc95b7ff0e82274b140"));
        Assert.assertFalse(res.getBody().contains(DigestUtils.sha3_224Hex("100.100.1.1")));
        Assert.assertTrue(res.getBody().contains(DigestUtils.sha512Hex("100.100.1.1")));
    }

    @Test
    public void testMaskedGetWithClusterDefaultSHA3() throws Exception {

        final Settings settings = Settings.builder().put(ConfigConstants.SECURITY_MASKED_FIELDS_ALGORITHM_DEFAULT, "SHA3-224").build();
        setup(settings);

        HttpResponse res;

        assertThat(
            HttpStatus.SC_OK,
            is((res = rh.executeGetRequest("/deals/_doc/0?pretty", encodeBasicHeader("admin", "admin"))).getStatusCode())
        );
        Assert.assertTrue(res.getBody().contains("\"found\" : true"));
        Assert.assertTrue(res.getBody().contains("cust1"));
        Assert.assertFalse(res.getBody().contains("cust2"));
        Assert.assertTrue(res.getBody().contains("100.100.1.1"));
        Assert.assertFalse(res.getBody().contains("100.100.2.2"));
        Assert.assertFalse(res.getBody().contains("87873bdb698e5f0f60e0b02b76dad1ec11b2787c628edbc95b7ff0e82274b140"));
        Assert.assertFalse(res.getBody().contains(DigestUtils.sha3_224Hex("100.100.1.1")));
        Assert.assertFalse(res.getBody().contains(DigestUtils.sha512Hex("100.100.1.1")));

        assertThat(
            HttpStatus.SC_OK,
            is((res = rh.executeGetRequest("/deals/_doc/0?pretty", encodeBasicHeader("user_masked", "password"))).getStatusCode())
        );

        Assert.assertTrue(res.getBody().contains("\"found\" : true"));
        Assert.assertTrue(res.getBody().contains("cust1"));
        Assert.assertFalse(res.getBody().contains("cust2"));
        Assert.assertFalse(res.getBody().contains("100.100.1.1"));
        Assert.assertFalse(res.getBody().contains("100.100.2.2"));
        Assert.assertFalse(res.getBody().contains("87873bdb698e5f0f60e0b02b76dad1ec11b2787c628edbc95b7ff0e82274b140"));
        Assert.assertTrue(res.getBody().contains(DigestUtils.sha3_224Hex("100.100.1.1")));
        Assert.assertFalse(res.getBody().contains(DigestUtils.sha512Hex("100.100.1.1")));
    }

    @Test(expected = IllegalStateException.class)
    public void testMaskedGetClusterDefaultDoesNotExist() throws Exception {
        final Settings settings = Settings.builder()
            .put(ConfigConstants.SECURITY_MASKED_FIELDS_ALGORITHM_DEFAULT, "SHA6-FORCEFAIL")
            .build();
        setup(settings);
    }
}
