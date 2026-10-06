/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.privileges.dlsfls;

import java.util.ArrayList;

import org.apache.lucene.tests.util.LuceneTestCase;

public class FieldMaskingDiagnosticsTests extends LuceneTestCase {
    public void testWarningsAreBoundedPerValidation() {
        var warnings = new ArrayList<FieldMaskingDiagnostics.Warning>();
        var diagnostics = new FieldMaskingDiagnostics(warnings::add);
        for (int i = 0; i < FieldMaskingDiagnostics.MAX_WARNINGS_PER_VALIDATION + 2; i++) {
            diagnostics.warn("index", "field", "integer", "role mapping inspection");
        }
        assertEquals(FieldMaskingDiagnostics.MAX_WARNINGS_PER_VALIDATION, warnings.size());
        // An immediate second validation can report the same field again.
        new FieldMaskingDiagnostics(warnings::add).warn("index", "field", "integer", "role mapping inspection");
        assertEquals(FieldMaskingDiagnostics.MAX_WARNINGS_PER_VALIDATION + 1, warnings.size());
    }

    public void testDiagnosticIdentifiersAreBoundedAndSingleLine() {
        var warnings = new ArrayList<FieldMaskingDiagnostics.Warning>();
        new FieldMaskingDiagnostics(warnings::add).warn("index\nname", "x".repeat(500), "integer", "role mapping inspection");
        assertEquals("index?name", warnings.get(0).index());
        assertEquals(256, warnings.get(0).field().length());
    }

}
