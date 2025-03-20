/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions.generic.get;

import java.io.IOException;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.core.common.io.stream.StreamInput;

/**
 * Request object for GetResource transport action
 */
public class GetResourceRequest extends ActionRequest {
    private final String resourceId;

    /**
     * Default constructor
     */
    public GetResourceRequest(String resourceId) {
        this.resourceId = resourceId;
    }

    public GetResourceRequest(StreamInput in) throws IOException {
        super(in);
        this.resourceId = in.readString();
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    public String getResourceId() {
        return this.resourceId;
    }
}
