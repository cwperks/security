package org.opensearch.security.spi;

import org.opensearch.core.common.io.stream.NamedWriteable;
import org.opensearch.core.xcontent.ToXContentObject;

public interface Resource extends NamedWriteable, ToXContentObject {}
