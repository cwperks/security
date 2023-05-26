/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;

import org.opensearch.common.network.NetworkService;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.BigArrays;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.http.HttpHandlingSettings;
import org.opensearch.http.netty4.Netty4HttpServerTransport;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.SharedGroupFactory;

public class SecurityNonSslHttpServerTransport extends Netty4HttpServerTransport {

    public SecurityNonSslHttpServerTransport(
        final Settings settings,
        final NetworkService networkService,
        final BigArrays bigArrays,
        final ThreadPool threadPool,
        final NamedXContentRegistry namedXContentRegistry,
        final Dispatcher dispatcher,
        ClusterSettings clusterSettings,
        SharedGroupFactory sharedGroupFactory
    ) {
        super(settings, networkService, bigArrays, threadPool, namedXContentRegistry, dispatcher, clusterSettings, sharedGroupFactory);
    }

    @Override
    public ChannelHandler configureServerChannelHandler() {
        return new NonSslHttpChannelHandler(this, handlingSettings);
    }

    protected class NonSslHttpChannelHandler extends Netty4HttpServerTransport.HttpChannelHandler {

        protected NonSslHttpChannelHandler(Netty4HttpServerTransport transport, final HttpHandlingSettings handlingSettings) {
            super(transport, handlingSettings);
        }

        @Override
        protected void initChannel(Channel ch) throws Exception {
            super.initChannel(ch);
        }
    }
}
