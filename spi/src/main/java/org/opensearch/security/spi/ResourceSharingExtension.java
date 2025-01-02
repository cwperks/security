/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.spi;

/**
 * SPI of security.
 */
public interface ResourceSharingExtension {
    /**
     * @return resource type string.
     */
    String getResourceType();

    /**
     * @return resource index name.
     */
    String getResourceIndex();

    /**
     * @return returns a parser for this resource
     */
    default ResourceParser<? extends Resource> getResourceParser() {
        return null;
    };

    void assignResourceSharingService(ResourceSharingService<? extends Resource> service);
}
