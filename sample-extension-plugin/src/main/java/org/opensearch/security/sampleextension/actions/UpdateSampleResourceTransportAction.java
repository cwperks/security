/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.client.Client;
import org.opensearch.common.inject.Inject;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

import static org.opensearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Transport action for UpdateSampleResource.
 */
public class UpdateSampleResourceTransportAction extends HandledTransportAction<UpdateSampleResourceRequest, UpdateSampleResourceResponse> {
    private static final Logger log = LogManager.getLogger(UpdateSampleResourceTransportAction.class);

    private final Client nodeClient;
    private final String resourceIndex;

    @Inject
    public UpdateSampleResourceTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        Client nodeClient,
        String resourceIndex
    ) {
        super(UpdateSampleResourceAction.NAME, transportService, actionFilters, UpdateSampleResourceRequest::new);
        this.nodeClient = nodeClient;
        this.resourceIndex = resourceIndex;
    }

    @Override
    protected void doExecute(Task task, UpdateSampleResourceRequest request, ActionListener<UpdateSampleResourceResponse> actionListener) {
        indexResource(request, actionListener);
    }

    private void indexResource(UpdateSampleResourceRequest request, ActionListener<UpdateSampleResourceResponse> listener) {
        log.warn("resourceId: " + request.getResourceId());
        String name = request.getName();
        SampleResource updatedResource = new SampleResource();
        updatedResource.setName(name);
        try {
            IndexRequest ir = nodeClient.prepareIndex(resourceIndex)
                .setId(request.getResourceId())
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                .setSource(updatedResource.toXContent(jsonBuilder(), ToXContent.EMPTY_PARAMS))
                .request();

            log.warn("Index Request: " + ir.toString());

            ActionListener<IndexResponse> irListener = ActionListener.wrap(idxResponse -> {
                log.info("Updated resource: " + idxResponse.toString());
                listener.onResponse(new UpdateSampleResourceResponse(List.of(updatedResource)));
            }, listener::onFailure);
            nodeClient.index(ir, irListener);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}