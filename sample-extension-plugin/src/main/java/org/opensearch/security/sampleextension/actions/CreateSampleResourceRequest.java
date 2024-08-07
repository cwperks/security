/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions;

import java.io.IOException;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;

/**
 * Request object for CreateSampleResource transport action
 */
public class CreateSampleResourceRequest extends ActionRequest {

    private final String name;

    /**
     * Default constructor
     */
    public CreateSampleResourceRequest(String name) {
        this.name = name;
    }

    /**
     * Constructor with stream input
     * @param in the stream input
     * @throws IOException IOException
     */
    public CreateSampleResourceRequest(final StreamInput in) throws IOException {
        this.name = in.readString();
    }

    @Override
    public void writeTo(final StreamOutput out) throws IOException {
        out.writeString(name);
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }
}
