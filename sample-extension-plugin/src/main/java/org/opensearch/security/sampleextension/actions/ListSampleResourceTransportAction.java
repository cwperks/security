/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions;

import java.util.List;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.client.Client;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.core.action.ActionListener;
import org.opensearch.index.query.MatchAllQueryBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.security.sampleextension.resource.SampleResourceSharingService;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

/**
 * Transport action for ListSampleResource.
 */
public class ListSampleResourceTransportAction extends HandledTransportAction<ListSampleResourceRequest, ListSampleResourceResponse> {
    private final TransportService transportService;
    private final Client nodeClient;

    @Inject
    public ListSampleResourceTransportAction(TransportService transportService, ActionFilters actionFilters, Client nodeClient) {
        super(ListSampleResourceAction.NAME, transportService, actionFilters, ListSampleResourceRequest::new);
        this.transportService = transportService;
        this.nodeClient = nodeClient;
    }

    @Override
    protected void doExecute(Task task, ListSampleResourceRequest request, ActionListener<ListSampleResourceResponse> listener) {
        try (ThreadContext.StoredContext ignore = transportService.getThreadPool().getThreadContext().stashContext()) {
            SearchRequest sr = new SearchRequest(".resource-sharing");
            SearchSourceBuilder matchAllQuery = new SearchSourceBuilder();
            matchAllQuery.query(new MatchAllQueryBuilder());
            sr.source(matchAllQuery);
            ActionListener<List<SampleResource>> sampleResourceListener = ActionListener.wrap(sampleResourcesList -> {
                System.out.println("sampleResourcesList: " + sampleResourcesList);
                listener.onResponse(new ListSampleResourceResponse(sampleResourcesList));
            }, listener::onFailure);
            SampleResourceSharingService.getInstance().getSharingService().listResources(sampleResourceListener);
            // listener.onResponse(new ListSampleResourceResponse(sampleResources));
            /* Index already exists, ignore and continue */
            // nodeClient.search(sr, searchListener);
        }
    }
}
