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
import org.opensearch.security.user.AuthCredentials;

public class UserNameBasedRateLimiter extends AbstractRateLimiter<String> implements AuthFailureListener, ClientBlockRegistry<String> {

    public UserNameBasedRateLimiter(Settings settings, Path configPath) {
        super(settings, configPath, String.class);
    }

    @Override
    public void onAuthFailure(InetAddress remoteAddress, AuthCredentials authCredentials, Object request) {
        if (authCredentials != null && authCredentials.getUsername() != null && this.rateTracker.track(authCredentials.getUsername())) {
            block(authCredentials.getUsername());
        }
    }
}
