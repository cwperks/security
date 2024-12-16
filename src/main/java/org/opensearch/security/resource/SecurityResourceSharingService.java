/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.security.resource;

import java.util.ArrayList;
import java.util.List;

import org.opensearch.OpenSearchException;
import org.opensearch.ResourceNotFoundException;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.get.MultiGetItemResponse;
import org.opensearch.action.get.MultiGetRequest;
import org.opensearch.action.get.MultiGetResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.Client;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.core.action.ActionListener;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.security.spi.Resource;
import org.opensearch.security.spi.ResourceFactory;
import org.opensearch.security.spi.ResourceSharingService;
import org.opensearch.security.spi.ShareWith;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.support.WildcardMatcher;
import org.opensearch.security.user.User;

import static org.opensearch.security.resource.ResourceSharingListener.RESOURCE_SHARING_INDEX;

public class SecurityResourceSharingService<T extends Resource> implements ResourceSharingService<T> {
    private final Client client;
    private final String resourceIndex;
    private final ResourceFactory<T> resourceFactory;

    public SecurityResourceSharingService(Client client, String resourceIndex, ResourceFactory<T> resourceFactory) {
        this.client = client;
        this.resourceIndex = resourceIndex;
        this.resourceFactory = resourceFactory;
    }

    private boolean hasPermissionsFor(User authenticatedUser, ResourceSharingEntry sharedWith) {
        // 1. The resource_user is the currently authenticated user
        // 2. The resource has been shared with the authenticated user
        // 3. The resource has been shared with a backend role that the authenticated user has
        if (authenticatedUser.getName().equals(sharedWith.getResourceUser().getName())) {
            return true;
        }

        for (ShareWith shareWith : sharedWith.getShareWith()) {
            WildcardMatcher userMatcher = WildcardMatcher.from(shareWith.getUsers());
            if (userMatcher.test(authenticatedUser.getName())) {
                return true;
            }
            WildcardMatcher backendRoleMatcher = WildcardMatcher.from(shareWith.getBackendRoles());
            if (authenticatedUser.getRoles().stream().anyMatch(backendRoleMatcher::test)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void listResources(ActionListener<List<T>> listResourceListener) {
        User authenticatedUser = client.threadPool().getThreadContext().getTransient(ConfigConstants.OPENDISTRO_SECURITY_USER);
        try (ThreadContext.StoredContext ignore = client.threadPool().getThreadContext().stashContext()) {
            SearchRequest rsr = new SearchRequest(RESOURCE_SHARING_INDEX);
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

            // 1. The resource_user is the currently authenticated user
            boolQuery.should(QueryBuilders.termQuery("resource_user.name", authenticatedUser.getName()));

            // 2. The resource has been shared with the authenticated user
            boolQuery.should(QueryBuilders.termQuery("share_with.users", authenticatedUser.getName()));

            // 3. The resource has been shared with a backend role that the authenticated user has
            if (!authenticatedUser.getRoles().isEmpty()) {
                BoolQueryBuilder roleQuery = QueryBuilders.boolQuery();
                for (String role : authenticatedUser.getRoles()) {
                    roleQuery.should(QueryBuilders.termQuery("share_with.backend_roles", role));
                }
                boolQuery.should(roleQuery);
            }

            // Set minimum should match to 1 to ensure at least one of the conditions is met
            boolQuery.minimumShouldMatch(1);

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQuery);
            rsr.source(searchSourceBuilder);

            ActionListener<SearchResponse> searchListener = new ActionListener<SearchResponse>() {
                @Override
                public void onResponse(SearchResponse searchResponse) {
                    List<T> resources = new ArrayList<>();
                    List<String> resourceIds = new ArrayList<>();
                    for (SearchHit hit : searchResponse.getHits().getHits()) {
                        resourceIds.add((String) hit.getSourceAsMap().get("resource_id"));
                    }
                    if (resourceIds.isEmpty()) {
                        listResourceListener.onResponse(resources);
                    }

                    final MultiGetRequest mget = new MultiGetRequest();

                    for (String resourceId : resourceIds) {
                        mget.add(resourceIndex, resourceId);
                    }

                    mget.refresh(true);
                    mget.realtime(true);

                    client.multiGet(mget, new ActionListener<MultiGetResponse>() {
                        @Override
                        public void onResponse(MultiGetResponse response) {
                            MultiGetItemResponse[] responses = response.getResponses();
                            for (MultiGetItemResponse singleResponse : responses) {
                                if (singleResponse != null && !singleResponse.isFailed()) {
                                    GetResponse singleGetResponse = singleResponse.getResponse();
                                    if (singleGetResponse.isExists() && !singleGetResponse.isSourceEmpty()) {
                                        // TODO Is there a better way to create this instance of a generic w/o using reflection
                                        T resource = resourceFactory.createResource();
                                        resource.fromSource(singleGetResponse.getId(), singleGetResponse.getSourceAsMap());
                                        resources.add(resource);
                                    } else {
                                        // does not exist or empty source
                                        continue;
                                    }
                                } else {
                                    // failure
                                    continue;
                                }
                            }
                            listResourceListener.onResponse(resources);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            listResourceListener.onFailure(e);
                        }
                    });

                }

                @Override
                public void onFailure(Exception e) {
                    throw new OpenSearchException("Caught exception while loading resources: " + e.getMessage());
                }
            };
            client.search(rsr, searchListener);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void getResource(String resourceId, ActionListener<T> getResourceListener) {
        User authenticatedUser = client.threadPool().getThreadContext().getTransient(ConfigConstants.OPENDISTRO_SECURITY_USER);
        try (ThreadContext.StoredContext ignore = client.threadPool().getThreadContext().stashContext()) {
            GetRequest gr = new GetRequest(resourceIndex);
            gr.id(resourceId);
            /* Index already exists, ignore and continue */
            ActionListener<GetResponse> getListener = new ActionListener<GetResponse>() {
                @Override
                public void onResponse(GetResponse getResponse) {
                    T resource = resourceFactory.createResource();
                    resource.fromSource(getResponse.getId(), getResponse.getSourceAsMap());
                    System.out.println("finishGetResourceIfUserIsAllowed");
                    finishGetResourceIfUserIsAllowed(resource, authenticatedUser, getResourceListener);
                }

                @Override
                public void onFailure(Exception e) {
                    getResourceListener.onFailure(new OpenSearchException("Caught exception while loading resources: " + e.getMessage()));
                }
            };
            client.get(gr, getListener);
        }
    }

    private void finishGetResourceIfUserIsAllowed(T resource, User authenticatedUser, ActionListener<T> getResourceListener) {
        try (ThreadContext.StoredContext ignore = client.threadPool().getThreadContext().stashContext()) {
            SearchRequest searchRequest = new SearchRequest(RESOURCE_SHARING_INDEX);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            System.out.println("resourceIndex: " + resourceIndex);
            System.out.println("resource.getResourceId(): " + resource.getResourceId());
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery("resource_index", resourceIndex))
                .must(QueryBuilders.matchQuery("resource_id", resource.getResourceId()));

            searchSourceBuilder.query(boolQuery);
            searchSourceBuilder.size(1); // Limit to 1 result
            searchRequest.source(searchSourceBuilder);

            ActionListener<SearchResponse> searchListener = new ActionListener<SearchResponse>() {
                @Override
                public void onResponse(SearchResponse searchResponse) {
                    SearchHit[] hits = searchResponse.getHits().getHits();
                    if (hits.length > 0) {
                        SearchHit hit = hits[0];
                        ResourceSharingEntry sharedWith = ResourceSharingEntry.fromSource(hit.getSourceAsMap());
                        if (hasPermissionsFor(authenticatedUser, sharedWith)) {
                            getResourceListener.onResponse(resource);
                        } else {
                            getResourceListener.onFailure(new OpenSearchException("User is not authorized to access this resource"));
                        }
                    } else {
                        getResourceListener.onFailure(new ResourceNotFoundException("Resource not found"));
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    throw new OpenSearchException("Caught exception while loading resources: " + e.getMessage());
                }
            };

            client.search(searchRequest, searchListener);
        }
    }
}
