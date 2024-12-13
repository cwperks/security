/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions.list;

import java.util.List;

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
 * Transport action for ListSampleResource.
 */
public class ListSampleResourceTransportAction extends HandledTransportAction<ListSampleResourceRequest, ListSampleResourceResponse> {
    private final TransportService transportService;
    private final Client nodeClient;
    private final SampleResourceSharingServiceProvider resourceSharingService;

    // TODO How can this inject work if either a DefaultResourceSharingService or SecurityResourceSharingService is binded?
    @Inject
    public ListSampleResourceTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        Client nodeClient,
        SampleResourceSharingServiceProvider resourceSharingService
    ) {
        super(ListSampleResourceAction.NAME, transportService, actionFilters, ListSampleResourceRequest::new);
        this.transportService = transportService;
        this.nodeClient = nodeClient;
        this.resourceSharingService = resourceSharingService;
    }

    @Override
    protected void doExecute(Task task, ListSampleResourceRequest request, ActionListener<ListSampleResourceResponse> listener) {
        ActionListener<List<SampleResource>> sampleResourceListener = ActionListener.wrap(sampleResourcesList -> {
            System.out.println("sampleResourcesList: " + sampleResourcesList);
            listener.onResponse(new ListSampleResourceResponse(sampleResourcesList));
        }, listener::onFailure);
        resourceSharingService.get().listResources(sampleResourceListener);
    }
}
