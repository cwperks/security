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
     * @return The class corresponding to this resource
     */
    Class<? extends AbstractResource> getResourceClass();

    void assignResourceSharingService(ResourceSharingService<? extends AbstractResource> service);
}
