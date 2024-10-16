/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.spi.actions;

import java.io.IOException;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.security.spi.AbstractResource;
import org.opensearch.security.spi.ShareWith;

/**
 * Request object for CreateSampleResource transport action
 */
public class UpdateResourceSharingRequest<T extends AbstractResource> extends ActionRequest {

    private final String resourceId;
    private final ShareWith shareWith;

    /**
     * Default constructor
     */
    public UpdateResourceSharingRequest(String resourceId, ShareWith shareWith) {
        this.resourceId = resourceId;
        this.shareWith = shareWith;
    }

    public UpdateResourceSharingRequest(StreamInput in, Reader<ShareWith> shareWithReader) throws IOException {
        this.resourceId = in.readString();
        this.shareWith = shareWithReader.read(in);
    }

    @Override
    public void writeTo(final StreamOutput out) throws IOException {
        out.writeString(resourceId);
        shareWith.writeTo(out);
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    public String getResourceId() {
        return this.resourceId;
    }

    public ShareWith getShareWith() {
        return this.shareWith;
    }
}
