/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.util.ratetracking;

public interface RateTracker<ClientIdType> {

    boolean track(ClientIdType clientId);

    void reset(ClientIdType clientId);

    static <ClientIdType> RateTracker<ClientIdType> create(long timeWindowMs, int allowedTries, int maxEntries) {
        if (allowedTries == 1) {
            return new SingleTryRateTracker<ClientIdType>();
        } else if (allowedTries > 1) {
            return new HeapBasedRateTracker<ClientIdType>(timeWindowMs, allowedTries, maxEntries);
        } else {
            throw new IllegalArgumentException("allowedTries must be > 0: " + allowedTries);
        }
    }

}
