/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.privileges;

import java.util.Map;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.opensearch.client.Client;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.security.resolver.IndexResolverReplacer.Resolved;
import org.opensearch.security.securityconf.DynamicConfigModel;
import org.opensearch.security.user.User;
import org.opensearch.threadpool.ThreadPool;

public class PrivilegesInterceptor {

    public static class ReplaceResult {
        final boolean continueEvaluation;
        final boolean accessDenied;
        final CreateIndexRequestBuilder createIndexRequestBuilder;

        private ReplaceResult(boolean continueEvaluation, boolean accessDenied, CreateIndexRequestBuilder createIndexRequestBuilder) {
            this.continueEvaluation = continueEvaluation;
            this.accessDenied = accessDenied;
            this.createIndexRequestBuilder = createIndexRequestBuilder;
        }
    }

    public static final ReplaceResult CONTINUE_EVALUATION_REPLACE_RESULT = new ReplaceResult(true, false, null);
    public static final ReplaceResult ACCESS_DENIED_REPLACE_RESULT = new ReplaceResult(false, true, null);
    public static final ReplaceResult ACCESS_GRANTED_REPLACE_RESULT = new ReplaceResult(false, false, null);

    protected static ReplaceResult newAccessGrantedReplaceResult(CreateIndexRequestBuilder createIndexRequestBuilder) {
        return new ReplaceResult(false, false, createIndexRequestBuilder);
    }

    protected final IndexNameExpressionResolver resolver;
    protected final ClusterService clusterService;
    protected final Client client;
    protected final ThreadPool threadPool;

    public PrivilegesInterceptor(
        final IndexNameExpressionResolver resolver,
        final ClusterService clusterService,
        final Client client,
        ThreadPool threadPool
    ) {
        this.resolver = resolver;
        this.clusterService = clusterService;
        this.client = client;
        this.threadPool = threadPool;
    }

    public ReplaceResult replaceDashboardsIndex(
        final ActionRequest request,
        final String action,
        final User user,
        final DynamicConfigModel config,
        final Resolved requestedResolved,
        final Map<String, Boolean> tenants
    ) {
        throw new RuntimeException("not implemented");
    }

    protected final ThreadContext getThreadContext() {
        return threadPool.getThreadContext();
    }
}
