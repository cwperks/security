/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.action.whoami;

import org.opensearch.action.ActionType;

public class WhoAmIAction extends ActionType<WhoAmIResponse> {

    public static final WhoAmIAction INSTANCE = new WhoAmIAction();
    public static final String NAME = "cluster:admin/opendistro_security/whoami";

    protected WhoAmIAction() {
        super(NAME, WhoAmIResponse::new);
    }
}
