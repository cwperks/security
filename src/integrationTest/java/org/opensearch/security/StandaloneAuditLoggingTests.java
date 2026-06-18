/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 */
package org.opensearch.security;

import java.util.Collections;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.opensearch.security.auditlog.impl.AuditCategory;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.test.framework.audit.AuditLogsRule;
import org.opensearch.test.framework.audit.TestRuleAuditLogSink;
import org.opensearch.test.framework.cluster.ClusterManager;
import org.opensearch.test.framework.cluster.LocalCluster;
import org.opensearch.test.framework.cluster.TestRestClient;
import org.opensearch.test.framework.cluster.TestRestClient.HttpResponse;

import static org.opensearch.test.framework.TestSecurityConfig.AuthcDomain.AUTHC_HTTPBASIC_INTERNAL;

/**
 * Integration tests verifying that standalone audit logging works in non-FGAC modes.
 */
public class StandaloneAuditLoggingTests {

    /**
     * Cluster running in SSL-only mode with standalone audit enabled.
     */
    @ClassRule
    public static LocalCluster sslOnlyCluster = new LocalCluster.Builder().clusterManager(ClusterManager.SINGLENODE)
        .anonymousAuth(false)
        .loadConfigurationIntoIndex(false)
        .nodeSettings(
            Map.of(
                ConfigConstants.SECURITY_SSL_ONLY,
                true,
                ConfigConstants.SECURITY_AUDIT_STANDALONE_ENABLED,
                true,
                "plugins.security.audit.type",
                TestRuleAuditLogSink.class.getName(),
                ConfigConstants.OPENDISTRO_SECURITY_AUDIT_CONFIG_DISABLED_TRANSPORT_CATEGORIES,
                Collections.emptyList(),
                ConfigConstants.OPENDISTRO_SECURITY_AUDIT_CONFIG_DISABLED_REST_CATEGORIES,
                Collections.emptyList()
            )
        )
        .sslOnly(true)
        .authc(AUTHC_HTTPBASIC_INTERNAL)
        .build();

    @Rule
    public AuditLogsRule auditLogsRule = new AuditLogsRule();

    @Test
    public void shouldAuditRestRequestsInSslOnlyMode() {
        try (TestRestClient client = sslOnlyCluster.getRestClient()) {
            HttpResponse response = client.get("_cat/indices");
            response.assertStatusCode(200);
        }

        auditLogsRule.assertAtLeast(1, msg -> msg.getCategory() == AuditCategory.GRANTED_PRIVILEGES);
    }

    @Test
    public void shouldAuditSearchRequestsInSslOnlyMode() {
        try (TestRestClient client = sslOnlyCluster.getRestClient()) {
            HttpResponse response = client.get("_search");
            response.assertStatusCode(200);
        }

        auditLogsRule.assertAtLeast(1, msg -> msg.getCategory() == AuditCategory.GRANTED_PRIVILEGES);
    }
}
