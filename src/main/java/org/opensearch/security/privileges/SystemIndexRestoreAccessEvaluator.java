/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.security.privileges;

import java.util.Collection;
import java.util.Set;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.admin.cluster.snapshots.restore.RestoreSnapshotRequest;
import org.opensearch.common.settings.Settings;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.support.WildcardMatcher;

/**
 * Grants REST API administrators a narrowly scoped exception to the system index privilege requirement when restoring
 * explicitly allowlisted system indices from a snapshot.
 */
public class SystemIndexRestoreAccessEvaluator {

    private final Set<String> restApiAllowedRoles;
    private final WildcardMatcher restorableSystemIndices;
    private final String securityIndex;

    public SystemIndexRestoreAccessEvaluator(Settings settings) {
        this.restApiAllowedRoles = Set.copyOf(settings.getAsList(ConfigConstants.SECURITY_RESTAPI_ROLES_ENABLED));
        this.restorableSystemIndices = WildcardMatcher.from(
            settings.getAsList(
                ConfigConstants.SECURITY_SYSTEM_INDICES_RESTORE_INDICES_KEY,
                ConfigConstants.SECURITY_SYSTEM_INDICES_RESTORE_INDICES_DEFAULT
            )
        );
        this.securityIndex = settings.get(
            ConfigConstants.SECURITY_CONFIG_INDEX_NAME,
            ConfigConstants.OPENDISTRO_SECURITY_DEFAULT_CONFIG_INDEX
        );
    }

    public boolean isAllowed(PrivilegesEvaluationContext context, String systemIndex) {
        return isRestoreRequestByRestApiAdmin(context, context.getRequest())
            && !securityIndex.equals(systemIndex)
            && restorableSystemIndices.test(systemIndex);
    }

    public boolean isAllowed(PrivilegesEvaluationContext context, ActionRequest request, Collection<String> requestedSystemIndices) {
        return !requestedSystemIndices.isEmpty()
            && isRestoreRequestByRestApiAdmin(context, request)
            && requestedSystemIndices.stream().noneMatch(securityIndex::equals)
            && requestedSystemIndices.stream().allMatch(restorableSystemIndices::test);
    }

    private boolean isRestoreRequestByRestApiAdmin(PrivilegesEvaluationContext context, ActionRequest request) {
        return request instanceof RestoreSnapshotRequest && context.getMappedRoles().stream().anyMatch(restApiAllowedRoles::contains);
    }
}
