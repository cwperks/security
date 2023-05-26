/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.auth.limiting;

import java.net.InetAddress;

import org.junit.Test;

import org.opensearch.common.settings.Settings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserNameBasedRateLimiterTest {

    @Test
    public void simpleTest() throws Exception {
        Settings settings = Settings.builder().put("allowed_tries", 3).build();

        AddressBasedRateLimiter rateLimiter = new AddressBasedRateLimiter(settings, null);

        assertFalse(rateLimiter.isBlocked(InetAddress.getByAddress(new byte[] { 1, 2, 3, 4 })));
        rateLimiter.onAuthFailure(InetAddress.getByAddress(new byte[] { 1, 2, 3, 4 }), null, null);
        assertFalse(rateLimiter.isBlocked(InetAddress.getByAddress(new byte[] { 1, 2, 3, 4 })));
        rateLimiter.onAuthFailure(InetAddress.getByAddress(new byte[] { 1, 2, 3, 4 }), null, null);
        assertFalse(rateLimiter.isBlocked(InetAddress.getByAddress(new byte[] { 1, 2, 3, 4 })));
        rateLimiter.onAuthFailure(InetAddress.getByAddress(new byte[] { 1, 2, 3, 4 }), null, null);
        assertTrue(rateLimiter.isBlocked(InetAddress.getByAddress(new byte[] { 1, 2, 3, 4 })));

    }
}
