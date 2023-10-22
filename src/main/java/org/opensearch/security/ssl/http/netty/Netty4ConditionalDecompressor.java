/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.ssl.http.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;

import static org.opensearch.security.http.SecurityHttpServerTransport.REQUEST_CONTEXTS;

import java.util.Map;

public class Netty4ConditionalDecompressor extends HttpContentDecompressor {

    private String contentEncodingOverride;

    @Override
    protected EmbeddedChannel newContentDecoder(String contentEncoding) throws Exception {
        if (contentEncodingOverride != null) {
            // If there was an error prompting an early response,... don't decompress
            // If there is no explicit decompress flag,... don't decompress
            // If there is a decompress flag and it is false,... don't decompress
            return super.newContentDecoder(contentEncodingOverride);
        }

        // Decompresses the content based on its encoding
        return super.newContentDecoder(contentEncoding);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        contentEncodingOverride = null;
        if (msg instanceof HttpMessage) {
            final HttpMessage message = (HttpMessage) msg;
            final HttpHeaders headers = message.headers();
            Map<String, Netty4RequestContext> requestContexts = ctx.channel().attr(REQUEST_CONTEXTS).get();
            String requestId = headers.get("X-Channel-Request-ID");
            if (requestId != null && requestContexts != null) {
                Netty4RequestContext requestContext = requestContexts.get(requestId);
                if (requestContext != null && (!requestContext.shouldDecompress || requestContext.earlyResponse != null)) {
                    contentEncodingOverride = "identity";
                }
            }
        }
        super.channelRead(ctx, msg);
    }
}
