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
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.action.support.master.AcknowledgedResponse;
import org.opensearch.client.Client;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.core.action.ActionListener;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

import static org.opensearch.security.sampleextension.SampleExtensionPlugin.RESOURCE_INDEX_NAME;

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
        try (ThreadContext.StoredContext ignore = transportService.getThreadPool().getThreadContext().stashContext()) {
            CreateIndexRequest cir = new CreateIndexRequest(RESOURCE_INDEX_NAME);
            ActionListener<CreateIndexResponse> cirListener = ActionListener.wrap(response -> {
                if (response.isAcknowledged()) {
                    System.out.println("Created " + RESOURCE_INDEX_NAME);
                } else {
                    System.out.println("Created " + RESOURCE_INDEX_NAME + " call not acknowledged.");
                }
                DeleteIndexRequest dir = new DeleteIndexRequest(RESOURCE_INDEX_NAME);
                ActionListener<AcknowledgedResponse> dirListener = ActionListener.wrap(deletedResponse -> {
                    listener.onResponse(new CreateSampleResourceResponse("Created and Deleted " + RESOURCE_INDEX_NAME));
                }, listener::onFailure);

                System.out.println("Calling delete index for " + RESOURCE_INDEX_NAME);
                nodeClient.admin().indices().delete(dir, dirListener);
                // listener.onResponse(new CreateSampleResourceResponse("Created " + RESOURCE_INDEX_NAME));
            }, listener::onFailure);
            System.out.println("Calling create index for " + RESOURCE_INDEX_NAME);
            nodeClient.admin().indices().create(cir, cirListener);
        }
    }
}
