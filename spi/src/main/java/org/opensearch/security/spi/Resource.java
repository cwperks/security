package org.opensearch.security.spi;

import java.util.Map;

import org.opensearch.core.common.io.stream.NamedWriteable;
import org.opensearch.core.xcontent.ToXContentFragment;

public abstract class Resource implements NamedWriteable, ToXContentFragment {
    protected String resourceId;

    public String getResourceId() {
        return resourceId;
    }

    public void fromSource(String resourceId, Map<String, Object> sourceAsMap) {
        this.resourceId = resourceId;
    }
}
