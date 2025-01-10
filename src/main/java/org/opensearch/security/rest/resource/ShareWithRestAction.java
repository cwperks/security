/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.rest.resource;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import org.opensearch.client.node.NodeClient;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;
import org.opensearch.security.spi.SharableResourceExtension;

import static org.opensearch.rest.RestRequest.Method.PUT;
import static org.opensearch.security.dlic.rest.support.Utils.addRoutesPrefix;

public class ShareWithRestAction extends BaseRestHandler {

    private final Map<String, String> resourceTypeToIndexMap = new HashMap<>();

    public ShareWithRestAction(final List<SharableResourceExtension> sharableResourceExtensions) {
        if (sharableResourceExtensions != null) {
            for (SharableResourceExtension resourceSharingExtension : sharableResourceExtensions) {
                resourceTypeToIndexMap.put(resourceSharingExtension.getResourceType(), resourceSharingExtension.getResourceIndex());
            }
        }
    }

    private static final List<Route> routes = addRoutesPrefix(
        ImmutableList.of(new Route(PUT, "/resource/{resource_type}/{id}/share_with")),
        "/_plugins/_security"
    );

    @Override
    public List<Route> routes() {
        return routes;
    }

    @Override
    public String getName() {
        return "update_resource_sharing";
    }

    @SuppressWarnings("unchecked")
    @Override
    public RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        String resourceId = request.param("id");
        String resourceType = request.param("resource_type");

        if (!resourceTypeToIndexMap.containsKey(resourceType)) {
            throw new IllegalArgumentException("Resource type " + resourceType + " is not supported");
        }

        System.out.println("share with endpoint");
        System.out.println("resourceId: " + resourceId);
        Map<String, Object> source;
        try (XContentParser parser = request.contentParser()) {
            source = parser.map();
        }

        Map<String, Object> shareWithMap = (Map<String, Object>) source.get("share_with");
        ShareWith shareWith = new ShareWith(
            (List<String>) shareWithMap.get("allowed_actions"),
            (List<String>) shareWithMap.get("users"),
            (List<String>) shareWithMap.get("backend_roles")
        );

        final ShareWithRequest shareWithRequest = new ShareWithRequest(resourceId, resourceTypeToIndexMap.get(resourceType), shareWith);
        return channel -> client.executeLocally(ShareWithAction.INSTANCE, shareWithRequest, new RestToXContentListener<>(channel));
    }
}
