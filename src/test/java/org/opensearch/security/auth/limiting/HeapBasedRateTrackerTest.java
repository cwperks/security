/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.auth.limiting;

import org.junit.Ignore;
import org.junit.Test;

import org.opensearch.security.util.ratetracking.HeapBasedRateTracker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HeapBasedRateTrackerTest {

    @Test
    public void simpleTest() throws Exception {
        HeapBasedRateTracker<String> tracker = new HeapBasedRateTracker<>(100, 5, 100_000);

        assertFalse(tracker.track("a"));
        assertFalse(tracker.track("a"));
        assertFalse(tracker.track("a"));
        assertFalse(tracker.track("a"));
        assertTrue(tracker.track("a"));

    }

    @Test
    @Ignore // https://github.com/opensearch-project/security/issues/2193
    public void expiryTest() throws Exception {
        HeapBasedRateTracker<String> tracker = new HeapBasedRateTracker<>(100, 5, 100_000);

        assertFalse(tracker.track("a"));
        assertFalse(tracker.track("a"));
        assertFalse(tracker.track("a"));
        assertFalse(tracker.track("a"));
        assertTrue(tracker.track("a"));

        assertFalse(tracker.track("b"));
        assertFalse(tracker.track("b"));
        assertFalse(tracker.track("b"));
        assertFalse(tracker.track("b"));
        assertTrue(tracker.track("b"));

        assertFalse(tracker.track("c"));

        Thread.sleep(50);

        assertFalse(tracker.track("c"));
        assertFalse(tracker.track("c"));
        assertFalse(tracker.track("c"));

        Thread.sleep(55);

        assertFalse(tracker.track("c"));
        assertTrue(tracker.track("c"));

        assertFalse(tracker.track("a"));

        Thread.sleep(55);
        assertFalse(tracker.track("c"));
        assertFalse(tracker.track("c"));
        assertTrue(tracker.track("c"));

    }

    @Test
    @Ignore // https://github.com/opensearch-project/security/issues/2193
    public void maxTwoTriesTest() throws Exception {
        HeapBasedRateTracker<String> tracker = new HeapBasedRateTracker<>(100, 2, 100_000);

        assertFalse(tracker.track("a"));
        assertTrue(tracker.track("a"));

        assertFalse(tracker.track("b"));
        Thread.sleep(50);
        assertTrue(tracker.track("b"));

        Thread.sleep(55);
        assertTrue(tracker.track("b"));

        Thread.sleep(105);
        assertFalse(tracker.track("b"));
        assertTrue(tracker.track("b"));

    }
}
