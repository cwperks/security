package org.opensearch.security.spi;

import java.io.IOException;

import org.opensearch.core.xcontent.XContentParser;

public interface ResourceParser<T extends Resource> {
    T parse(XContentParser xContentParser, String id) throws IOException;
}
