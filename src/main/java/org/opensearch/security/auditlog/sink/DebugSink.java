/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.auditlog.sink;

import org.opensearch.common.settings.Settings;
import org.opensearch.security.auditlog.impl.AuditMessage;

public final class DebugSink extends AuditLogSink {

    public DebugSink(String name, Settings settings, AuditLogSink fallbackSink) {
        super(name, settings, null, fallbackSink);
    }

    @Override
    public boolean isHandlingBackpressure() {
        return true;
    }

    @Override
    public boolean doStore(final AuditMessage msg) {
        System.out.println("AUDIT_LOG: " + msg.toPrettyString());
        return true;
    }

}
