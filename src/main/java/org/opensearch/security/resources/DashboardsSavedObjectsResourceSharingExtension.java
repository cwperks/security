/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.resources;

import java.util.Set;

import org.opensearch.security.spi.resources.ResourceProvider;
import org.opensearch.security.spi.resources.ResourceSharingExtension;
import org.opensearch.security.spi.resources.client.ResourceSharingClient;

/**
 * Built-in resource sharing extension for OpenSearch Dashboards saved objects
 * (dashboards and visualizations) stored in the .kibana* indices.
 */
public class DashboardsSavedObjectsResourceSharingExtension implements ResourceSharingExtension {

    private static final String KIBANA_INDEX_PATTERN = ".kibana*";

    @Override
    public Set<ResourceProvider> getResourceProviders() {
        return Set.of(new ResourceProvider() {
            @Override
            public String resourceType() {
                return "dashboard";
            }

            @Override
            public String resourceIndexName() {
                return KIBANA_INDEX_PATTERN;
            }

            @Override
            public String typeField() {
                return "type";
            }

            @Override
            public String defaultGeneralAccess() {
                return "dashboard_view";
            }
        }, new ResourceProvider() {
            @Override
            public String resourceType() {
                return "visualization";
            }

            @Override
            public String resourceIndexName() {
                return KIBANA_INDEX_PATTERN;
            }

            @Override
            public String typeField() {
                return "type";
            }

            @Override
            public String defaultGeneralAccess() {
                return "visualization_view";
            }
        });
    }

    @Override
    public void assignResourceSharingClient(ResourceSharingClient resourceSharingClient) {
        // No client needed — access is managed via security REST APIs
    }
}
