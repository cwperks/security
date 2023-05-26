/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.auth.blocking;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HeapBasedClientBlockRegistryTest {

    @Test
    public void simpleTest() throws Exception {
        HeapBasedClientBlockRegistry<String> registry = new HeapBasedClientBlockRegistry<>(50, 3, String.class);

        assertFalse(registry.isBlocked("a"));
        registry.block("a");
        assertTrue(registry.isBlocked("a"));

        registry.block("b");
        assertTrue(registry.isBlocked("a"));
        assertTrue(registry.isBlocked("b"));

        registry.block("c");
        assertTrue(registry.isBlocked("a"));
        assertTrue(registry.isBlocked("b"));
        assertTrue(registry.isBlocked("c"));

        registry.block("d");
        assertFalse(registry.isBlocked("a"));
        assertTrue(registry.isBlocked("b"));
        assertTrue(registry.isBlocked("c"));
        assertTrue(registry.isBlocked("d"));
    }

    @Test
    public void expiryTest() throws Exception {
        HeapBasedClientBlockRegistry<String> registry = new HeapBasedClientBlockRegistry<>(50, 3, String.class);

        assertFalse(registry.isBlocked("a"));
        registry.block("a");
        assertTrue(registry.isBlocked("a"));
        Thread.sleep(55);
        assertFalse(registry.isBlocked("a"));
    }
}
