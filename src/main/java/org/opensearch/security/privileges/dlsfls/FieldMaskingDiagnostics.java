/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.privileges.dlsfls;

import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Diagnostics scoped to one role validation; no state is shared between requests. */
public final class FieldMaskingDiagnostics {
    private static final Logger LOGGER = LogManager.getLogger(FieldMaskingDiagnostics.class);
    static final int MAX_WARNINGS_PER_VALIDATION = 10;
    private static final int MAX_IDENTIFIER_LENGTH = 256;

    record Warning(String index, String field, String type, String origin) {
    }

    private final Consumer<Warning> sink;
    private int warnings;

    public FieldMaskingDiagnostics() {
        this(
            warning -> LOGGER.warn(
                "Field masking cannot guarantee protection for index [{}], field [{}], type [{}], detected by [{}]. "
                    + "Only string values are masked; use FLS to hide unsupported values. At most {} warnings are emitted per role validation.",
                warning.index(),
                warning.field(),
                warning.type(),
                warning.origin(),
                MAX_WARNINGS_PER_VALIDATION
            )
        );
    }

    FieldMaskingDiagnostics(Consumer<Warning> sink) {
        this.sink = sink;
    }

    public void warn(String index, String field, String type, String origin) {
        if (warnings >= MAX_WARNINGS_PER_VALIDATION) return;
        warnings++;
        sink.accept(new Warning(safe(index), safe(field), safe(type), safe(origin)));
    }

    private static String safe(String value) {
        if (value == null) return "unknown";
        return value.substring(0, Math.min(value.length(), MAX_IDENTIFIER_LENGTH)).replaceAll("[\\p{Cntrl}]", "?");
    }
}
