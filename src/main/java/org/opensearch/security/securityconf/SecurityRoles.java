/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.securityconf;

import java.util.Set;

import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.security.resolver.IndexResolverReplacer.Resolved;
import org.opensearch.security.user.User;

public interface SecurityRoles {

    boolean impliesClusterPermissionPermission(String action0);

    boolean hasExplicitClusterPermissionPermission(String action);

    Set<String> getRoleNames();

    Set<String> reduce(
        Resolved requestedResolved,
        User user,
        String[] strings,
        IndexNameExpressionResolver resolver,
        ClusterService clusterService
    );

    boolean impliesTypePermGlobal(
        Resolved requestedResolved,
        User user,
        String[] allIndexPermsRequiredA,
        IndexNameExpressionResolver resolver,
        ClusterService clusterService
    );

    boolean get(
        Resolved requestedResolved,
        User user,
        String[] allIndexPermsRequiredA,
        IndexNameExpressionResolver resolver,
        ClusterService clusterService
    );

    EvaluatedDlsFlsConfig getDlsFls(
        User user,
        boolean dfmEmptyOverwritesAll,
        IndexNameExpressionResolver resolver,
        ClusterService clusterService,
        NamedXContentRegistry namedXContentRegistry
    );

    Set<String> getAllPermittedIndicesForDashboards(
        Resolved resolved,
        User user,
        String[] actions,
        IndexNameExpressionResolver resolver,
        ClusterService cs
    );

    SecurityRoles filter(Set<String> roles);

}
