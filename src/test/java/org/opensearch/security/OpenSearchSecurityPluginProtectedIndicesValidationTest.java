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

package org.opensearch.security;

import java.util.List;

import org.junit.Test;

import org.opensearch.common.settings.Settings;
import org.opensearch.indices.SystemIndexDescriptor;
import org.opensearch.security.support.ConfigConstants;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThrows;

public class OpenSearchSecurityPluginProtectedIndicesValidationTest {

    private static final List<SystemIndexDescriptor> SYSTEM_INDEX_DESCRIPTORS = List.of(
        new SystemIndexDescriptor(".opendistro_security", "Security index"),
        new SystemIndexDescriptor(".opensearch-notifications-*", "Notifications index")
    );

    @Test
    public void rejectsExactOverlapWithSystemIndexDescriptor() {
        Settings settings = protectedIndicesSettings(".opendistro_security").build();

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> OpenSearchSecurityPlugin.validateProtectedIndicesDoNotOverlapSystemIndices(settings, SYSTEM_INDEX_DESCRIPTORS)
        );

        assertThat(exception.getMessage(), containsString(".opendistro_security"));
    }

    @Test
    public void rejectsWildcardOverlapWithSystemIndexDescriptor() {
        Settings settings = protectedIndicesSettings(".opensearch-*").build();

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> OpenSearchSecurityPlugin.validateProtectedIndicesDoNotOverlapSystemIndices(settings, SYSTEM_INDEX_DESCRIPTORS)
        );

        assertThat(exception.getMessage(), containsString(".opensearch-notifications-*"));
    }

    @Test
    public void rejectsOverlapWithConfiguredSystemIndex() {
        Settings settings = protectedIndicesSettings("business-*-2026").put(ConfigConstants.SECURITY_SYSTEM_INDICES_ENABLED_KEY, true)
            .putList(ConfigConstants.SECURITY_SYSTEM_INDICES_KEY, "business-results-*")
            .build();

        assertThrows(
            IllegalStateException.class,
            () -> OpenSearchSecurityPlugin.validateProtectedIndicesDoNotOverlapSystemIndices(settings, SYSTEM_INDEX_DESCRIPTORS)
        );
    }

    @Test
    public void allowsDisabledProtectedIndices() {
        Settings settings = protectedIndicesSettings(".opendistro_security").put(
            ConfigConstants.SECURITY_PROTECTED_INDICES_ENABLED_KEY,
            false
        ).build();

        OpenSearchSecurityPlugin.validateProtectedIndicesDoNotOverlapSystemIndices(settings, SYSTEM_INDEX_DESCRIPTORS);
    }

    @Test
    public void allowsDisjointPatterns() {
        Settings settings = protectedIndicesSettings("business-*").build();

        OpenSearchSecurityPlugin.validateProtectedIndicesDoNotOverlapSystemIndices(settings, SYSTEM_INDEX_DESCRIPTORS);
    }

    private static Settings.Builder protectedIndicesSettings(String pattern) {
        return Settings.builder()
            .put(ConfigConstants.SECURITY_PROTECTED_INDICES_ENABLED_KEY, true)
            .putList(ConfigConstants.SECURITY_PROTECTED_INDICES_KEY, pattern);
    }
}
