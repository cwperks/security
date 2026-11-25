/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.auditlog.impl;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.indices.breaker.CircuitBreakerTripNotifier;
import org.opensearch.security.auditlog.AuditLog.Origin;
import org.opensearch.security.filter.SecurityRestFilter;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.support.HeaderHelper;
import org.opensearch.security.user.User;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.ThreadPool;

/**
 * Converts breaker trips to audit events outside the breaker thread.
 */
public final class CircuitBreakerAuditListener implements AutoCloseable {
    private static final Logger log = LogManager.getLogger(CircuitBreakerAuditListener.class);
    private static final int QUEUE_CAPACITY = 256;
    private static final int MAX_LABEL_LENGTH = 256;

    private final CircuitBreakerTripNotifier notifier;
    private final AuditLogImpl auditLog;
    private final ClusterService clusterService;
    private final ThreadPool threadPool;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
        1,
        1,
        0,
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(QUEUE_CAPACITY)
    );
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean warnedAboutMissingOrigin = new AtomicBoolean();
    private final AtomicBoolean warnedAboutOversizedUser = new AtomicBoolean();
    private final AtomicLong droppedEvents = new AtomicLong();
    private final Consumer<CircuitBreakerTripNotifier.Trip> listener = this::onTrip;

    public CircuitBreakerAuditListener(
        CircuitBreakerTripNotifier notifier,
        AuditLogImpl auditLog,
        ClusterService clusterService,
        ThreadPool threadPool
    ) {
        this.notifier = notifier;
        this.auditLog = auditLog;
        this.clusterService = clusterService;
        this.threadPool = threadPool;
        notifier.addListener(listener);
    }

    private void onTrip(CircuitBreakerTripNotifier.Trip trip) {
        ThreadContext context = threadPool.getThreadContext();
        if (closed.get() || "true".equals(HeaderHelper.getSafeFromHeader(context, ConfigConstants.SECURITY_AUDIT_SINK_REQUEST_HEADER))) {
            return;
        }
        User user = context.getTransient(ConfigConstants.OPENDISTRO_SECURITY_USER);
        String userName = user == null ? null : user.getName();
        String originValue = context.getTransient(ConfigConstants.OPENDISTRO_SECURITY_ORIGIN);
        Origin origin = switch (originValue == null ? "" : originValue) {
            case "REST" -> Origin.REST;
            case "TRANSPORT" -> Origin.TRANSPORT;
            case "GRPC" -> Origin.GRPC;
            case "LOCAL" -> Origin.LOCAL;
            default -> null;
        };
        if (!auditLog.isCircuitBreakerAuditEnabled(userName, origin)) {
            return;
        }
        if (userName != null && userName.length() > MAX_LABEL_LENGTH) {
            if (warnedAboutOversizedUser.compareAndSet(false, true)) {
                log.warn("Skipping circuit breaker audit events with oversized user names");
            }
            return;
        }
        // Before transport dispatch the audit-sink marker is not in the thread context.
        // Unknown-origin trips are skipped except for the identifiable HTTP ingress path.
        if (origin == null && !"<http_request>".equals(trip.label())) {
            if (warnedAboutMissingOrigin.compareAndSet(false, true)) {
                log.warn("Skipping circuit breaker audit events without a verified origin to avoid auditing audit-sink writes");
            }
            return;
        }
        String requestId = context.getTransient(ConfigConstants.SECURITY_AUDIT_REQUEST_ID);
        if (requestId == null) {
            String header = context.getHeader(Task.X_REQUEST_ID);
            requestId = header == null ? null : SecurityRestFilter.sanitizeRequestId(header);
        }
        String finalRequestId = requestId == null || requestId.length() <= MAX_LABEL_LENGTH ? requestId : null;
        String breaker = trip.breaker();
        if (breaker != null && breaker.length() > MAX_LABEL_LENGTH) {
            breaker = breaker.substring(0, MAX_LABEL_LENGTH);
        }
        String finalBreaker = breaker;
        String label = trip.label();
        if (label != null && label.length() > MAX_LABEL_LENGTH) {
            label = label.substring(0, MAX_LABEL_LENGTH);
        }
        String finalLabel = label;
        long bytesWanted = trip.bytesWanted();
        long bytesLimit = trip.bytesLimit();
        String durability = trip.durability();
        try {
            executor.execute(() -> {
                try {
                    if (!auditLog.isCircuitBreakerAuditEnabled(userName, origin)) {
                        return;
                    }
                    AuditMessage message = new AuditMessage(AuditCategory.CIRCUIT_BREAKER_TRIPPED, clusterService, origin, null);
                    message.addEffectiveUser(userName);
                    message.addRequestId(finalRequestId);
                    message.addCircuitBreakerDetails(finalBreaker, finalLabel, bytesWanted, bytesLimit, durability);
                    auditLog.logCircuitBreakerTrip(message);
                } catch (RuntimeException e) {
                    log.error("Failed to record circuit breaker audit event", e);
                }
            });
        } catch (RejectedExecutionException e) {
            if (!closed.get()) {
                long dropped = droppedEvents.incrementAndGet();
                if ((dropped & (dropped - 1)) == 0) {
                    log.warn("Dropped {} circuit breaker audit events because the audit queue is full", dropped);
                }
            }
        }
    }

    long getDroppedEvents() {
        return droppedEvents.get();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        notifier.removeListener(listener);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                int dropped = executor.shutdownNow().size();
                log.warn("Dropped {} queued circuit breaker audit events during shutdown", dropped);
            }
        } catch (InterruptedException e) {
            int dropped = executor.shutdownNow().size();
            log.warn("Interrupted while stopping circuit breaker audit listener; dropped {} queued events", dropped);
            Thread.currentThread().interrupt();
        }
    }
}
