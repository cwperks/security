/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions.generic.list;

import java.io.IOException;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.core.common.io.stream.StreamInput;

/**
 * Request object for ListResource transport action
 */
public class ListResourceRequest extends ActionRequest {

    // TODO Change this into Search instead of List

    /**
     * Default constructor
     */
    public ListResourceRequest() {
        super();
    }

    public ListResourceRequest(StreamInput in) throws IOException {
        super(in);
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }
}
