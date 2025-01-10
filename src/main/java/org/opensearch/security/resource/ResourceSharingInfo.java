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
import java.util.Collections;
import java.util.List;

import org.opensearch.core.ParseField;
import org.opensearch.core.xcontent.ConstructingObjectParser;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.security.rest.resource.ShareWith;

/**
 * ResourceSharingInfo is a subset of the fields in a SharableResource document that correspond to the owner
 * of the SharableResource (the resource_user) and a list of ShareWith data structures that corresponding
 * to who (individuals or groups) the resource has been shared with and at what access level
 */
public class ResourceSharingInfo {
    private final ResourceUser resourceUser;
    private final List<ShareWith> shareWith;

    public ResourceSharingInfo(ResourceUser resourceUser) {
        this.resourceUser = resourceUser;
        this.shareWith = Collections.emptyList();
    }

    public ResourceSharingInfo(ResourceUser resourceUser, List<ShareWith> shareWith) {
        this.resourceUser = resourceUser;
        this.shareWith = shareWith;
    }

    public ResourceUser getResourceUser() {
        return resourceUser;
    }

    public List<ShareWith> getShareWith() {
        return shareWith;
    }

    @SuppressWarnings("unchecked")
    private static final ConstructingObjectParser<ResourceSharingInfo, Void> PARSER = new ConstructingObjectParser<>(
        "resource_sharing_info",
        true,
        a -> new ResourceSharingInfo((ResourceUser) a[0], (List<ShareWith>) a[1])
    );

    static {
        PARSER.declareObject(
            ConstructingObjectParser.constructorArg(),
            (p, c) -> ResourceUser.fromXContent(p),
            new ParseField("resource_user")
        );
        PARSER.declareObjectArray(
            ConstructingObjectParser.optionalConstructorArg(),
            (p, c) -> ShareWith.fromXContent(p),
            new ParseField("share_with")
        );
    }

    public static ResourceSharingInfo parse(XContentParser parser) throws IOException {
        return PARSER.parse(parser, null);
    }
}
