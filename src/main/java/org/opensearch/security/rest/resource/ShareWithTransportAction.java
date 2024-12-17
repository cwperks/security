/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.rest.resource;

import java.io.IOException;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.action.update.UpdateRequest;
import org.opensearch.action.update.UpdateResponse;
import org.opensearch.client.Client;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

/**
 * Transport action for UpdateResourceSharing.
 */
public class ShareWithTransportAction extends HandledTransportAction<ShareWithRequest, ShareWithResponse> {
    private static final Logger log = LogManager.getLogger(ShareWithTransportAction.class);

    public static final String RESOURCE_SHARING_INDEX = ".resource-sharing";

    private final TransportService transportService;
    private final Client nodeClient;

    @Inject
    public ShareWithTransportAction(TransportService transportService, ActionFilters actionFilters, Client nodeClient) {
        super(ShareWithAction.NAME, transportService, actionFilters, ShareWithRequest::new);
        this.transportService = transportService;
        this.nodeClient = nodeClient;
    }

    @Override
    protected void doExecute(Task task, ShareWithRequest request, ActionListener<ShareWithResponse> listener) {
        try (ThreadContext.StoredContext ignore = transportService.getThreadPool().getThreadContext().stashContext()) {
            SearchRequest searchRequest = new SearchRequest(RESOURCE_SHARING_INDEX);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery("resource_id", request.getResourceId()))
                .must(QueryBuilders.matchQuery("resource_index", request.getResourceIndex()));
            searchSourceBuilder.query(boolQuery);
            searchRequest.source(searchSourceBuilder);

            // Execute the search request
            nodeClient.search(searchRequest, new ActionListener<SearchResponse>() {
                @Override
                public void onResponse(SearchResponse searchResponse) {
                    if (Objects.requireNonNull(searchResponse.getHits().getTotalHits()).value == 1) {
                        // Record found, update it
                        SearchHit hit = searchResponse.getHits().getAt(0);
                        UpdateRequest updateRequest = new UpdateRequest(RESOURCE_SHARING_INDEX, hit.getId());
                        try {
                            XContentBuilder builder = XContentFactory.jsonBuilder();
                            builder.startObject();
                            {
                                builder.startObject("share_with");
                                {
                                    builder.field("users", request.getShareWith().getUsers());
                                    builder.field("backend_roles", request.getShareWith().getBackendRoles());
                                    builder.field("allowed_actions", request.getShareWith().getAllowedActions());
                                }
                                builder.endObject();
                            }
                            builder.endObject();
                            updateRequest.doc(builder);

                            nodeClient.update(updateRequest, new ActionListener<UpdateResponse>() {
                                @Override
                                public void onResponse(UpdateResponse updateResponse) {
                                    listener.onResponse(new ShareWithResponse("success"));
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    listener.onFailure(e);
                                }
                            });
                        } catch (IOException e) {
                            listener.onFailure(e);
                        }
                    } else {
                        // Record not found, create a new one
                        // createNewRecord(request, listener);
                        listener.onFailure(new IllegalStateException(".resource-sharing entry not found"));
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            });
        }
    }
}
