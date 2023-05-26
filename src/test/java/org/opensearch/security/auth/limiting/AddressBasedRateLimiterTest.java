/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.auth.limiting;

import org.junit.Test;

import org.opensearch.common.settings.Settings;
import org.opensearch.security.user.AuthCredentials;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AddressBasedRateLimiterTest {

    private final static byte[] PASSWORD = new byte[] { '1', '2', '3' };

    @Test
    public void simpleTest() throws Exception {
        Settings settings = Settings.builder().put("allowed_tries", 3).build();

        UserNameBasedRateLimiter rateLimiter = new UserNameBasedRateLimiter(settings, null);

        assertFalse(rateLimiter.isBlocked("a"));
        rateLimiter.onAuthFailure(null, new AuthCredentials("a", PASSWORD), null);
        assertFalse(rateLimiter.isBlocked("a"));
        rateLimiter.onAuthFailure(null, new AuthCredentials("a", PASSWORD), null);
        assertFalse(rateLimiter.isBlocked("a"));
        rateLimiter.onAuthFailure(null, new AuthCredentials("a", PASSWORD), null);
        assertTrue(rateLimiter.isBlocked("a"));

    }
}
