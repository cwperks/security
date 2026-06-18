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

package org.opensearch.security.auditlog.filter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.support.ActionFilter;
import org.opensearch.action.support.ActionFilterChain;
import org.opensearch.action.support.ActionRequestMetadata;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.action.ActionResponse;
import org.opensearch.security.auditlog.AuditLog;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.ThreadPool;

/**
 * A lightweight ActionFilter that captures transport-layer actions for audit logging
 * without requiring the full security auth/authz stack. Used in SSL-only mode when
 * audit logging is enabled.
 */
public class AuditActionFilter implements ActionFilter {

    private static final Logger log = LogManager.getLogger(AuditActionFilter.class);

    private final AuditLog auditLog;
    private final ThreadPool threadPool;

    public AuditActionFilter(AuditLog auditLog, ThreadPool threadPool) {
        this.auditLog = auditLog;
        this.threadPool = threadPool;
    }

    @Override
    public int order() {
        return Integer.MIN_VALUE;
    }

    @Override
    public <Request extends ActionRequest, Response extends ActionResponse> void apply(
        Task task,
        String action,
        Request request,
        ActionRequestMetadata<Request, Response> actionRequestMetadata,
        ActionListener<Response> listener,
        ActionFilterChain<Request, Response> chain
    ) {
        try {
            // Log the action as a GRANTED_PRIVILEGES event (general request audit)
            auditLog.logGrantedPrivileges(action, request, task);
        } catch (Exception e) {
            log.debug("Failed to log audit event for action {}", action, e);
        }

        // Always continue the chain — audit-only mode never blocks requests
        chain.proceed(task, action, request, listener);
    }
}
