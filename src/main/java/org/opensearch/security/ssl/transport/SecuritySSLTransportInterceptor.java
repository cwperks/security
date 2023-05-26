/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.ssl.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.common.settings.Settings;
import org.opensearch.security.ssl.SslExceptionHandler;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportInterceptor;
import org.opensearch.transport.TransportRequest;
import org.opensearch.transport.TransportRequestHandler;

public final class SecuritySSLTransportInterceptor implements TransportInterceptor {

    protected final Logger log = LogManager.getLogger(this.getClass());
    protected final ThreadPool threadPool;
    protected final PrincipalExtractor principalExtractor;
    protected final SslExceptionHandler errorHandler;
    protected final SSLConfig SSLConfig;

    public SecuritySSLTransportInterceptor(
        final Settings settings,
        final ThreadPool threadPool,
        PrincipalExtractor principalExtractor,
        final SSLConfig SSLConfig,
        final SslExceptionHandler errorHandler
    ) {
        this.threadPool = threadPool;
        this.principalExtractor = principalExtractor;
        this.errorHandler = errorHandler;
        this.SSLConfig = SSLConfig;
    }

    @Override
    public <T extends TransportRequest> TransportRequestHandler<T> interceptHandler(
        String action,
        String executor,
        boolean forceExecution,
        TransportRequestHandler<T> actualHandler
    ) {
        return new SecuritySSLRequestHandler<T>(action, actualHandler, threadPool, principalExtractor, SSLConfig, errorHandler);
    }

}
