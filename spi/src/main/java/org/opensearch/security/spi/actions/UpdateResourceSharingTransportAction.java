/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.spi.actions;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.action.update.UpdateRequest;
import org.opensearch.action.update.UpdateResponse;
import org.opensearch.client.Client;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
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

    public static final String RESOURCE_SHARING_INDEX = ".resource-sharing";

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
            // TODO write a request to find the record in .resource-sharing matching this resource_id and resource_index
            SearchRequest searchRequest = new SearchRequest(RESOURCE_SHARING_INDEX);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("resource_id", request.getResourceId()))
                .must(QueryBuilders.termQuery("resource_index", resourceIndex));
            searchSourceBuilder.query(boolQuery);
            searchRequest.source(searchSourceBuilder);

            System.out.println("SearchRequest: " + searchRequest.toString());

            // Execute the search request
            nodeClient.search(searchRequest, new ActionListener<SearchResponse>() {
                @Override
                public void onResponse(SearchResponse searchResponse) {
                    System.out.println("searchResponse.getHits(): " + Arrays.toString(searchResponse.getHits().getHits()));
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
                                }
                                builder.endObject();
                            }
                            builder.endObject();
                            updateRequest.doc(builder);

                            nodeClient.update(updateRequest, new ActionListener<UpdateResponse>() {
                                @Override
                                public void onResponse(UpdateResponse updateResponse) {
                                    listener.onResponse(new UpdateResourceSharingResponse("success"));
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
