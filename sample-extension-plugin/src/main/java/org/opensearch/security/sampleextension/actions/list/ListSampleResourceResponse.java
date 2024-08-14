/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions.list;

import java.io.IOException;

import org.opensearch.core.action.ActionResponse;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;

/**
 * Response to a ListSampleResourceRequest
 */
public class ListSampleResourceResponse extends ActionResponse implements ToXContentObject {
    private final String message;

    /**
     * Default constructor
     *
     * @param message The message
     */
    public ListSampleResourceResponse(String message) {
        this.message = message;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(message);
    }

    /**
     * Constructor with StreamInput
     *
     * @param in the stream input
     */
    public ListSampleResourceResponse(final StreamInput in) throws IOException {
        message = in.readString();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("message", message);
        builder.endObject();
        return builder;
    }
}
