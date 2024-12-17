/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions.sharing;

import org.opensearch.action.ActionType;
import org.opensearch.security.spi.actions.sharing.update.UpdateResourceSharingResponse;

/**
 * Action to update sharing configuration for a sample resource
 */
public class UpdateSampleResourceSharingAction extends ActionType<UpdateResourceSharingResponse> {
    /**
     * Update sharing configuratino for sample resource action instance
     */
    public static final UpdateSampleResourceSharingAction INSTANCE = new UpdateSampleResourceSharingAction();
    /**
     * Update sharing configuration for sample resource action name
     */
    public static final String NAME = "cluster:admin/sampleresource/updatesharing";

    private UpdateSampleResourceSharingAction() {
        super(NAME, UpdateResourceSharingResponse::new);
    }
}
