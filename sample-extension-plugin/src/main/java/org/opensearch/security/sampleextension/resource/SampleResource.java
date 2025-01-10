package org.opensearch.security.sampleextension.resource;

import java.io.IOException;
import java.time.Instant;

import org.opensearch.core.ParseField;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ConstructingObjectParser;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.security.spi.SharableResource;

import static org.opensearch.core.xcontent.ConstructingObjectParser.constructorArg;

public class SampleResource implements SharableResource {

    private String name;
    private Instant lastUpdateTime;

    public SampleResource() {
        Instant now = Instant.now();
        this.lastUpdateTime = now;
    }

    public SampleResource(String name, Instant lastUpdateTime) {
        this.name = name;
        this.lastUpdateTime = lastUpdateTime;
    }

    public SampleResource(String name, Long lastUpdateTime) {
        this.name = name;
        this.lastUpdateTime = Instant.ofEpochMilli(lastUpdateTime);
    }

    public SampleResource(StreamInput in) throws IOException {
        this.name = in.readString();
        this.lastUpdateTime = in.readInstant();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject().field("name", name).field("last_update_time", lastUpdateTime.toEpochMilli()).endObject();
    }

    @SuppressWarnings("unchecked")
    private static final ConstructingObjectParser<SampleResource, Void> PARSER = new ConstructingObjectParser<>(
        "sample_resource",
        true,
        a -> new SampleResource((String) a[0], (Long) a[1])
    );

    static {
        PARSER.declareString(constructorArg(), new ParseField("name"));
        PARSER.declareLong(constructorArg(), new ParseField("last_update_time"));
    }

    public static SampleResource fromXContent(XContentParser parser) throws IOException {
        return PARSER.parse(parser, null);
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
