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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opensearch.core.xcontent.ToXContentFragment;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.security.spi.ResourceUser;
import org.opensearch.security.spi.ShareWith;

public class ResourceSharingEntry implements ToXContentFragment {
    private final String resourceIndex;
    private final String resourceId;
    private final ResourceUser resourceUser;
    private final List<ShareWith> shareWith;

    public ResourceSharingEntry(String resourceIndex, String resourceId, ResourceUser resourceUser, List<ShareWith> shareWith) {
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
        List<Map<String, Object>> sharedWithList = (List<Map<String, Object>>) sourceAsMap.get("share_with");
        List<ShareWith> sharedWith = new ArrayList<>();
        for (Map<String, Object> sharedWithMap : sharedWithList) {
            sharedWith.add(ShareWith.fromSource(sharedWithMap));
        }
        return new ResourceSharingEntry(resourceIndex, resourceId, resourceUser, sharedWith);
    }

    public ResourceUser getResourceUser() {
        return resourceUser;
    }

    public List<ShareWith> getShareWith() {
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
