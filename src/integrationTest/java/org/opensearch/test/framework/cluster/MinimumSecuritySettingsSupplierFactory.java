/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.test.framework.cluster;

import org.opensearch.common.settings.Settings;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.test.framework.certificate.TestCertificates;

public class MinimumSecuritySettingsSupplierFactory {

    private final String PRIVATE_KEY_HTTP_PASSWORD = "aWVV63OJ4qzZyPrBwl2MFny4ZV8lQRZchjL";
    private final String PRIVATE_KEY_TRANSPORT_PASSWORD = "iWbUv9w79sbd5tcxvSJNfHXS9GhcPCvdw9x";

    private TestCertificates testCertificates;

    public MinimumSecuritySettingsSupplierFactory(TestCertificates testCertificates) {
        if (testCertificates == null) {
            throw new IllegalArgumentException("certificates must not be null");
        }
        this.testCertificates = testCertificates;

    }

    public NodeSettingsSupplier minimumOpenSearchSettings(boolean sslOnly, Settings other) {
        return i -> minimumOpenSearchSettingsBuilder(i, sslOnly).put(other).build();
    }

    private Settings.Builder minimumOpenSearchSettingsBuilder(int node, boolean sslOnly) {

        Settings.Builder builder = Settings.builder();

        builder.put("plugins.security.ssl.transport.pemtrustedcas_filepath", testCertificates.getRootCertificate().getAbsolutePath());
        builder.put("plugins.security.ssl.transport.pemcert_filepath", testCertificates.getNodeCertificate(node).getAbsolutePath());
        builder.put(
            "plugins.security.ssl.transport.pemkey_filepath",
            testCertificates.getNodeKey(node, PRIVATE_KEY_TRANSPORT_PASSWORD).getAbsolutePath()
        );
        builder.put("plugins.security.ssl.transport.pemkey_password", PRIVATE_KEY_TRANSPORT_PASSWORD);

        builder.put("plugins.security.ssl.http.enabled", true);
        builder.put("plugins.security.ssl.http.pemtrustedcas_filepath", testCertificates.getRootCertificate().getAbsolutePath());
        builder.put("plugins.security.ssl.http.pemcert_filepath", testCertificates.getNodeCertificate(node).getAbsolutePath());
        builder.put(
            "plugins.security.ssl.http.pemkey_filepath",
            testCertificates.getNodeKey(node, PRIVATE_KEY_HTTP_PASSWORD).getAbsolutePath()
        );
        builder.put("plugins.security.ssl.http.pemkey_password", PRIVATE_KEY_HTTP_PASSWORD);
        if (sslOnly == false) {
            builder.put(ConfigConstants.SECURITY_BACKGROUND_INIT_IF_SECURITYINDEX_NOT_EXIST, false);
            builder.putList("plugins.security.authcz.admin_dn", testCertificates.getAdminDNs());
            builder.put("plugins.security.compliance.salt", "1234567890123456");
            builder.put("plugins.security.audit.type", "noop");
            builder.put("plugins.security.background_init_if_securityindex_not_exist", "false");
        }
        return builder;

    }
}
