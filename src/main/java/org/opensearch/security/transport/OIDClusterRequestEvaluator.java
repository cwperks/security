/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.transport;

import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.opensearch.common.settings.Settings;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.transport.TransportRequest;

// CS-SUPPRESS-SINGLE: RegexpSingleline Java Cryptography Extension is unrelated to OpenSearch extensions
/**
 * Implementation to evaluate a certificate extension with a given OID
 * and value to the same value found on the peer certificate
 *
 */
// CS-ENFORCE-SINGLE
public final class OIDClusterRequestEvaluator implements InterClusterRequestEvaluator {
    private final String certOid;

    public OIDClusterRequestEvaluator(final Settings settings) {
        this.certOid = settings.get(ConfigConstants.SECURITY_CERT_OID, "1.2.3.4.5.5");
    }

    @Override
    public boolean isInterClusterRequest(
        TransportRequest request,
        X509Certificate[] localCerts,
        X509Certificate[] peerCerts,
        final String principal
    ) {
        if (localCerts != null && localCerts.length > 0 && peerCerts != null && peerCerts.length > 0) {
            // CS-SUPPRESS-SINGLE: RegexpSingleline Java Cryptography Extension is unrelated to OpenSearch extensions
            final byte[] localValue = localCerts[0].getExtensionValue(certOid);
            final byte[] peerValue = peerCerts[0].getExtensionValue(certOid);
            // CS-ENFORCE-SINGLE
            if (localValue != null && peerValue != null) {
                return Arrays.equals(localValue, peerValue);
            }
        }
        return false;
    }

}
