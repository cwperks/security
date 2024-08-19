package org.opensearch.security.user;

import java.io.IOException;

import org.opensearch.core.xcontent.ToXContentFragment;
import org.opensearch.core.xcontent.XContentBuilder;

public class UserFragment implements ToXContentFragment {
    private final User user;

    public UserFragment(User user) {
        this.user = user;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject()
            .field("username", user.getName())
            .field("roles", user.getSecurityRoles())
            .field("backend_roles", user.getRoles())
            .endObject();
    }
}
