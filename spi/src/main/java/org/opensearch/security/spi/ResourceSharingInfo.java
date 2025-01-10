package org.opensearch.security.spi;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.opensearch.core.ParseField;
import org.opensearch.core.xcontent.ConstructingObjectParser;
import org.opensearch.core.xcontent.XContentParser;

/**
 * ResourceSharingInfo is a subset of the fields in a SharableResource document that correspond to the owner
 * of the SharableResource (the resource_user) and a list of ShareWith data structures that corresponding
 * to who (individuals or groups) the resource has been shared with and at what access level
 */
public class ResourceSharingInfo {
    private final ResourceUser resourceUser;
    private final Map<String, ShareWith> shareWith;

    public ResourceSharingInfo(ResourceUser resourceUser) {
        this.resourceUser = resourceUser;
        this.shareWith = Collections.emptyMap();
    }

    public ResourceSharingInfo(ResourceUser resourceUser, Map<String, ShareWith> shareWith) {
        this.resourceUser = resourceUser;
        this.shareWith = shareWith;
    }

    public ResourceUser getResourceUser() {
        return resourceUser;
    }

    public Map<String, ShareWith> getShareWith() {
        return shareWith;
    }

    @SuppressWarnings("unchecked")
    private static final ConstructingObjectParser<ResourceSharingInfo, Void> PARSER = new ConstructingObjectParser<>(
        "resource_sharing_info",
        true,
        a -> new ResourceSharingInfo((ResourceUser) a[0], (Map<String, ShareWith>) a[1])
    );

    static {
        PARSER.declareObject(
            ConstructingObjectParser.constructorArg(),
            (p, c) -> ResourceUser.fromXContent(p),
            new ParseField("resource_user")
        );
        PARSER.declareObject(ConstructingObjectParser.optionalConstructorArg(), (p, c) -> {
            Map<String, ShareWith> shareWithMap = new HashMap<>();
            String fieldName;
            while ((fieldName = p.currentName()) != null) {
                if (p.nextToken() == XContentParser.Token.START_OBJECT) {
                    shareWithMap.put(fieldName, ShareWith.fromXContent(p));
                }
            }
            return shareWithMap;
        }, new ParseField("share_with"));
        // PARSER.declareObjectArray(
        // ConstructingObjectParser.optionalConstructorArg(),
        // (p, c) -> ShareWith.fromXContent(p),
        // new ParseField("share_with")
        // );
    }

    public static ResourceSharingInfo parse(XContentParser parser) throws IOException {
        return PARSER.parse(parser, null);
    }
}
