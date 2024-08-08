/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions;

import java.util.List;

import org.opensearch.client.node.NodeClient;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;

import static java.util.Collections.singletonList;
import static org.opensearch.rest.RestRequest.Method.POST;

public class CreateSampleResourceRestAction extends BaseRestHandler {

    public CreateSampleResourceRestAction() {}

    @Override
    public List<Route> routes() {
        return singletonList(new Route(POST, "/_plugins/resource_sharing_example/resource"));
    }

    @Override
    public String getName() {
        return "create_sample_resource";
    }

    @Override
    public RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) {
        String name = request.param("name");
        final CreateSampleResourceRequest createSampleResourceRequest = new CreateSampleResourceRequest(name);
        return channel -> client.executeLocally(
            CreateSampleResourceAction.INSTANCE,
            createSampleResourceRequest,
            new RestToXContentListener<>(channel)
        );
    }
}
