/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.support;

import org.apache.lucene.tests.util.LuceneTestCase;

import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.concurrent.ThreadContext;

public class HeaderHelperTests extends LuceneTestCase {

    public void testLocalExecution() {
        final ThreadContext context = new ThreadContext(Settings.EMPTY);
        assertTrue(HeaderHelper.isLocalExecution(context));
        context.putTransient(ConfigConstants.OPENDISTRO_SECURITY_CHANNEL_TYPE, "direct");
        assertTrue(HeaderHelper.isLocalExecution(context));
        for (String channel : new String[] { "transport", "stream-transport", "", "unknown" }) {
            try (ThreadContext.StoredContext ignored = context.stashContext()) {
                context.putTransient(ConfigConstants.OPENDISTRO_SECURITY_CHANNEL_TYPE, channel);
                // A LOCAL origin must not turn another channel type into a direct channel.
                context.putTransient(ConfigConstants.OPENDISTRO_SECURITY_ORIGIN, "LOCAL");
                assertFalse(channel, HeaderHelper.isLocalExecution(context));
            }
        }
    }

    public void testLocalClusterNodeRequest() {
        final ThreadContext context = new ThreadContext(Settings.EMPTY);

        assertFalse(HeaderHelper.isLocalClusterNodeRequest(context));

        context.putTransient(ConfigConstants.OPENDISTRO_SECURITY_SSL_TRANSPORT_INTERCLUSTER_REQUEST, Boolean.TRUE);

        assertTrue(HeaderHelper.isLocalClusterNodeRequest(context));
        assertFalse(HeaderHelper.isRemoteClusterNodeRequest(context));
    }

    public void testRemoteClusterNodeRequest() {
        final ThreadContext context = new ThreadContext(Settings.EMPTY);

        assertFalse(HeaderHelper.isRemoteClusterNodeRequest(context));

        context.putTransient(ConfigConstants.OPENDISTRO_SECURITY_SSL_TRANSPORT_TRUSTED_CLUSTER_REQUEST, Boolean.TRUE);

        assertTrue(HeaderHelper.isRemoteClusterNodeRequest(context));
        assertFalse(HeaderHelper.isLocalClusterNodeRequest(context));
    }
}
