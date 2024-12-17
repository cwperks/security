/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.spi.actions.resource.get;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.core.action.ActionListener;
import org.opensearch.security.spi.Resource;
import org.opensearch.security.spi.ResourceSharingService;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

/**
 * Transport action for GetResource.
 */
public class GetResourceTransportAction<T extends Resource> extends HandledTransportAction<GetResourceRequest, GetResourceResponse<T>> {
    private static final Logger log = LogManager.getLogger(GetResourceTransportAction.class);

    private final ResourceSharingService<T> resourceSharingService;

    public GetResourceTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        String actionName,
        ResourceSharingService<T> resourceSharingService
    ) {
        super(actionName, transportService, actionFilters, GetResourceRequest::new);
        this.resourceSharingService = resourceSharingService;
    }

    @Override
    protected void doExecute(Task task, GetResourceRequest request, ActionListener<GetResourceResponse<T>> actionListener) {
        getResource(request, actionListener);
    }

    private void getResource(GetResourceRequest request, ActionListener<GetResourceResponse<T>> listener) {
        ActionListener<T> getResourceListener = ActionListener.wrap(sampleResource -> {
            System.out.println("sampleResource: " + sampleResource);
            listener.onResponse(new GetResourceResponse<T>(sampleResource));
        }, listener::onFailure);
        resourceSharingService.getResource(request.getResourceId(), getResourceListener);
    }
}
