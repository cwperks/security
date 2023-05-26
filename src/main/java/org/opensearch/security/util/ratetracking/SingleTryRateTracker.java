/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.util.ratetracking;

public class SingleTryRateTracker<ClientIdType> implements RateTracker<ClientIdType> {

    @Override
    public boolean track(ClientIdType clientId) {
        return true;
    }

    @Override
    public void reset(ClientIdType clientId) {}
}
