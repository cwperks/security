/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.dlic.rest.api.legacy;

import org.opensearch.security.dlic.rest.api.SslCertsApiTest;

import static org.opensearch.security.OpenSearchSecurityPlugin.LEGACY_OPENDISTRO_PREFIX;

public class LegacySslCertsApiTest extends SslCertsApiTest {

    @Override
    public String certsInfoEndpoint() {
        return LEGACY_OPENDISTRO_PREFIX + "/api/ssl/certs";
    }

    @Override
    public String certsReloadEndpoint(String certType) {
        return String.format("%s/api/ssl/%s/reloadcerts", LEGACY_OPENDISTRO_PREFIX, certType);
    }
}
