/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions.create;

import java.io.IOException;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.security.sampleextension.resource.Resource;
import org.opensearch.security.sampleextension.resource.SampleResource;

/**
 * Request object for CreateSampleResource transport action
 */
public class CreateSampleResourceRequest extends ActionRequest {

    private final Resource resource;

    /**
     * Default constructor
     */
    public CreateSampleResourceRequest(Resource resource) {
        this.resource = resource;
    }

    /**
     * Constructor with stream input
     * @param in the stream input
     * @throws IOException IOException
     */
    public CreateSampleResourceRequest(final StreamInput in) throws IOException {
        this.resource = new SampleResource(in);
    }

    @Override
    public void writeTo(final StreamOutput out) throws IOException {
        resource.writeTo(out);
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    public Resource getResource() {
        return this.resource;
    }
}
