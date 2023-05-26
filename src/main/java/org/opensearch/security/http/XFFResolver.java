/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.http;

import java.net.InetSocketAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.greenrobot.eventbus.Subscribe;

import org.opensearch.OpenSearchSecurityException;
import org.opensearch.common.transport.TransportAddress;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.http.netty4.Netty4HttpChannel;
import org.opensearch.rest.RestRequest;
import org.opensearch.security.securityconf.DynamicConfigModel;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.threadpool.ThreadPool;

public class XFFResolver {

    protected final Logger log = LogManager.getLogger(this.getClass());
    private volatile boolean enabled;
    private volatile RemoteIpDetector detector;
    private final ThreadContext threadContext;

    public XFFResolver(final ThreadPool threadPool) {
        super();
        this.threadContext = threadPool.getThreadContext();
    }

    public TransportAddress resolve(final RestRequest request) throws OpenSearchSecurityException {
        final boolean isTraceEnabled = log.isTraceEnabled();
        if (isTraceEnabled) {
            log.trace("resolve {}", request.getHttpChannel().getRemoteAddress());
        }

        if (enabled
            && request.getHttpChannel().getRemoteAddress() instanceof InetSocketAddress
            && request.getHttpChannel() instanceof Netty4HttpChannel) {

            final InetSocketAddress isa = new InetSocketAddress(
                detector.detect(request, threadContext),
                ((InetSocketAddress) request.getHttpChannel().getRemoteAddress()).getPort()
            );

            if (isa.isUnresolved()) {
                throw new OpenSearchSecurityException("Cannot resolve address " + isa.getHostString());
            }

            if (isTraceEnabled) {
                if (threadContext.getTransient(ConfigConstants.OPENDISTRO_SECURITY_XFF_DONE) == Boolean.TRUE) {
                    log.trace("xff resolved {} to {}", request.getHttpChannel().getRemoteAddress(), isa);
                } else {
                    log.trace("no xff done for {}", request.getClass());
                }
            }
            return new TransportAddress(isa);
        } else if (request.getHttpChannel().getRemoteAddress() instanceof InetSocketAddress) {

            if (isTraceEnabled) {
                log.trace("no xff done (enabled or no netty request) {},{},{},{}", enabled, request.getClass());

            }
            return new TransportAddress((InetSocketAddress) request.getHttpChannel().getRemoteAddress());
        } else {
            throw new OpenSearchSecurityException(
                "Cannot handle this request. Remote address is "
                    + request.getHttpChannel().getRemoteAddress()
                    + " with request class "
                    + request.getClass()
            );
        }
    }

    @Subscribe
    public void onDynamicConfigModelChanged(DynamicConfigModel dcm) {
        enabled = dcm.isXffEnabled();
        if (enabled) {
            detector = new RemoteIpDetector();
            detector.setInternalProxies(dcm.getInternalProxies());
            detector.setRemoteIpHeader(dcm.getRemoteIpHeader());
        } else {
            detector = null;
        }
    }
}
