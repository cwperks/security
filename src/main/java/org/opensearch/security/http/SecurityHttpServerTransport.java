/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.http;

import org.opensearch.common.network.NetworkService;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.BigArrays;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.security.ssl.SecurityKeyStore;
import org.opensearch.security.ssl.SslExceptionHandler;
import org.opensearch.security.ssl.http.netty.SecuritySSLNettyHttpServerTransport;
import org.opensearch.security.ssl.http.netty.ValidatingDispatcher;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.SharedGroupFactory;

public class SecurityHttpServerTransport extends SecuritySSLNettyHttpServerTransport {

    public SecurityHttpServerTransport(
        final Settings settings,
        final NetworkService networkService,
        final BigArrays bigArrays,
        final ThreadPool threadPool,
        final SecurityKeyStore odsks,
        final SslExceptionHandler sslExceptionHandler,
        final NamedXContentRegistry namedXContentRegistry,
        final ValidatingDispatcher dispatcher,
        final ClusterSettings clusterSettings,
        SharedGroupFactory sharedGroupFactory
    ) {
        super(
            settings,
            networkService,
            bigArrays,
            threadPool,
            odsks,
            namedXContentRegistry,
            dispatcher,
            sslExceptionHandler,
            clusterSettings,
            sharedGroupFactory
        );
    }
}
