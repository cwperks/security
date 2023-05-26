/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.auth.internal;

import java.nio.file.Path;

import org.opensearch.common.settings.Settings;
import org.opensearch.security.auth.AuthenticationBackend;
import org.opensearch.security.user.AuthCredentials;
import org.opensearch.security.user.User;

public class NoOpAuthenticationBackend implements AuthenticationBackend {

    public NoOpAuthenticationBackend(final Settings settings, final Path configPath) {
        super();
    }

    @Override
    public String getType() {
        return "noop";
    }

    @Override
    public User authenticate(final AuthCredentials credentials) {
        return new User(credentials.getUsername(), credentials.getBackendRoles(), credentials);
    }

    @Override
    public boolean exists(User user) {
        return true;
    }

}
