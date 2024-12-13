package org.opensearch.security.sampleextension.resource;

import org.opensearch.security.spi.ResourceFactory;

public class SampleResourceFactory implements ResourceFactory<SampleResource> {
    @Override
    public SampleResource createResource() {
        return new SampleResource();
    }
}
