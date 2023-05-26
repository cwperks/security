/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security;

import java.security.cert.X509Certificate;

import org.opensearch.common.settings.Settings;
import org.opensearch.security.transport.InterClusterRequestEvaluator;
import org.opensearch.transport.TransportRequest;

public class AlwaysFalseInterClusterRequestEvaluator implements InterClusterRequestEvaluator {

    public AlwaysFalseInterClusterRequestEvaluator(Settings settings) {
        super();
    }

    @Override
    public boolean isInterClusterRequest(
        TransportRequest request,
        X509Certificate[] localCerts,
        X509Certificate[] peerCerts,
        String principal
    ) {

        if (localCerts == null
            || peerCerts == null
            || principal == null
            || localCerts.length == 0
            || peerCerts.length == 0
            || principal.length() == 0) {
            return true;
        }

        return false;
    }

}
