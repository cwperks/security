package org.opensearch.security.sampleextension.resource;

import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.core.xcontent.ToXContentFragment;

public abstract class Resource implements Writeable, ToXContentFragment {}
