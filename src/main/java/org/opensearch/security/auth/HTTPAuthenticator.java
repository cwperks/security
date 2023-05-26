/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.auth;

import org.opensearch.OpenSearchSecurityException;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.rest.RestChannel;
import org.opensearch.rest.RestRequest;
import org.opensearch.security.user.AuthCredentials;

/**
 * OpenSearch Security custom HTTP authenticators need to implement this interface.
 * <p/>
 * A HTTP authenticator extracts {@link AuthCredentials} from a {@link RestRequest}
 * <p/>
 *
 * Implementation classes must provide a public constructor
 * <p/>
 * {@code public MyHTTPAuthenticator(org.opensearch.common.settings.Settings settings, java.nio.file.Path configPath)}
 * <p/>
 * The constructor should not throw any exception in case of an initialization problem.
 * Instead catch all exceptions and log a appropriate error message. A logger can be instantiated like:
 * <p/>
 * {@code private final Logger log = LogManager.getLogger(this.getClass());}
 * <p/>
 */
public interface HTTPAuthenticator {

    /**
     * The type (name) of the authenticator. Only for logging.
     * @return the type
     */
    String getType();

    /**
     * Extract {@link AuthCredentials} from {@link RestRequest}
     *
     * @param request The rest request
     * @param context The current thread context
     * @return The authentication credentials (complete or incomplete) or null when no credentials are found in the request
     * <p>
     * When the credentials could be fully extracted from the request {@code .markComplete()} must be called on the {@link AuthCredentials} which are returned.
     * If the authentication flow needs another roundtrip with the request originator do not mark it as complete.
     * @throws OpenSearchSecurityException
     */
    AuthCredentials extractCredentials(RestRequest request, ThreadContext context) throws OpenSearchSecurityException;

    /**
     * If the {@code extractCredentials()} call was not successful or the authentication flow needs another roundtrip this method
     * will be called. If the custom HTTP authenticator does not support this method is a no-op and false should be returned.
     *
     * If the custom HTTP authenticator does support re-request authentication or supports authentication flows with multiple roundtrips
     * then the response should be sent (through the channel) and true must be returned.
     *
     * @param channel The rest channel to sent back the response via {@code channel.sendResponse()}
     * @param credentials The credentials from the prior authentication attempt
     * @return false  if re-request is not supported/necessary, true otherwise.
     * If true is returned {@code channel.sendResponse()} must be called so that the request completes.
     */
    boolean reRequestAuthentication(final RestChannel channel, AuthCredentials credentials);
}
