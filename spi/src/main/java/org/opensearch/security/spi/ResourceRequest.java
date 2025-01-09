package org.opensearch.security.spi;

/**
 * A ResourceRequest is a subtype of ActionRequest that pertains to resource.
 */
public interface ResourceRequest {
    String getResourceId();

    String getResourceIndex();
}
