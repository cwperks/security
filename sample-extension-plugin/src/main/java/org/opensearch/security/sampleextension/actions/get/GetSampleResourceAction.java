/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions.get;

import org.opensearch.action.ActionType;
import org.opensearch.security.sampleextension.resource.SampleResource;
import org.opensearch.security.spi.actions.resource.get.GetResourceResponse;

/**
 * Action to get a sample resource
 */
public class GetSampleResourceAction extends ActionType<GetResourceResponse<SampleResource>> {
    /**
     * Get sample resource action instance
     */
    public static final GetSampleResourceAction INSTANCE = new GetSampleResourceAction();
    /**
     * Get sample resource action name
     */
    public static final String NAME = "cluster:admin/sampleresource/get";

    private GetSampleResourceAction() {
        super(NAME, in -> new GetResourceResponse<>(in, SampleResource::from));
    }
}
