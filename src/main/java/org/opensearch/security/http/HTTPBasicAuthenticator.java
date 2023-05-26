/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.http;

import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestChannel;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestStatus;
import org.opensearch.security.auth.HTTPAuthenticator;
import org.opensearch.security.support.HTTPHelper;
import org.opensearch.security.user.AuthCredentials;

//TODO FUTURE allow only if protocol==https
public class HTTPBasicAuthenticator implements HTTPAuthenticator {

    protected final Logger log = LogManager.getLogger(this.getClass());

    public HTTPBasicAuthenticator(final Settings settings, final Path configPath) {

    }

    @Override
    public AuthCredentials extractCredentials(final RestRequest request, ThreadContext threadContext) {

        final boolean forceLogin = request.paramAsBoolean("force_login", false);

        if (forceLogin) {
            return null;
        }

        final String authorizationHeader = request.header("Authorization");

        return HTTPHelper.extractCredentials(authorizationHeader, log);
    }

    @Override
    public boolean reRequestAuthentication(final RestChannel channel, AuthCredentials creds) {
        final BytesRestResponse wwwAuthenticateResponse = new BytesRestResponse(RestStatus.UNAUTHORIZED, "Unauthorized");
        wwwAuthenticateResponse.addHeader("WWW-Authenticate", "Basic realm=\"OpenSearch Security\"");
        channel.sendResponse(wwwAuthenticateResponse);
        return true;
    }

    @Override
    public String getType() {
        return "basic";
    }
}
