/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.ssl;

import java.security.cert.X509Certificate;

import org.opensearch.security.ssl.transport.PrincipalExtractor;

public class TestPrincipalExtractor implements PrincipalExtractor {

    private static int transportCount = 0;
    private static int httpCount = 0;

    public TestPrincipalExtractor() {}

    @Override
    public String extractPrincipal(X509Certificate x509Certificate, Type type) {
        if (type == Type.HTTP) {
            httpCount++;
        }

        if (type == Type.TRANSPORT) {
            transportCount++;
        }

        return "testdn";
    }

    public static int getTransportCount() {
        return transportCount;
    }

    public static int getHttpCount() {
        return httpCount;
    }

    public static void reset() {
        httpCount = 0;
        transportCount = 0;
    }

}
