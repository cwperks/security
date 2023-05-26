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
import java.nio.file.Path;

import org.opensearch.common.settings.Settings;
import org.opensearch.security.auth.AuthFailureListener;
import org.opensearch.security.auth.blocking.ClientBlockRegistry;
import org.opensearch.security.auth.blocking.HeapBasedClientBlockRegistry;
import org.opensearch.security.user.AuthCredentials;
import org.opensearch.security.util.ratetracking.RateTracker;

public abstract class AbstractRateLimiter<ClientIdType> implements AuthFailureListener, ClientBlockRegistry<ClientIdType> {
    protected final ClientBlockRegistry<ClientIdType> clientBlockRegistry;
    protected final RateTracker<ClientIdType> rateTracker;

    public AbstractRateLimiter(Settings settings, Path configPath, Class<ClientIdType> clientIdType) {
        this.clientBlockRegistry = new HeapBasedClientBlockRegistry<>(
            settings.getAsInt("block_expiry_seconds", 60 * 10) * 1000,
            settings.getAsInt("max_blocked_clients", 100_000),
            clientIdType
        );
        this.rateTracker = RateTracker.create(
            settings.getAsInt("time_window_seconds", 60 * 60) * 1000,
            settings.getAsInt("allowed_tries", 10),
            settings.getAsInt("max_tracked_clients", 100_000)
        );
    }

    @Override
    public abstract void onAuthFailure(InetAddress remoteAddress, AuthCredentials authCredentials, Object request);

    @Override
    public boolean isBlocked(ClientIdType clientId) {
        return clientBlockRegistry.isBlocked(clientId);
    }

    @Override
    public void block(ClientIdType clientId) {
        clientBlockRegistry.block(clientId);
        rateTracker.reset(clientId);
    }

    @Override
    public Class<ClientIdType> getClientIdType() {
        return clientBlockRegistry.getClientIdType();
    }
}
