/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.support;

import org.opensearch.indices.SystemIndexRegistry;

public final class CcrSystemIndexReplication {

    private CcrSystemIndexReplication() {}

    public static boolean isMarkedReplicatedSystemIndex(String index, String replicatedSystemIndex) {
        return index != null && index.equals(replicatedSystemIndex) && SystemIndexRegistry.matchesSystemIndexPattern(index);
    }
}
