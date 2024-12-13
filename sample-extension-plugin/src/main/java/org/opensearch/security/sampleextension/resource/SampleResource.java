package org.opensearch.security.sampleextension.resource;

import java.io.IOException;
import java.util.Map;

import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.security.spi.AbstractResource;

public class SampleResource extends AbstractResource {

    private String name;

    public SampleResource() {}

    SampleResource(StreamInput in) throws IOException {
        this.name = in.readString();
    }

    public static SampleResource from(StreamInput in) throws IOException {
        return new SampleResource(in);
    }

    @Override
    public void fromSource(String resourceId, Map<String, Object> sourceAsMap) {
        super.fromSource(resourceId, sourceAsMap);
        this.name = (String) sourceAsMap.get("name");
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
