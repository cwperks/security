/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions;

import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.common.inject.Inject;
import org.opensearch.core.action.ActionListener;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

/**
 * Transport action for GetExecutionContext.
 *
 * Returns the canonical class name of the plugin that is currently executing the transport action.
 */
public class CreateSampleResourceTransportAction extends HandledTransportAction<CreateSampleResourceRequest, CreateSampleResourceResponse> {
    private final TransportService transportService;

    @Inject
    public CreateSampleResourceTransportAction(TransportService transportService, ActionFilters actionFilters) {
        super(CreateSampleResourceAction.NAME, transportService, actionFilters, CreateSampleResourceRequest::new);
        this.transportService = transportService;
    }

    @Override
    protected void doExecute(Task task, CreateSampleResourceRequest request, ActionListener<CreateSampleResourceResponse> listener) {
        System.out.println("HERE");
        listener.onResponse(new CreateSampleResourceResponse("Hello, world!"));
    }
}
