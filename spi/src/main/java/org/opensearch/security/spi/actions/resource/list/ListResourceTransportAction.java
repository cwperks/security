/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.spi.actions.resource.list;

import java.util.List;

import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.core.action.ActionListener;
import org.opensearch.security.spi.Resource;
import org.opensearch.security.spi.ResourceSharingService;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

/**
 * Transport action for ListResource.
 */
public class ListResourceTransportAction<T extends Resource> extends HandledTransportAction<ListResourceRequest, ListResourceResponse<T>> {
    private final ResourceSharingService<T> resourceSharingService;

    public ListResourceTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        String actionName,
        ResourceSharingService<T> resourceSharingService
    ) {
        super(actionName, transportService, actionFilters, ListResourceRequest::new);
        this.resourceSharingService = resourceSharingService;
    }

    @Override
    protected void doExecute(Task task, ListResourceRequest request, ActionListener<ListResourceResponse<T>> listener) {
        ActionListener<List<T>> sampleResourceListener = ActionListener.wrap(sampleResourcesList -> {
            System.out.println("sampleResourcesList: " + sampleResourcesList);
            listener.onResponse(new ListResourceResponse<T>(sampleResourcesList));
        }, listener::onFailure);
        resourceSharingService.listResources(sampleResourceListener);
    }
}
