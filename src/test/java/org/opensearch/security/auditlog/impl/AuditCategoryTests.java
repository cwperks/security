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

package org.opensearch.security.auditlog.impl;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.tests.util.LuceneTestCase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.opensearch.security.auditlog.impl.AuditCategory.AUTHENTICATED;
import static org.opensearch.security.auditlog.impl.AuditCategory.BAD_HEADERS;
import static org.junit.Assert.assertThrows;

public class AuditCategoryTests extends LuceneTestCase {

    public void testAuditCategoryEnumSetGeneration() {
        assertParsedCategories(Arrays.asList(), EnumSet.noneOf(AuditCategory.class));
        assertParsedCategories(Arrays.asList("BAD_HEADERS"), EnumSet.of(BAD_HEADERS));
        assertParsedCategories(Arrays.asList("bad_headers"), EnumSet.of(BAD_HEADERS));
        assertParsedCategories(Arrays.asList("bAd_HeAdErS"), EnumSet.of(BAD_HEADERS));
        assertParsedCategories(Arrays.asList("BAD_HEADERS", "AUTHENTICATED"), EnumSet.of(BAD_HEADERS, AUTHENTICATED));
        assertParsedCategories(
            Arrays.asList(
                "BAD_HEADERS",
                "FAILED_LOGIN",
                "MISSING_PRIVILEGES",
                "GRANTED_PRIVILEGES",
                "OPENDISTRO_SECURITY_INDEX_ATTEMPT",
                "SSL_EXCEPTION",
                "AUTHENTICATED",
                "INDEX_EVENT",
                "COMPLIANCE_DOC_READ",
                "COMPLIANCE_DOC_WRITE",
                "COMPLIANCE_EXTERNAL_CONFIG",
                "COMPLIANCE_INTERNAL_CONFIG_READ",
                "COMPLIANCE_INTERNAL_CONFIG_WRITE",
                "CLUSTER_SETTINGS_CHANGED",
                "INDEX_SETTINGS_CHANGED",
                "API_TOKEN_WRITE"
            ),
            EnumSet.allOf(AuditCategory.class)
        );
    }

    public void testInvalidCategoryThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> AuditCategory.parse(Arrays.asList("BAD_INPUT")));
        assertThrows(
            IllegalArgumentException.class,
            () -> AuditCategory.parse(Arrays.asList("BAD_HEADERS", "bad_category", "AUTHENTICATED"))
        );
    }

    public void testNullInputThrowsException() {
        assertThrows(NullPointerException.class, () -> AuditCategory.parse(null));
    }

    private void assertParsedCategories(List<String> input, EnumSet<AuditCategory> expected) {
        Set<AuditCategory> categories = AuditCategory.parse(input);
        assertThat(categories, is(expected));
    }
}
