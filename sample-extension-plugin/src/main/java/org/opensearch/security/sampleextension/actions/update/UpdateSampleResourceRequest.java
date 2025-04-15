/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions.update;

import java.io.IOException;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;

/**
 * Request object for UpdateSampleResource transport action
 */
public class UpdateSampleResourceRequest extends ActionRequest {

    private String resourceId;
    private String name;

    public UpdateSampleResourceRequest(String resourceId, String name) {
        this.resourceId = resourceId;
        this.name = name;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getName() {
        return name;
    }

    /**
     * Constructor with stream input
     * @param in the stream input
     * @throws IOException IOException
     */
    public UpdateSampleResourceRequest(final StreamInput in) throws IOException {}

    @Override
    public void writeTo(final StreamOutput out) throws IOException {}

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }
}
