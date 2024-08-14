package org.opensearch.security.sampleextension.resource;

import java.io.IOException;

import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.core.xcontent.ToXContentFragment;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.security.spi.ResourceSharingExtension;

import static org.opensearch.security.sampleextension.SampleExtensionPlugin.RESOURCE_INDEX_NAME;

public class SampleResource implements ResourceSharingExtension, Writeable, ToXContentFragment {

    private String name;

    public SampleResource(String name) {
        this.name = name;
    }

    @Override
    public String getResourceType() {
        return "sample_resource";
    }

    @Override
    public String getResourceIndex() {
        return RESOURCE_INDEX_NAME;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject().field("name", name).endObject();
    }

    @Override
    public void writeTo(StreamOutput streamOutput) throws IOException {
        streamOutput.writeString(name);
    }
}
