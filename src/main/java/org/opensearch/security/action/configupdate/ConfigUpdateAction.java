/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.action.configupdate;

import org.opensearch.action.ActionType;

public class ConfigUpdateAction extends ActionType<ConfigUpdateResponse> {

    public static final ConfigUpdateAction INSTANCE = new ConfigUpdateAction();
    public static final String NAME = "cluster:admin/opendistro_security/config/update";

    protected ConfigUpdateAction() {
        super(NAME, ConfigUpdateResponse::new);
    }
}
