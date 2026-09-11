/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file to be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */
package org.opensearch.security.privileges.actionlevel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.admin.cluster.health.ClusterHealthAction;
import org.opensearch.action.admin.cluster.health.ClusterHealthRequest;
import org.opensearch.action.admin.indices.stats.IndicesStatsAction;
import org.opensearch.action.admin.indices.stats.IndicesStatsRequest;
import org.opensearch.security.privileges.DashboardsMultiTenancyConfiguration;
import org.opensearch.security.user.User;

/**
 * A transitional compatibility shim that treats Dashboards tenant indices as hidden from broad
 * monitor requests until they are created with {@code index.hidden=true}.
 */
public class DashboardsTenantIndicesRequestFilter {
    private static final Logger log = LogManager.getLogger(DashboardsTenantIndicesRequestFilter.class);

    private final Supplier<DashboardsMultiTenancyConfiguration> multiTenancyConfigurationSupplier;

    public DashboardsTenantIndicesRequestFilter(Supplier<DashboardsMultiTenancyConfiguration> multiTenancyConfigurationSupplier) {
        this.multiTenancyConfigurationSupplier = multiTenancyConfigurationSupplier;
    }

    /**
     * Rewrites mixed stats and health requests to exclude concrete Dashboards tenant indices.
     *
     * @return true if the request was rewritten
     */
    public boolean filter(ActionRequest request, String action, User user, Collection<String> resolvedIndices) {
        if (isApplicable(action, user) == false) {
            return false;
        }

        DashboardsMultiTenancyConfiguration multiTenancyConfiguration = multiTenancyConfigurationSupplier.get();
        String tenantIndexPrefix = multiTenancyConfiguration.dashboardsIndex() + "_";
        Set<String> nonTenantIndices = new HashSet<>();
        boolean containsTenantIndex = false;
        for (String index : resolvedIndices) {
            if (index.startsWith(tenantIndexPrefix)) {
                containsTenantIndex = true;
            } else {
                nonTenantIndices.add(index);
            }
        }

        // A tenant-only request may be explicit, so leave it on the existing authorization path.
        if (containsTenantIndex == false || nonTenantIndices.isEmpty()) {
            return false;
        }

        String[] filteredIndices = nonTenantIndices.toArray(String[]::new);
        if (request instanceof IndicesStatsRequest indicesStatsRequest) {
            indicesStatsRequest.indices(filteredIndices);
        } else if (request instanceof ClusterHealthRequest clusterHealthRequest) {
            clusterHealthRequest.indices(filteredIndices);
        } else {
            return false;
        }

        log.debug("Filtered Dashboards tenant indices from broad monitor request [{}]", action);
        return true;
    }

    /**
     * Returns whether this filter applies to the action and user.
     */
    public boolean isApplicable(String action, User user) {
        if ((IndicesStatsAction.NAME.equals(action) || ClusterHealthAction.NAME.equals(action)) == false) {
            return false;
        }

        DashboardsMultiTenancyConfiguration multiTenancyConfiguration = multiTenancyConfigurationSupplier.get();
        if (multiTenancyConfiguration.multitenancyEnabled() == false
            || user.getName().equals(multiTenancyConfiguration.dashboardsServerUsername())) {
            return false;
        }

        return true;
    }
}
