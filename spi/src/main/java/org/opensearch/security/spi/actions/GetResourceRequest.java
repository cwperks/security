/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.spi.actions;

import java.io.IOException;

import org.opensearch.core.common.io.stream.StreamInput;

/**
 * Request object for GetSampleResource transport action
 */
public class GetResourceRequest extends ResourceRequest {

    /**
     * Default constructor
     */
    public GetResourceRequest(String resourceId, String resourceIndex) {
        super(resourceId, resourceIndex);
    }

    public GetResourceRequest(StreamInput in) throws IOException {
        super(in);
    }
}
