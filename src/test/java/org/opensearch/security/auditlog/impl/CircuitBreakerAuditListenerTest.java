/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.auditlog.impl;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.opensearch.cluster.ClusterName;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.indices.breaker.CircuitBreakerTripNotifier;
import org.opensearch.security.auditlog.AuditLog.Origin;
import org.opensearch.security.auditlog.AuditTestUtils;
import org.opensearch.security.auditlog.config.AuditConfig;
import org.opensearch.security.auditlog.integration.TestAuditlogImpl;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.test.AbstractSecurityUnitTest;
import org.opensearch.threadpool.ThreadPool;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CircuitBreakerAuditListenerTest {
    @Test
    public void breakerAuditIsDisabledByDefault() {
        assertTrue(AuditConfig.from(Settings.EMPTY).getFilter().getDisabledCategories().contains(AuditCategory.CIRCUIT_BREAKER_TRIPPED));
    }

    @Test
    public void respectsAuditCategoryFilters() throws Exception {
        ClusterService clusterService = mock(ClusterService.class);
        Settings settings = Settings.builder()
            .put("plugins.security.audit.type", TestAuditlogImpl.class.getName())
            .put(ConfigConstants.SECURITY_AUDIT_CONFIG_DISABLED_CATEGORIES, "NONE")
            .build();
        try (
            AuditLogImpl auditLog = (AuditLogImpl) AuditTestUtils.createAuditLog(
                settings,
                null,
                null,
                AbstractSecurityUnitTest.MOCK_POOL,
                null,
                clusterService
            )
        ) {
            assertTrue(auditLog.isCircuitBreakerAuditEnabled(null, Origin.REST));
            assertTrue(auditLog.isCircuitBreakerAuditEnabled(null, Origin.TRANSPORT));
            auditLog.getFilter().setDisabledRestCategories(List.of("CIRCUIT_BREAKER_TRIPPED"));
            assertFalse(auditLog.isCircuitBreakerAuditEnabled(null, Origin.REST));
            assertTrue(auditLog.isCircuitBreakerAuditEnabled(null, Origin.TRANSPORT));
            auditLog.getFilter().setDisabledCategories(List.of("CIRCUIT_BREAKER_TRIPPED"));
            assertFalse(auditLog.isCircuitBreakerAuditEnabled(null, null));
            assertFalse(auditLog.isCircuitBreakerAuditEnabled(null, Origin.TRANSPORT));
        }
    }

    @Test
    public void suppressesTripsFromAuditSinkWrites() {
        CircuitBreakerTripNotifier notifier = new CircuitBreakerTripNotifier();
        AuditLogImpl auditLog = mock(AuditLogImpl.class);
        ThreadPool threadPool = mock(ThreadPool.class);
        ThreadContext context = new ThreadContext(Settings.EMPTY);
        when(threadPool.getThreadContext()).thenReturn(context);
        context.putHeader(ConfigConstants.SECURITY_AUDIT_SINK_REQUEST_HEADER, "true");
        try (
            CircuitBreakerAuditListener listener = new CircuitBreakerAuditListener(
                notifier,
                auditLog,
                mock(ClusterService.class),
                threadPool
            )
        ) {
            notifier.notifyTrip("request", "audit", 10, 1, "TRANSIENT");
        }
        verify(auditLog, never()).logCircuitBreakerTrip(any());
    }

    @Test
    public void suppressesUnattributedTransportTrips() {
        CircuitBreakerTripNotifier notifier = new CircuitBreakerTripNotifier();
        AuditLogImpl auditLog = mock(AuditLogImpl.class);
        ThreadPool threadPool = mock(ThreadPool.class);
        ThreadContext context = new ThreadContext(Settings.EMPTY);
        when(threadPool.getThreadContext()).thenReturn(context);
        when(auditLog.isCircuitBreakerAuditEnabled(isNull(), isNull())).thenReturn(true);
        try (
            CircuitBreakerAuditListener listener = new CircuitBreakerAuditListener(
                notifier,
                auditLog,
                mock(ClusterService.class),
                threadPool
            )
        ) {
            notifier.notifyTrip("in_flight_requests", "indices:data/write/index", 10, 1, "TRANSIENT");
        }
        verify(auditLog, never()).logCircuitBreakerTrip(any());
    }

    @Test
    public void doesNotBlockBreakerWhenAuditQueueIsFull() throws Exception {
        CircuitBreakerTripNotifier notifier = new CircuitBreakerTripNotifier();
        AuditLogImpl auditLog = mock(AuditLogImpl.class);
        ThreadPool threadPool = mock(ThreadPool.class);
        ClusterService clusterService = mock(ClusterService.class);
        DiscoveryNode node = mock(DiscoveryNode.class);
        when(clusterService.localNode()).thenReturn(node);
        when(clusterService.getClusterName()).thenReturn(new ClusterName("test"));
        when(node.getHostAddress()).thenReturn("127.0.0.1");
        when(node.getHostName()).thenReturn("localhost");
        when(node.getName()).thenReturn("node");
        when(node.getId()).thenReturn("id");
        when(auditLog.isCircuitBreakerAuditEnabled(isNull(), eq(Origin.REST))).thenReturn(true);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            started.countDown();
            assertTrue(release.await(10, TimeUnit.SECONDS));
            return null;
        }).when(auditLog).logCircuitBreakerTrip(any());

        ThreadContext context = new ThreadContext(Settings.EMPTY);
        when(threadPool.getThreadContext()).thenReturn(context);
        context.putTransient(ConfigConstants.OPENDISTRO_SECURITY_ORIGIN, Origin.REST.toString());
        try (CircuitBreakerAuditListener listener = new CircuitBreakerAuditListener(notifier, auditLog, clusterService, threadPool)) {
            try {
                notifier.notifyTrip("request", "test", 10, 1, "TRANSIENT");
                assertTrue(started.await(10, TimeUnit.SECONDS));
                for (int i = 0; i < 300; i++) {
                    notifier.notifyTrip("request", "test", 10, 1, "TRANSIENT");
                }
                assertTrue(listener.getDroppedEvents() > 0);
            } finally {
                release.countDown();
            }
        }
        verify(auditLog, atLeastOnce()).logCircuitBreakerTrip(any());
    }
}
