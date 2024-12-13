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

import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.client.Client;
import org.opensearch.common.inject.Inject;
import org.opensearch.core.action.ActionListener;
import org.opensearch.security.sampleextension.resource.SampleResource;
import org.opensearch.security.sampleextension.resource.SampleResourceSharingServiceProvider;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

/**
 * Transport action for UpdateSampleResource.
 */
public class GetSampleResourceTransportAction extends HandledTransportAction<GetSampleResourceRequest, GetSampleResourceResponse> {
    private static final Logger log = LogManager.getLogger(GetSampleResourceTransportAction.class);

    private final Client nodeClient;
    private final SampleResourceSharingServiceProvider resourceSharingService;

    @Inject
    public GetSampleResourceTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        Client nodeClient,
        SampleResourceSharingServiceProvider resourceSharingService
    ) {
        super(GetSampleResourceAction.NAME, transportService, actionFilters, GetSampleResourceRequest::new);
        this.nodeClient = nodeClient;
        this.resourceSharingService = resourceSharingService;
    }

    @Override
    protected void doExecute(Task task, GetSampleResourceRequest request, ActionListener<GetSampleResourceResponse> actionListener) {
        getResource(request, actionListener);
    }

    private void getResource(GetSampleResourceRequest request, ActionListener<GetSampleResourceResponse> listener) {
        ActionListener<SampleResource> getResourceListener = ActionListener.wrap(sampleResource -> {
            System.out.println("sampleResource: " + sampleResource);
            listener.onResponse(new GetSampleResourceResponse(sampleResource));
        }, listener::onFailure);
        resourceSharingService.get().getResource(request.getResourceId(), getResourceListener);
    }
}
