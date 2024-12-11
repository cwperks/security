package org.opensearch.security.spi;

import java.io.IOException;
import java.util.List;

import org.opensearch.core.common.io.stream.NamedWriteable;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ToXContentFragment;
import org.opensearch.core.xcontent.XContentBuilder;

public class ShareWith implements NamedWriteable, ToXContentFragment {

    public final static ShareWith PRIVATE = new ShareWith(List.of(), List.of(), List.of("*"));
    public final static ShareWith PUBLIC = new ShareWith(List.of("*"), List.of("*"), List.of("*"));

    private final List<String> allowedActions;
    private final List<String> users;
    private final List<String> backendRoles;

    public ShareWith(List<String> users, List<String> backendRoles, List<String> allowedActions) {
        this.users = users;
        this.backendRoles = backendRoles;
        this.allowedActions = allowedActions;
    }

    public ShareWith(StreamInput in) throws IOException {
        this.users = in.readStringList();
        this.backendRoles = in.readStringList();
        this.allowedActions = in.readStringList();
    }

    public List<String> getUsers() {
        return users;
    }

    public List<String> getBackendRoles() {
        return backendRoles;
    }

    public List<String> getAllowedActions() {
        return allowedActions;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject()
            .field("users", users)
            .field("backend_roles", backendRoles)
            .field("allowed_actions", allowedActions)
            .endObject();
    }

    @Override
    public String getWriteableName() {
        return "share_with";
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeStringCollection(users);
        out.writeStringCollection(backendRoles);
        out.writeStringCollection(allowedActions);
    }
}
