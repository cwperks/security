/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.action.whoami;

import java.io.IOException;

import org.opensearch.action.ActionResponse;
import org.opensearch.common.Strings;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.XContentBuilder;

public class WhoAmIResponse extends ActionResponse implements ToXContent {

    private String dn;
    private boolean isAdmin;
    private boolean isAuthenticated;
    private boolean isNodeCertificateRequest;

    public WhoAmIResponse(String dn, boolean isAdmin, boolean isAuthenticated, boolean isNodeCertificateRequest) {
        this.dn = dn;
        this.isAdmin = isAdmin;
        this.isAuthenticated = isAuthenticated;
        this.isNodeCertificateRequest = isNodeCertificateRequest;
    }

    public WhoAmIResponse() {
        super();
        this.dn = null;
        this.isAdmin = false;
        this.isAuthenticated = false;
        this.isNodeCertificateRequest = false;
    }

    public WhoAmIResponse(StreamInput in) throws IOException {
        super(in);
        dn = in.readString();
        isAdmin = in.readBoolean();
        isAuthenticated = in.readBoolean();
        isNodeCertificateRequest = in.readBoolean();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(dn);
        out.writeBoolean(isAdmin);
        out.writeBoolean(isAuthenticated);
        out.writeBoolean(isNodeCertificateRequest);
    }

    public String getDn() {
        return dn;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public boolean isNodeCertificateRequest() {
        return isNodeCertificateRequest;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {

        builder.startObject("whoami");
        builder.field("dn", dn);
        builder.field("is_admin", isAdmin);
        builder.field("is_authenticated", isAuthenticated);
        builder.field("is_node_certificate_request", isNodeCertificateRequest);
        builder.endObject();

        return builder;
    }

    @Override
    public String toString() {
        return Strings.toString(XContentType.JSON, this, true, true);
    }
}
