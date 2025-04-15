/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.security.resource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.opensearch.core.xcontent.ToXContentFragment;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.security.rest.resource.ShareWith;

public class ResourceSharingEntry implements ToXContentFragment {
    private final String resourceIndex;
    private final String resourceId;
    private final ResourceUser resourceUser;
    // Key to this map is an action group
    private final Map<String, ShareWith> shareWith;

    public ResourceSharingEntry(String resourceIndex, String resourceId, ResourceUser resourceUser, Map<String, ShareWith> shareWith) {
        this.resourceIndex = resourceIndex;
        this.resourceId = resourceId;
        this.resourceUser = resourceUser;
        this.shareWith = shareWith;
    }

    @SuppressWarnings("unchecked")
    public static ResourceSharingEntry fromSource(Map<String, Object> sourceAsMap) {
        String resourceIndex = (String) sourceAsMap.get("resource_index");
        String resourceId = (String) sourceAsMap.get("resource_id");
        ResourceUser resourceUser = ResourceUser.fromSource((Map<String, Object>) sourceAsMap.get("resource_user"));
        Map<String, Object> sharedWithMap = (Map<String, Object>) sourceAsMap.get("share_with");
        Map<String, ShareWith> sharedWith = new HashMap<>();
        for (Map.Entry<String, Object> entry : sharedWithMap.entrySet()) {
            ShareWith shareWith = ShareWith.fromSource((Map<String, Object>) entry.getValue());
            sharedWith.put(entry.getKey(), shareWith);
        }
        return new ResourceSharingEntry(resourceIndex, resourceId, resourceUser, sharedWith);
    }

    public ResourceUser getResourceUser() {
        return resourceUser;
    }

    public Map<String, ShareWith> getShareWith() {
        return shareWith;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject()
            .field("resource_index", resourceIndex)
            .field("resource_id", resourceId)
            .field("resource_user", resourceUser)
            .field("share_with", shareWith)
            .endObject();
    }
}
