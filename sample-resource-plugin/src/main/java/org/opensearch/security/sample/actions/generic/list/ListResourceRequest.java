/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sample.actions.generic.list;

import java.io.IOException;

import org.opensearch.action.ResourceRequest;
import org.opensearch.core.common.io.stream.StreamInput;

/**
 * Request object for ListResource transport action
 */
public class ListResourceRequest extends ResourceRequest {

    // TODO Change this into Search instead of List

    /**
     * Default constructor
     */
    public ListResourceRequest(String resourceIndex) {
        super(resourceIndex);
    }

    public ListResourceRequest(StreamInput in) throws IOException {
        super(in);
    }
}
