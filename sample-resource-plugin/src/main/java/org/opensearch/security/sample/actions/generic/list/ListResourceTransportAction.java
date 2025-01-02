/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sample.actions.generic.list;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.opensearch.OpenSearchException;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.client.Client;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.common.xcontent.LoggingDeprecationHandler;
import org.opensearch.common.xcontent.XContentHelper;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.index.query.MatchAllQueryBuilder;
import org.opensearch.plugins.resource.Resource;
import org.opensearch.plugins.resource.ResourceParser;
import org.opensearch.plugins.resource.ResourceSharingService;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

/**
 * Transport action for ListResource.
 */
public class ListResourceTransportAction<T extends Resource> extends HandledTransportAction<ListResourceRequest, ListResourceResponse<T>> {
    private final ResourceSharingService resourceSharingService;

    private final ResourceParser<T> resourceParser;

    private final Client client;

    private final String resourceIndex;

    private final NamedXContentRegistry xContentRegistry;

    public ListResourceTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        String actionName,
        String resourceIndex,
        ResourceSharingService resourceSharingService,
        ResourceParser<T> resourceParser,
        Client client,
        NamedXContentRegistry xContentRegistry
    ) {
        super(actionName, transportService, actionFilters, ListResourceRequest::new);
        this.client = client;
        this.resourceSharingService = resourceSharingService;
        this.resourceIndex = resourceIndex;
        this.xContentRegistry = xContentRegistry;
        Objects.requireNonNull(resourceParser);
        this.resourceParser = resourceParser;
    }

    @Override
    protected void doExecute(Task task, ListResourceRequest request, ActionListener<ListResourceResponse<T>> listener) {
        ActionListener<List<T>> listResourceListener = ActionListener.wrap(resourcesList -> {
            System.out.println("resourcesList: " + resourcesList);
            listener.onResponse(new ListResourceResponse<>(resourcesList));
        }, listener::onFailure);
        try (ThreadContext.StoredContext ignore = client.threadPool().getThreadContext().stashContext()) {
            SearchRequest sr = new SearchRequest(resourceIndex);
            SearchSourceBuilder matchAllQuery = new SearchSourceBuilder();
            matchAllQuery.query(new MatchAllQueryBuilder());
            sr.source(matchAllQuery);
            ActionListener<SearchResponse> searchListener = new ActionListener<>() {
                @Override
                public void onResponse(SearchResponse searchResponse) {
                    List<T> resources = new ArrayList<>();

                    SearchHit[] hits = searchResponse.getHits().getHits();

                    if (hits.length == 0) {
                        listResourceListener.onResponse(resources);
                        return;
                    }

                    AtomicInteger remainingChecks = new AtomicInteger(hits.length);

                    for (SearchHit hit : hits) {
                        try {
                            XContentParser parser = XContentHelper.createParser(
                                xContentRegistry,
                                LoggingDeprecationHandler.INSTANCE,
                                hit.getSourceRef(),
                                XContentType.JSON
                            );
                            T resource = resourceParser.parse(parser, hit.getId());

                            ActionListener<Boolean> shareListener = new ActionListener<>() {
                                @Override
                                public void onResponse(Boolean isShared) {
                                    if (isShared) {
                                        synchronized (resources) {
                                            resources.add(resource);
                                        }
                                    }
                                    if (remainingChecks.decrementAndGet() == 0) {
                                        listResourceListener.onResponse(resources);
                                    }
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    listResourceListener.onFailure(
                                        new OpenSearchException("Failed to check sharing status: " + e.getMessage(), e)
                                    );
                                }
                            };

                            resourceSharingService.isSharedWithCurrentRequester(hit.getId(), shareListener);

                        } catch (IOException e) {
                            listResourceListener.onFailure(
                                new OpenSearchException("Caught exception while loading resources: " + e.getMessage(), e)
                            );
                            return;
                        }
                    }
                    listResourceListener.onResponse(resources);
                }

                @Override
                public void onFailure(Exception e) {
                    throw new OpenSearchException("Caught exception while loading resources: " + e.getMessage());
                }
            };
            client.search(sr, searchListener);
        }
    }
}
