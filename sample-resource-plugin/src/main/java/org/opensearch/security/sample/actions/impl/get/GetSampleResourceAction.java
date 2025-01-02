/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sample.actions.impl.get;

import org.opensearch.action.ActionType;
import org.opensearch.plugins.resource.action.generic.get.GetResourceResponse;
import org.opensearch.security.sample.resource.SampleResource;

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
