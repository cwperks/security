/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions;

import org.opensearch.action.ActionType;

/**
 * Action to create a sample resource
 */
public class CreateSampleResourceAction extends ActionType<CreateSampleResourceResponse> {
    /**
     * Create sample resource action instance
     */
    public static final CreateSampleResourceAction INSTANCE = new CreateSampleResourceAction();
    /**
     * Create sample resource action name
     */
    public static final String NAME = "cluster:admin/sampleresource/create";

    private CreateSampleResourceAction() {
        super(NAME, CreateSampleResourceResponse::new);
    }
}
