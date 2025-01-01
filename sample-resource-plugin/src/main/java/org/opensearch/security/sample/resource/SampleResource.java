package org.opensearch.security.sample.resource;

import java.io.IOException;

import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.plugins.resource.Resource;

public class SampleResource implements Resource {

    private String name;

    public SampleResource() {}

    SampleResource(StreamInput in) throws IOException {
        this.name = in.readString();
    }

    public static SampleResource from(StreamInput in) throws IOException {
        return new SampleResource(in);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject().field("name", name).endObject();
    }

    @Override
    public void writeTo(StreamOutput streamOutput) throws IOException {
        streamOutput.writeString(name);
    }

    @Override
    public String getWriteableName() {
        return "sample_resource";
    }

    public void setName(String name) {
        this.name = name;
    }
}
