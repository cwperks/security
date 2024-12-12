/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions.get;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.client.Client;
import org.opensearch.common.inject.Inject;
import org.opensearch.core.action.ActionListener;
import org.opensearch.security.sampleextension.actions.SampleResource;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

import static org.opensearch.security.sampleextension.SampleExtensionPlugin.RESOURCE_INDEX_NAME;

/**
 * Transport action for UpdateSampleResource.
 */
public class GetSampleResourceTransportAction extends HandledTransportAction<GetSampleResourceRequest, GetSampleResourceResponse> {
    private static final Logger log = LogManager.getLogger(GetSampleResourceTransportAction.class);

    private final Client nodeClient;

    @Inject
    public GetSampleResourceTransportAction(TransportService transportService, ActionFilters actionFilters, Client nodeClient) {
        super(GetSampleResourceAction.NAME, transportService, actionFilters, GetSampleResourceRequest::new);
        this.nodeClient = nodeClient;
    }

    @Override
    protected void doExecute(Task task, GetSampleResourceRequest request, ActionListener<GetSampleResourceResponse> actionListener) {
        getResource(request, actionListener);
    }

    private void getResource(GetSampleResourceRequest request, ActionListener<GetSampleResourceResponse> listener) {
        log.warn("resourceId: " + request.getResourceId());
        GetRequest gr = nodeClient.prepareGet().setIndex(RESOURCE_INDEX_NAME).setId(request.getResourceId()).request();

        log.warn("GET Request: " + gr.toString());

        ActionListener<GetResponse> grListener = ActionListener.wrap(getResponse -> {
            log.info("Updated resource: " + getResponse.toString());
            getResponse.getSource();
            SampleResource resource = new SampleResource();
            resource.setName(getResponse.getSource().get("name").toString());
            listener.onResponse(new GetSampleResourceResponse(resource));
        }, listener::onFailure);
        nodeClient.get(gr, grListener);
    }
}
