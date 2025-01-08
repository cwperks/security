/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.rest.resource;

import java.io.IOException;
import java.util.List;

import org.opensearch.core.common.io.stream.NamedWriteable;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ToXContentFragment;
import org.opensearch.core.xcontent.XContentBuilder;

public class ShareWith implements NamedWriteable, ToXContentFragment {

    public final static ShareWith PRIVATE = new ShareWith(List.of("unlimited"), List.of(), List.of());
    public final static ShareWith PUBLIC = new ShareWith(List.of("unlimited"), List.of("*"), List.of("*"));

    private final List<String> allowedActions;
    private final List<String> users;
    private final List<String> backendRoles;

    public ShareWith(List<String> allowedActions, List<String> users, List<String> backendRoles) {
        this.allowedActions = allowedActions;
        this.users = users;
        this.backendRoles = backendRoles;
    }

    public ShareWith(StreamInput in) throws IOException {
        this.allowedActions = in.readStringList();
        this.users = in.readStringList();
        this.backendRoles = in.readStringList();
    }

    public List<String> getAllowedActions() {
        return allowedActions;
    }

    public List<String> getUsers() {
        return users;
    }

    public List<String> getBackendRoles() {
        return backendRoles;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject()
            .field("allowedActions", allowedActions)
            .field("users", users)
            .field("backend_roles", backendRoles)
            .endObject();
    }

    @Override
    public String getWriteableName() {
        return "share_with";
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeStringCollection(allowedActions);
        out.writeStringCollection(users);
        out.writeStringCollection(backendRoles);
    }
}
