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

import org.opensearch.core.xcontent.ToXContentFragment;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.security.spi.ResourceUser;
import org.opensearch.security.spi.ShareWith;

public class ResourceSharingEntry implements ToXContentFragment {
    private final String resourceIndex;
    private final String resourceId;
    private final ResourceUser resourceUser;
    private final ShareWith shareWith;

    public ResourceSharingEntry(String resourceIndex, String resourceId, ResourceUser resourceUser, ShareWith shareWith) {
        this.resourceIndex = resourceIndex;
        this.resourceId = resourceId;
        this.resourceUser = resourceUser;
        this.shareWith = shareWith;
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
