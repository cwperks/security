/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sample.actions.list;

import java.util.List;

import org.opensearch.client.node.NodeClient;
import org.opensearch.plugins.resource.action.list.ListResourceRequest;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;

import static java.util.Collections.singletonList;
import static org.opensearch.rest.RestRequest.Method.GET;
import static org.opensearch.security.sample.SampleResourcePlugin.RESOURCE_INDEX_NAME;

public class ListSampleResourceRestAction extends BaseRestHandler {

    public ListSampleResourceRestAction() {}

    @Override
    public List<Route> routes() {
        return singletonList(new Route(GET, "/_plugins/resource_sharing_example/resource"));
    }

    @Override
    public String getName() {
        return "list_sample_resources";
    }

    @Override
    public RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) {
        final ListResourceRequest listSampleResourceRequest = new ListResourceRequest(RESOURCE_INDEX_NAME);
        return channel -> client.executeLocally(
            ListSampleResourceAction.INSTANCE,
            listSampleResourceRequest,
            new RestToXContentListener<>(channel)
        );
    }
}
