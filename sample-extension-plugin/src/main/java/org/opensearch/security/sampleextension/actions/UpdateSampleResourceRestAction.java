/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.opensearch.client.node.NodeClient;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;

import static java.util.Collections.singletonList;
import static org.opensearch.rest.RestRequest.Method.PUT;

public class UpdateSampleResourceRestAction extends BaseRestHandler {

    public UpdateSampleResourceRestAction() {}

    @Override
    public List<Route> routes() {
        return singletonList(new Route(PUT, "/_plugins/resource_sharing_example/resource/update/{id}"));
    }

    @Override
    public String getName() {
        return "update_sample_resource";
    }

    @SuppressWarnings("unchecked")
    @Override
    public RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        String resourceId = request.param("id");
        Map<String, Object> source;
        try (XContentParser parser = request.contentParser()) {
            source = parser.map();
        }

        String name = (String) source.get("name");

        // TODO Update the request obj
        final UpdateSampleResourceRequest updateSampleResourceRequest = new UpdateSampleResourceRequest(resourceId, name);
        return channel -> client.executeLocally(
            UpdateSampleResourceAction.INSTANCE,
            updateSampleResourceRequest,
            new RestToXContentListener<>(channel)
        );
    }
}
