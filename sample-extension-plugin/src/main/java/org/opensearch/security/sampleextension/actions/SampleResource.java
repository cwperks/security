package org.opensearch.security.sampleextension.actions;

import java.io.IOException;
import java.util.Map;

import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.security.spi.Resource;
import org.opensearch.security.spi.ResourceSharingExtension;
import org.opensearch.security.spi.ResourceSharingService;

import static org.opensearch.security.sampleextension.SampleExtensionPlugin.RESOURCE_INDEX_NAME;

public class SampleResource extends Resource implements ResourceSharingExtension {

    private String name;
    private ResourceSharingService<?> resourceSharingService;

    public SampleResource() {}

    SampleResource(StreamInput in) throws IOException {
        this.name = in.readString();
    }

    @Override
    public String getResourceType() {
        return "sample_resource";
    }

    @Override
    public SampleResource fromSource(Map<String, Object> sourceAsMap) {
        SampleResource sample = new SampleResource();
        sample.setName((String) sourceAsMap.get("name"));
        return sample;
    }

    @Override
    public String getResourceIndex() {
        return RESOURCE_INDEX_NAME;
    }

    @Override
    public void assignResourceSharingService(ResourceSharingService<?> service) {
        // Only called if security plugin is installed
        System.out.println("assignResourceSharingService called");
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
