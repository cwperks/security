/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions;

import java.io.IOException;
import java.util.List;

import org.opensearch.core.action.ActionResponse;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.security.spi.AbstractResource;

/**
 * Response to a ListSampleResourceRequest
 */
public class ListSampleResourceResponse extends ActionResponse implements ToXContentObject {
    private final List<SampleResource> resources;

    /**
     * Default constructor
     *
     * @param resources The resources
     */
    public ListSampleResourceResponse(List<SampleResource> resources) {
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
    public ListSampleResourceResponse(final StreamInput in) throws IOException {
        resources = in.readList(SampleResource::new);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.array("resources", (Object[]) resources.toArray(new AbstractResource[0]));
        builder.endObject();
        return builder;
    }
}
