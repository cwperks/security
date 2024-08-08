/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions;

import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.client.Client;
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
    private final Client nodeClient;

    @Inject
    public CreateSampleResourceTransportAction(TransportService transportService, ActionFilters actionFilters, Client nodeClient) {
        super(CreateSampleResourceAction.NAME, transportService, actionFilters, CreateSampleResourceRequest::new);
        this.transportService = transportService;
        this.nodeClient = nodeClient;
    }

    @Override
    protected void doExecute(Task task, CreateSampleResourceRequest request, ActionListener<CreateSampleResourceResponse> listener) {
        CreateIndexRequest cir = new CreateIndexRequest(".resource-sharing");
        ActionListener<CreateIndexResponse> cirListener = ActionListener.wrap(response -> {
            if (response.isAcknowledged()) {
                System.out.println("Created .resource-sharing");
            } else {
                System.out.println("Created .resource-sharing call not acknowledged.");
            }
            listener.onResponse(new CreateSampleResourceResponse("Created .resource-sharing"));
        }, listener::onFailure);
        System.out.println("Calling create index for .resource-sharing");
        nodeClient.admin().indices().create(cir, cirListener);
    }
}
