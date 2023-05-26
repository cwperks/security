/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.ssl.http.netty;

import java.nio.file.Path;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.ExceptionsHelper;
import org.opensearch.OpenSearchException;
import org.opensearch.OpenSearchSecurityException;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.http.HttpServerTransport.Dispatcher;
import org.opensearch.rest.RestChannel;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestStatus;
import org.opensearch.security.ssl.SslExceptionHandler;
import org.opensearch.security.ssl.util.ExceptionUtils;
import org.opensearch.security.ssl.util.SSLRequestHelper;

public class ValidatingDispatcher implements Dispatcher {

    private static final Logger logger = LogManager.getLogger(ValidatingDispatcher.class);

    private final ThreadContext threadContext;
    private final Dispatcher originalDispatcher;
    private final SslExceptionHandler errorHandler;
    private final Settings settings;
    private final Path configPath;

    public ValidatingDispatcher(
        final ThreadContext threadContext,
        final Dispatcher originalDispatcher,
        final Settings settings,
        final Path configPath,
        final SslExceptionHandler errorHandler
    ) {
        super();
        this.threadContext = threadContext;
        this.originalDispatcher = originalDispatcher;
        this.settings = settings;
        this.configPath = configPath;
        this.errorHandler = errorHandler;
    }

    @Override
    public void dispatchRequest(RestRequest request, RestChannel channel, ThreadContext threadContext) {
        checkRequest(request, channel);
        originalDispatcher.dispatchRequest(request, channel, threadContext);
    }

    @Override
    public void dispatchBadRequest(RestChannel channel, ThreadContext threadContext, Throwable cause) {
        checkRequest(channel.request(), channel);
        originalDispatcher.dispatchBadRequest(channel, threadContext, cause);
    }

    protected void checkRequest(final RestRequest request, final RestChannel channel) {

        if (SSLRequestHelper.containsBadHeader(threadContext, "_opendistro_security_ssl_")) {
            final OpenSearchException exception = ExceptionUtils.createBadHeaderException();
            errorHandler.logError(exception, request, 1);
            throw exception;
        }

        try {
            if (SSLRequestHelper.getSSLInfo(settings, configPath, request, null) == null) {
                logger.error("Not an SSL request");
                throw new OpenSearchSecurityException("Not an SSL request", RestStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (SSLPeerUnverifiedException e) {
            logger.error("No client certificates found but such are needed (SG 8).");
            errorHandler.logError(e, request, 0);
            throw ExceptionsHelper.convertToOpenSearchException(e);
        }
    }
}
