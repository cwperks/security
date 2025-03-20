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
import org.opensearch.security.spi.ResourceRequest;

import static org.opensearch.security.sampleextension.SampleExtensionPlugin.RESOURCE_INDEX_NAME;

/**
 * Request object for UpdateSampleResource transport action
 */
public class UpdateSampleResourceRequest extends ActionRequest implements ResourceRequest {

    private String resourceId;
    private String name;

    public UpdateSampleResourceRequest(String resourceId, String name) {
        this.resourceId = resourceId;
        this.name = name;
    }

    @Override
    public String getResourceId() {
        return resourceId;
    }

    @Override
    public String getResourceIndex() {
        return RESOURCE_INDEX_NAME;
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
