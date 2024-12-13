package org.opensearch.security.spi;

public interface ResourceFactory<T extends Resource> {
    T createResource();
}
