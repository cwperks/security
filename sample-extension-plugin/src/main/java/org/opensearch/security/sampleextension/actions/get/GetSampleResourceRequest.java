/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions.get;

import java.io.IOException;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;

/**
 * Request object for GetSampleResource transport action
 */
public class GetSampleResourceRequest extends ActionRequest {

    private String resourceId;

    public GetSampleResourceRequest(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceId() {
        return resourceId;
    }

    /**
     * Constructor with stream input
     * @param in the stream input
     * @throws IOException IOException
     */
    public GetSampleResourceRequest(final StreamInput in) throws IOException {}

    @Override
    public void writeTo(final StreamOutput out) throws IOException {}

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }
}
