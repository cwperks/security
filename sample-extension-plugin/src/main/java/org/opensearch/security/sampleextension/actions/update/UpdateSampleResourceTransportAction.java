/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions.update;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.action.update.UpdateRequest;
import org.opensearch.action.update.UpdateResponse;
import org.opensearch.client.Client;
import org.opensearch.common.inject.Inject;
import org.opensearch.core.action.ActionListener;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

import static org.opensearch.security.sampleextension.SampleExtensionPlugin.RESOURCE_INDEX_NAME;

/**
 * Transport action for UpdateSampleResource.
 */
public class UpdateSampleResourceTransportAction extends HandledTransportAction<UpdateSampleResourceRequest, UpdateSampleResourceResponse> {
    private static final Logger log = LogManager.getLogger(UpdateSampleResourceTransportAction.class);

    private final Client nodeClient;

    @Inject
    public UpdateSampleResourceTransportAction(TransportService transportService, ActionFilters actionFilters, Client nodeClient) {
        super(UpdateSampleResourceAction.NAME, transportService, actionFilters, UpdateSampleResourceRequest::new);
        this.nodeClient = nodeClient;
    }

    @Override
    protected void doExecute(Task task, UpdateSampleResourceRequest request, ActionListener<UpdateSampleResourceResponse> actionListener) {
        updateResource(request, actionListener);
    }

    private void updateResource(UpdateSampleResourceRequest request, ActionListener<UpdateSampleResourceResponse> listener) {
        // TODO Prevent resource_user or share_with from being updated by plugin...only security plugin should update
        log.warn("resourceId: " + request.getResourceId());
        UpdateRequest ur = nodeClient.prepareUpdate()
            .setIndex(RESOURCE_INDEX_NAME)
            .setId(request.getResourceId())
            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
            .setDoc(Map.of("name", request.getName()))
            .request();

        log.warn("Update Request: " + ur.toString());

        ActionListener<UpdateResponse> urListener = ActionListener.wrap(idxResponse -> {
            log.info("Updated resource: " + idxResponse.toString());
            listener.onResponse(new UpdateSampleResourceResponse(idxResponse.getId()));
        }, listener::onFailure);
        nodeClient.update(ur, urListener);
    }
}
