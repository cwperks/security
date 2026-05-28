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
 * Integration test verifying that standalone audit logging works in disabled mode
 * (security plugin installed but fully disabled).
 */
public class DisabledModeAuditLoggingTests {

    @ClassRule
    public static LocalCluster cluster = new LocalCluster.Builder().clusterManager(ClusterManager.SINGLENODE)
        .anonymousAuth(false)
        .loadConfigurationIntoIndex(false)
        .nodeSettings(
            Map.of(
                "plugins.security.disabled",
                true,
                "plugins.security.ssl.http.enabled",
                false,
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
        .authc(AUTHC_HTTPBASIC_INTERNAL)
        .build();

    @Rule
    public AuditLogsRule auditLogsRule = new AuditLogsRule();

    @Test
    public void shouldAuditRestRequestsInDisabledMode() {
        try (TestRestClient client = cluster.getSecurityDisabledRestClient()) {
            HttpResponse response = client.get("_cat/indices");
            response.assertStatusCode(200);
        }

        auditLogsRule.assertAtLeast(1, msg -> msg.getCategory() == AuditCategory.GRANTED_PRIVILEGES);
    }

    @Test
    public void shouldAuditSearchRequestsInDisabledMode() {
        try (TestRestClient client = cluster.getSecurityDisabledRestClient()) {
            HttpResponse response = client.get("_search");
            response.assertStatusCode(200);
        }

        auditLogsRule.assertAtLeast(1, msg -> msg.getCategory() == AuditCategory.GRANTED_PRIVILEGES);
    }
}
