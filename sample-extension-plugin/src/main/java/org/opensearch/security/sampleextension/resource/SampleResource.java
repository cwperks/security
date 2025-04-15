package org.opensearch.security.sampleextension.resource;

import java.io.IOException;
import java.time.Instant;

import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.security.spi.SharableResource;

public class SampleResource implements SharableResource {

    private String name;
    private Instant lastUpdateTime;

    public SampleResource() {
        Instant now = Instant.now();
        this.lastUpdateTime = now;
    }

    public SampleResource(StreamInput in) throws IOException {
        this.name = in.readString();
        this.lastUpdateTime = in.readInstant();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject().field("name", name).endObject();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(name);
        out.writeInstant(lastUpdateTime);

    }

    @Override
    public String getWriteableName() {
        return "sample_resource";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Instant getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLastUpdateTime(Instant lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}
