/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.rest.resource;

import org.opensearch.action.ActionType;

/**
 * Action to update sharing configuration for a sample resource
 */
public class ShareWithAction extends ActionType<ShareWithResponse> {
    /**
     * Update sharing configuration for sample resource action instance
     */
    public static final ShareWithAction INSTANCE = new ShareWithAction();
    /**
     * Update sharing configuration for sample resource action name
     */
    public static final String NAME = "cluster:admin/opendistro_security/resource/share_with";

    private ShareWithAction() {
        super(NAME, ShareWithResponse::new);
    }
}
