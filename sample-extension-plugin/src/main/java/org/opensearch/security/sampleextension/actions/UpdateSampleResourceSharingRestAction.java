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
import org.opensearch.security.spi.ShareWith;
import org.opensearch.security.spi.actions.UpdateResourceSharingRequest;

import static java.util.Collections.singletonList;
import static org.opensearch.rest.RestRequest.Method.PUT;

public class UpdateSampleResourceSharingRestAction extends BaseRestHandler {

    public UpdateSampleResourceSharingRestAction() {}

    @Override
    public List<Route> routes() {
        return singletonList(new Route(PUT, "/_plugins/resource_sharing_example/resource/update_sharing/{id}"));
    }

    @Override
    public String getName() {
        return "update_sample_resource_sharing";
    }

    @SuppressWarnings("unchecked")
    @Override
    public RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        String resourceId = request.param("id");

        System.out.println("update sharing endpoint");
        System.out.println("resourceId: " + resourceId);
        Map<String, Object> source;
        try (XContentParser parser = request.contentParser()) {
            source = parser.map();
        }

        Map<String, Object> shareWithMap = (Map<String, Object>) source.get("share_with");
        ShareWith shareWith = new ShareWith((List<String>) shareWithMap.get("users"), (List<String>) shareWithMap.get("backend_roles"));

        final UpdateResourceSharingRequest<SampleResource> updateSampleResourceSharingRequest = new UpdateResourceSharingRequest<>(
            resourceId,
            shareWith
        );
        return channel -> client.executeLocally(
            UpdateSampleResourceSharingAction.INSTANCE,
            updateSampleResourceSharingRequest,
            new RestToXContentListener<>(channel)
        );
    }
}
