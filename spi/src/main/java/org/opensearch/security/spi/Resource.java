package org.opensearch.security.spi;

import java.util.Map;

import org.opensearch.core.common.io.stream.NamedWriteable;
import org.opensearch.core.xcontent.ToXContentFragment;

public abstract class Resource implements NamedWriteable, ToXContentFragment {
    public Resource() {}

    public abstract String getResourceIndex();

    public abstract String getResourceType();

    public abstract Resource fromSource(Map<String, Object> sourceAsMap);
}
