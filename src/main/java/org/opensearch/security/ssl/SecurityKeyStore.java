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

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

public interface SecurityKeyStore {

    public SSLEngine createHTTPSSLEngine() throws SSLException;

    public SSLEngine createServerTransportSSLEngine() throws SSLException;

    public SSLEngine createClientTransportSSLEngine(String peerHost, int peerPort) throws SSLException;

    public String getHTTPProviderName();

    public String getTransportServerProviderName();

    public String getTransportClientProviderName();

    public String getSubjectAlternativeNames(X509Certificate cert);

    public void initHttpSSLConfig();

    public void initTransportSSLConfig();

    public X509Certificate[] getTransportCerts();

    public X509Certificate[] getHttpCerts();
}
