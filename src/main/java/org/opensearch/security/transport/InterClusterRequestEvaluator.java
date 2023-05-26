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

import org.opensearch.transport.TransportRequest;

/**
 * Evaluates a request to determine if it is
 * intercluster communication.  Implementations
 * should include a single arg constructor that
 * takes org.opensearch.common.settings.Settings
 *
 */
public interface InterClusterRequestEvaluator {

    /**
     * Determine if request is a message from
     * another node in the cluster
     *
     * @param   request     The transport request to evaluate
     * @param   localCerts  Local certs to use for evaluating the request which include criteria
     *                      specific to the implementation for confirming intercluster
     *                      communication
     *
     * @param   peerCerts       Certs to use for evaluating the request which include criteria
     *                      specific to the implementation for confirming intercluster
     *                      communication
     *
     * @param principal    The principal evaluated by the configured principal extractor
     *
     * @return True when determined to be intercluster, false otherwise
     */
    boolean isInterClusterRequest(
        final TransportRequest request,
        final X509Certificate[] localCerts,
        final X509Certificate[] peerCerts,
        final String principal
    );
}
