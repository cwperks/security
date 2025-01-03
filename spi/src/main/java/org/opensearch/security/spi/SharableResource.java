package org.opensearch.security.spi;

import java.time.Instant;

import org.opensearch.core.common.io.stream.NamedWriteable;
import org.opensearch.core.xcontent.ToXContentObject;

public interface SharableResource extends NamedWriteable, ToXContentObject {
    /**
     * @return resource name.
     */
    String getName();

    /**
     * @return resource last update time.
     */
    Instant getLastUpdateTime();
}
