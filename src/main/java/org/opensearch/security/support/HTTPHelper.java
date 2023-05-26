/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.support;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import org.opensearch.rest.RestRequest;
import org.opensearch.security.user.AuthCredentials;

public class HTTPHelper {

    public static AuthCredentials extractCredentials(String authorizationHeader, Logger log) {

        if (authorizationHeader != null) {
            if (!authorizationHeader.trim().toLowerCase().startsWith("basic ")) {
                log.warn("No 'Basic Authorization' header, send 401 and 'WWW-Authenticate Basic'");
                return null;
            } else {

                final String decodedBasicHeader = new String(
                    Base64.getDecoder().decode(authorizationHeader.split(" ")[1]),
                    StandardCharsets.UTF_8
                );

                // username:password
                // special case
                // username must not contain a :, but password is allowed to do so
                // username:pass:word
                // blank password
                // username:

                final int firstColonIndex = decodedBasicHeader.indexOf(':');

                String username = null;
                String password = null;

                if (firstColonIndex > 0) {
                    username = decodedBasicHeader.substring(0, firstColonIndex);

                    if (decodedBasicHeader.length() - 1 != firstColonIndex) {
                        password = decodedBasicHeader.substring(firstColonIndex + 1);
                    } else {
                        // blank password
                        password = "";
                    }
                }

                if (username == null || password == null) {
                    log.warn("Invalid 'Authorization' header, send 401 and 'WWW-Authenticate Basic'");
                    return null;
                } else {
                    return new AuthCredentials(username, password.getBytes(StandardCharsets.UTF_8)).markComplete();
                }
            }
        } else {
            return null;
        }
    }

    public static boolean containsBadHeader(final RestRequest request) {

        final Map<String, List<String>> headers;

        if (request != null && (headers = request.getHeaders()) != null) {
            for (final String key : headers.keySet()) {
                if (key != null && key.trim().toLowerCase().startsWith(ConfigConstants.OPENDISTRO_SECURITY_CONFIG_PREFIX.toLowerCase())) {
                    return true;
                }
            }
        }

        return false;
    }
}
