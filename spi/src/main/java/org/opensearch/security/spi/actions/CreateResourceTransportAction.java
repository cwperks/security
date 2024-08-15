/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.spi.actions;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.client.Client;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.security.spi.Resource;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

import static org.opensearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Transport action for CreateSampleResource.
 */
public class CreateResourceTransportAction<T extends Resource> extends HandledTransportAction<
    CreateResourceRequest<T>,
    CreateResourceResponse> {
    private static final Logger log = LogManager.getLogger(CreateResourceTransportAction.class);

    private final TransportService transportService;
    private final Client nodeClient;
    private final String resourceIndex;

    public CreateResourceTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        Client nodeClient,
        String actionName,
        String resourceIndex,
        Writeable.Reader<T> resourceReader
    ) {
        super(actionName, transportService, actionFilters, (in) -> new CreateResourceRequest<T>(in, resourceReader));
        this.transportService = transportService;
        this.nodeClient = nodeClient;
        this.resourceIndex = resourceIndex;
    }

    @Override
    protected void doExecute(Task task, CreateResourceRequest<T> request, ActionListener<CreateResourceResponse> listener) {
        try (ThreadContext.StoredContext ignore = transportService.getThreadPool().getThreadContext().stashContext()) {
            CreateIndexRequest cir = new CreateIndexRequest(resourceIndex);
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

    private void createResource(CreateResourceRequest<T> request, ActionListener<CreateResourceResponse> listener) {
        log.warn("Sample name: " + request.getResource());
        Resource sample = request.getResource();
        try {
            IndexRequest ir = nodeClient.prepareIndex(resourceIndex)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                .setSource(sample.toXContent(jsonBuilder(), ToXContent.EMPTY_PARAMS))
                .request();

            log.warn("Index Request: " + ir.toString());

            ActionListener<IndexResponse> irListener = ActionListener.wrap(idxResponse -> {
                listener.onResponse(new CreateResourceResponse("Created resource: " + idxResponse.toString()));
            }, listener::onFailure);
            nodeClient.index(ir, irListener);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
