/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.rest.resource;

import java.io.IOException;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.security.spi.ShareWith;

/**
 * Request object for UpdateResourceSharing transport action
 */
public class ShareWithRequest extends ActionRequest {

    private final String resourceId;
    private final String resourceIndex;
    private final ShareWith shareWith;

    /**
     * Default constructor
     */
    public ShareWithRequest(String resourceId, String resourceIndex, ShareWith shareWith) {
        this.resourceId = resourceId;
        this.resourceIndex = resourceIndex;
        this.shareWith = shareWith;
    }

    public ShareWithRequest(StreamInput in) throws IOException {
        this.resourceId = in.readString();
        this.resourceIndex = in.readString();
        this.shareWith = new ShareWith(in);
    }

    @Override
    public void writeTo(final StreamOutput out) throws IOException {
        out.writeString(resourceId);
        out.writeString(resourceIndex);
        shareWith.writeTo(out);
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    public String getResourceId() {
        return this.resourceId;
    }

    public String getResourceIndex() {
        return this.resourceIndex;
    }

    public ShareWith getShareWith() {
        return this.shareWith;
    }
}
