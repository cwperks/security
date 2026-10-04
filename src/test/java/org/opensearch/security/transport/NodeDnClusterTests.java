/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.transport;

import org.junit.Test;

import org.opensearch.common.settings.Settings;
import org.opensearch.security.DefaultObjectMapper;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.test.SingleClusterTest;

import static org.junit.Assert.assertEquals;

public class NodeDnClusterTests extends SingleClusterTest {
    @Test
    public void testMultiNodeClusterWithNodeDnOnly() throws Exception {
        setup(
            Settings.builder()
                .putNull(ConfigConstants.SECURITY_CERT_OID)
                .putList(ConfigConstants.SECURITY_NODES_DN, "CN=node-0.example.com,OU=SSL,O=Test,L=Test,C=DE")
                .build()
        );
        var response = nonSslRestHelper().executeGetRequest("_cluster/health", encodeBasicHeader("nagilum", "nagilum"));
        assertEquals(response.getBody(), 200, response.getStatusCode());
        assertEquals(3, DefaultObjectMapper.readTree(response.getBody()).get("number_of_nodes").asInt());
    }
}
