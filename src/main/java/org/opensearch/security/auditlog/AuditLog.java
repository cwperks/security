/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.auditlog;

import java.io.Closeable;
import java.util.Map;

import org.opensearch.index.engine.Engine.Delete;
import org.opensearch.index.engine.Engine.DeleteResult;
import org.opensearch.index.engine.Engine.Index;
import org.opensearch.index.engine.Engine.IndexResult;
import org.opensearch.index.get.GetResult;
import org.opensearch.index.shard.ShardId;
import org.opensearch.rest.RestRequest;
import org.opensearch.security.auditlog.config.AuditConfig;
import org.opensearch.security.compliance.ComplianceConfig;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportRequest;

public interface AuditLog extends Closeable {

    // login
    void logFailedLogin(String effectiveUser, boolean securityadmin, String initiatingUser, RestRequest request);

    void logSucceededLogin(String effectiveUser, boolean securityadmin, String initiatingUser, RestRequest request);

    // privs
    void logMissingPrivileges(String privilege, String effectiveUser, RestRequest request);

    void logGrantedPrivileges(String effectiveUser, RestRequest request);

    void logMissingPrivileges(String privilege, TransportRequest request, Task task);

    void logGrantedPrivileges(String privilege, TransportRequest request, Task task);

    // index event requests
    void logIndexEvent(String privilege, TransportRequest request, Task task);

    // spoof
    void logBadHeaders(TransportRequest request, String action, Task task);

    void logBadHeaders(RestRequest request);

    void logSecurityIndexAttempt(TransportRequest request, String action, Task task);

    void logSSLException(TransportRequest request, Throwable t, String action, Task task);

    void logSSLException(RestRequest request, Throwable t);

    void logDocumentRead(String index, String id, ShardId shardId, Map<String, String> fieldNameValues);

    void logDocumentWritten(ShardId shardId, GetResult originalIndex, Index currentIndex, IndexResult result);

    void logDocumentDeleted(ShardId shardId, Delete delete, DeleteResult result);

    // compliance config
    ComplianceConfig getComplianceConfig();

    // set config
    void setConfig(AuditConfig auditConfig);

    public enum Origin {
        REST,
        TRANSPORT,
        LOCAL
    }

    public enum Operation {
        CREATE,
        UPDATE,
        DELETE
    }
}
