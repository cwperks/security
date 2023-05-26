/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.auditlog;

import org.opensearch.OpenSearchException;
import org.opensearch.rest.RestRequest;
import org.opensearch.security.ssl.SslExceptionHandler;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportRequest;

public class AuditLogSslExceptionHandler implements SslExceptionHandler {

    private final AuditLog auditLog;

    public AuditLogSslExceptionHandler(final AuditLog auditLog) {
        super();
        this.auditLog = auditLog;
    }

    @Override
    public void logError(Throwable t, RestRequest request, int type) {
        switch (type) {
            case 0:
                auditLog.logSSLException(request, t);
                break;
            case 1:
                auditLog.logBadHeaders(request);
                break;
            default:
                break;
        }
    }

    @Override
    public void logError(Throwable t, boolean isRest) {
        if (isRest) {
            auditLog.logSSLException(null, t);
        } else {
            auditLog.logSSLException(null, t, null, null);
        }
    }

    @Override
    public void logError(Throwable t, TransportRequest request, String action, Task task, int type) {
        switch (type) {
            case 0:
                if (t instanceof OpenSearchException) {
                    auditLog.logMissingPrivileges(action, request, task);
                } else {
                    auditLog.logSSLException(request, t, action, task);
                }
                break;
            case 1:
                auditLog.logBadHeaders(request, action, task);
                break;
            default:
                break;
        }
    }

}
