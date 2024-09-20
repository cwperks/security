package org.opensearch.security.spi;

import java.io.IOException;
import java.util.List;

import org.opensearch.core.xcontent.ToXContentFragment;
import org.opensearch.core.xcontent.XContentBuilder;

public class ShareWith implements ToXContentFragment {

    public final static ShareWith PRIVATE = new ShareWith(List.of(), List.of(), List.of());
    public final static ShareWith PUBLIC = new ShareWith(List.of("*"), List.of("*"), List.of("*"));

    private final List<String> users;
    private final List<String> roles;
    private final List<String> backendRoles;

    public ShareWith(List<String> users, List<String> roles, List<String> backendRoles) {
        this.users = users;
        this.roles = roles;
        this.backendRoles = backendRoles;
    }

    public List<String> getUsers() {
        return users;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getBackendRoles() {
        return backendRoles;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject().field("users", users).field("roles", roles).field("backendRoles", backendRoles).endObject();
    }
}
