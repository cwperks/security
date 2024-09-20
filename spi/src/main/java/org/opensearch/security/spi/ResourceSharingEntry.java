package org.opensearch.security.spi;

import java.io.IOException;

import org.opensearch.core.xcontent.ToXContentFragment;
import org.opensearch.core.xcontent.XContentBuilder;

public class ResourceSharingEntry implements ToXContentFragment {
    private final String resourceIndex;
    private final String resourceId;
    private final ShareWith shareWith;

    public ResourceSharingEntry(String resourceIndex, String resourceId, ShareWith shareWith) {
        this.resourceIndex = resourceIndex;
        this.resourceId = resourceId;
        this.shareWith = shareWith;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject()
            .field("resource_index", resourceIndex)
            .field("resource_id", resourceId)
            .field("share_with", shareWith)
            .endObject();
    }
}
