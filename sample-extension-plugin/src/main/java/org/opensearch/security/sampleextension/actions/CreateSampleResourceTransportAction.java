/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions;

import java.io.IOException;

import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.client.Client;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.core.action.ActionListener;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

import static org.opensearch.common.xcontent.XContentFactory.jsonBuilder;
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
            ActionListener<CreateIndexResponse> cirListener = ActionListener.wrap(
                response -> { createResource(request, listener); },
                (failResponse) -> {
                    /* Index already exists, ignore and continue */
                    createResource(request, listener);
                }
            );
            nodeClient.admin().indices().create(cir, cirListener);
        }
    }

    private void createResource(CreateSampleResourceRequest request, ActionListener<CreateSampleResourceResponse> listener) {
        try {
            IndexRequest ir = nodeClient.prepareIndex(RESOURCE_INDEX_NAME)
                .setSource(jsonBuilder().startObject().field("name", request.getName()).endObject())
                .request();
            ActionListener<IndexResponse> irListener = ActionListener.wrap(idxResponse -> {
                listener.onResponse(new CreateSampleResourceResponse("Created resource: " + idxResponse.toString()));
            }, listener::onFailure);
            nodeClient.index(ir, irListener);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
