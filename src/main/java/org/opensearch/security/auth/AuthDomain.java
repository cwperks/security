/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.auth;

import java.util.Objects;

public class AuthDomain implements Comparable<AuthDomain> {

    private final AuthenticationBackend backend;
    private final HTTPAuthenticator httpAuthenticator;
    private final int order;
    private final boolean challenge;

    public AuthDomain(final AuthenticationBackend backend, final HTTPAuthenticator httpAuthenticator, boolean challenge, final int order) {
        super();
        this.backend = Objects.requireNonNull(backend);
        this.httpAuthenticator = httpAuthenticator;
        this.order = order;
        this.challenge = challenge;
    }

    public boolean isChallenge() {
        return challenge;
    }

    public AuthenticationBackend getBackend() {
        return backend;
    }

    public HTTPAuthenticator getHttpAuthenticator() {
        return httpAuthenticator;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return "AuthDomain [backend="
            + backend
            + ", httpAuthenticator="
            + httpAuthenticator
            + ", order="
            + order
            + ", challenge="
            + challenge
            + "]";
    }

    @Override
    public int compareTo(final AuthDomain o) {
        return Integer.compare(this.order, o.order);
    }
}
