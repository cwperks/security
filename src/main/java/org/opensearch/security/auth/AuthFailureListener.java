/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.auth;

import java.net.InetAddress;

import org.opensearch.security.user.AuthCredentials;

public interface AuthFailureListener {
    void onAuthFailure(InetAddress remoteAddress, AuthCredentials authCredentials, Object request);
}
