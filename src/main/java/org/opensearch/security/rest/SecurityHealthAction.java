/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.rest;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.ImmutableList;

import org.opensearch.client.node.NodeClient;
import org.opensearch.common.settings.Settings;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestChannel;
import org.opensearch.rest.RestController;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestStatus;
import org.opensearch.security.auth.BackendRegistry;

import static org.opensearch.rest.RestRequest.Method.GET;
import static org.opensearch.rest.RestRequest.Method.POST;
import static org.opensearch.security.dlic.rest.support.Utils.addRoutesPrefix;

public class SecurityHealthAction extends BaseRestHandler {
    private static final List<Route> routes = addRoutesPrefix(
        ImmutableList.of(new Route(GET, "/health"), new Route(POST, "/health")),
        "/_opendistro/_security",
        "/_plugins/_security"
    );

    private final BackendRegistry registry;

    public SecurityHealthAction(final Settings settings, final RestController controller, final BackendRegistry registry) {
        super();
        this.registry = registry;
    }

    @Override
    public List<Route> routes() {
        return routes;
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        return new RestChannelConsumer() {

            final String mode = request.param("mode", "strict");

            @Override
            public void accept(RestChannel channel) throws Exception {
                XContentBuilder builder = channel.newBuilder();
                RestStatus restStatus = RestStatus.OK;
                BytesRestResponse response = null;
                try {

                    String status = "UP";
                    String message = null;

                    builder.startObject();

                    if ("strict".equalsIgnoreCase(mode) && registry.isInitialized() == false) {
                        status = "DOWN";
                        message = "Not initialized";
                        restStatus = RestStatus.SERVICE_UNAVAILABLE;
                    }

                    builder.field("message", message);
                    builder.field("mode", mode);
                    builder.field("status", status);
                    builder.endObject();
                    response = new BytesRestResponse(restStatus, builder);

                } finally {
                    builder.close();
                }

                channel.sendResponse(response);
            }

        };
    }

    @Override
    public String getName() {
        return "OpenSearch Security Health Check";
    }

}
