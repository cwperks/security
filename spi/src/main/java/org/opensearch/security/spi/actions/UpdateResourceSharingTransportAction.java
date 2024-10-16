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
import org.opensearch.security.spi.AbstractResource;
import org.opensearch.security.spi.ShareWith;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

import static org.opensearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Transport action for CreateSampleResource.
 */
public class UpdateResourceSharingTransportAction<T extends AbstractResource> extends HandledTransportAction<
    UpdateResourceSharingRequest<T>,
    UpdateResourceSharingResponse> {
    private static final Logger log = LogManager.getLogger(UpdateResourceSharingTransportAction.class);

    private final TransportService transportService;
    private final Client nodeClient;
    private final String resourceIndex;

    public UpdateResourceSharingTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        Client nodeClient,
        String actionName,
        String resourceIndex,
        Writeable.Reader<ShareWith> shareWithReader
    ) {
        super(actionName, transportService, actionFilters, (in) -> new UpdateResourceSharingRequest<T>(in, shareWithReader));
        this.transportService = transportService;
        this.nodeClient = nodeClient;
        this.resourceIndex = resourceIndex;
    }

    @Override
    protected void doExecute(Task task, UpdateResourceSharingRequest<T> request, ActionListener<UpdateResourceSharingResponse> listener) {
        try (ThreadContext.StoredContext ignore = transportService.getThreadPool().getThreadContext().stashContext()) {
            System.out.println("resourceId: " + request.getResourceId());
            System.out.println("shareWith: " + request.getShareWith());
            IndexRequest ir = new IndexRequest(resourceIndex);
            ActionListener<IndexResponse> indexListener = ActionListener.wrap(response -> {
                listener.onResponse(new UpdateResourceSharingResponse("success"));
            }, listener::onFailure);
            nodeClient.index(ir, indexListener);
        }
    }

    private void indexResource(CreateResourceRequest<T> request, ActionListener<CreateResourceResponse> listener) {
        log.warn("Sample name: " + request.getResource());
        AbstractResource sample = request.getResource();
        try {
            IndexRequest ir = nodeClient.prepareIndex(resourceIndex)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                .setSource(sample.toXContent(jsonBuilder(), ToXContent.EMPTY_PARAMS))
                .request();

            log.warn("Index Request: " + ir.toString());

            // ActionListener<IndexResponse> resourceSharingListener = ActionListener.wrap(resourceSharingResponse -> {
            // listener.onResponse(new CreateResourceResponse("Created resource: " + resourceSharingResponse.toString()));
            // }, listener::onFailure);

            ActionListener<IndexResponse> irListener = ActionListener.wrap(idxResponse -> {
                log.info("Created resource: " + idxResponse.toString());
                // ResourceSharingUtils.getInstance()
                // .indexResourceSharing(idxResponse.getId(), sample, ShareWith.PUBLIC, resourceSharingListener);
                listener.onResponse(new CreateResourceResponse("Created resource: " + idxResponse.toString()));
            }, listener::onFailure);
            nodeClient.index(ir, irListener);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
