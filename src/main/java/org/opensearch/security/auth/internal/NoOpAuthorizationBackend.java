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
import org.opensearch.security.auth.AuthorizationBackend;
import org.opensearch.security.user.AuthCredentials;
import org.opensearch.security.user.User;

public class NoOpAuthorizationBackend implements AuthorizationBackend {

    public NoOpAuthorizationBackend(final Settings settings, final Path configPath) {
        super();
    }

    @Override
    public String getType() {
        return "noop";
    }

    @Override
    public void fillRoles(final User user, final AuthCredentials authCreds) {
        // no-op
    }

}
