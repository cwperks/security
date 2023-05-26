/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.dlic.rest.validation;

import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.settings.Settings;
import org.opensearch.rest.RestRequest;

public class TenantValidator extends AbstractConfigurationValidator {

    public TenantValidator(
        final RestRequest request,
        boolean isSuperAdmin,
        BytesReference ref,
        final Settings opensearchSettings,
        Object... param
    ) {
        super(request, ref, opensearchSettings, param);
        this.payloadMandatory = true;
        allowedKeys.put("description", DataType.STRING);
        if (isSuperAdmin) allowedKeys.put("reserved", DataType.BOOLEAN);
    }

}
