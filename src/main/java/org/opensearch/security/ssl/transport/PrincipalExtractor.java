/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.ssl.transport;

import java.security.cert.X509Certificate;

public interface PrincipalExtractor {

    public enum Type {
        HTTP,
        TRANSPORT
    }

    /**
     * Extract the principal name
     *
     * Please note that this method gets called for principal extraction of other nodes
     * as well as transport clients. It's up to the implementer to distinguish between them
     * and handle them appropriately.
     *
     * Implementations must be public classes with a default public default constructor.
     *
     * @param x509Certificate The first X509 certificate in the peer certificate chain
     *        This can be null, in this case the method must also return <code>null</code>.
     * @return The principal as string. This may be <code>null</code> in case where x509Certificate is null
     *        or the principal cannot be extracted because of any other circumstances.
     */
    String extractPrincipal(X509Certificate x509Certificate, Type type);

}
