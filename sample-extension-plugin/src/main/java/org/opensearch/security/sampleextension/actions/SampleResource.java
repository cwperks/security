package org.opensearch.security.sampleextension.actions;

import java.io.IOException;
import java.util.Map;

import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.security.sampleextension.resource.SampleResourceSharingService;
import org.opensearch.security.spi.AbstractResource;
import org.opensearch.security.spi.ResourceSharingExtension;
import org.opensearch.security.spi.ResourceSharingService;

import static org.opensearch.security.sampleextension.SampleExtensionPlugin.RESOURCE_INDEX_NAME;

public class SampleResource extends AbstractResource implements ResourceSharingExtension {

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
    public void fromSource(String resourceId, Map<String, Object> sourceAsMap) {
        super.fromSource(resourceId, sourceAsMap);
        this.name = (String) sourceAsMap.get("name");
    }

    @Override
    public String getResourceIndex() {
        return RESOURCE_INDEX_NAME;
    }

    @Override
    public Class<? extends AbstractResource> getResourceClass() {
        return SampleResource.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void assignResourceSharingService(ResourceSharingService<? extends AbstractResource> service) {
        // Only called if security plugin is installed
        System.out.println("assignResourceSharingService called");
        ResourceSharingService<SampleResource> sharingService = (ResourceSharingService<SampleResource>) service;
        SampleResourceSharingService.getInstance().setSharingService(sharingService);
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
