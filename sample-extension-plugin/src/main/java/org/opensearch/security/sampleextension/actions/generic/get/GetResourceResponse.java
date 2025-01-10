/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions.generic.get;

import java.io.IOException;

import org.opensearch.core.action.ActionResponse;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.security.spi.SharableResource;

/**
 * Response to a GetResourceRequest
 */
public class GetResourceResponse<T extends SharableResource> extends ActionResponse implements ToXContentObject {
    private final T resource;

    /**
     * Default constructor
     *
     * @param resource The resource
     */
    public GetResourceResponse(T resource) {
        this.resource = resource;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        resource.writeTo(out);
    }

    /**
     * Constructor with StreamInput
     *
     * @param in the stream input
     */
    public GetResourceResponse(final StreamInput in, Writeable.Reader<T> resourceReader) throws IOException {
        resource = resourceReader.read(in);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("resource", resource);
        builder.endObject();
        return builder;
    }
}
