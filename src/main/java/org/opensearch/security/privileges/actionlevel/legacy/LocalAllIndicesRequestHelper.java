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

package org.opensearch.security.privileges.actionlevel.legacy;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.admin.cluster.health.ClusterHealthAction;
import org.opensearch.action.admin.indices.stats.IndicesStatsAction;
import org.opensearch.action.support.LocalAllIndicesRequest;
import org.opensearch.action.support.LocalAllIndicesRequestContext;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.security.support.HeaderHelper;

final class LocalAllIndicesRequestHelper {

    private LocalAllIndicesRequestHelper() {}

    static boolean isTrustedLocalAllMonitorRequest(ActionRequest request, String action, ThreadContext threadContext) {
        final boolean trustedContext = LocalAllIndicesRequestContext.isMarked(threadContext)
            || HeaderHelper.isInterClusterRequest(threadContext);
        if (trustedContext == false) {
            return false;
        }

        return (IndicesStatsAction.NAME.equals(action)
            || IndicesStatsAction.NAME.concat("[n]").equals(action)
            || ClusterHealthAction.NAME.equals(action))
            && request instanceof LocalAllIndicesRequest
            && ((LocalAllIndicesRequest) request).isDerivedFromLocalAllIndices();
    }
}
