/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.spi.actions.resource.list;

import java.io.IOException;
import java.util.List;

import org.opensearch.core.action.ActionResponse;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.security.spi.SharableResource;

/**
 * Response to a ListResourceRequest
 */
public class ListResourceResponse<T extends SharableResource> extends ActionResponse implements ToXContentObject {
    private final List<T> resources;

    /**
     * Default constructor
     *
     * @param resources The resources
     */
    public ListResourceResponse(List<T> resources) {
        this.resources = resources;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeList(resources);
    }

    /**
     * Constructor with StreamInput
     *
     * @param in the stream input
     */
    public ListResourceResponse(final StreamInput in, Reader<T> resourceReader) throws IOException {
        resources = in.readList(resourceReader);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("resources", resources);
        builder.endObject();
        return builder;
    }
}
