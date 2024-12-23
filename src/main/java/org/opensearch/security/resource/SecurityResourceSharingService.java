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

import java.io.IOException;

import org.opensearch.OpenSearchException;
import org.opensearch.ResourceNotFoundException;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.Client;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.common.xcontent.LoggingDeprecationHandler;
import org.opensearch.common.xcontent.XContentHelper;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.security.rest.resource.ShareWith;
import org.opensearch.security.spi.Resource;
import org.opensearch.security.spi.ResourceParser;
import org.opensearch.security.spi.ResourceSharingService;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.support.WildcardMatcher;
import org.opensearch.security.user.User;

import static org.opensearch.security.resource.ResourceSharingListener.RESOURCE_SHARING_INDEX;

public class SecurityResourceSharingService<T extends Resource> implements ResourceSharingService<T> {
    private final Client client;
    private final String resourceIndex;
    private final ResourceParser<T> resourceParser;
    private final NamedXContentRegistry xContentRegistry;

    public SecurityResourceSharingService(
        Client client,
        String resourceIndex,
        ResourceParser<T> resourceParser,
        NamedXContentRegistry xContentRegistry
    ) {
        this.client = client;
        this.resourceIndex = resourceIndex;
        this.resourceParser = resourceParser;
        this.xContentRegistry = xContentRegistry;
    }

    private boolean hasPermissionsFor(User authenticatedUser, ResourceSharingEntry sharedWith) {
        // 1. The resource_user is the currently authenticated user
        // 2. The resource has been shared with the authenticated user
        // 3. The resource has been shared with a backend role that the authenticated user has
        if (authenticatedUser.getName().equals(sharedWith.getResourceUser().getName())) {
            return true;
        }

        for (ShareWith shareWith : sharedWith.getShareWith().values()) {
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

    @Override
    public void isSharedWithCurrentUser(String resourceId, ActionListener<Boolean> resourceSharingListener) {
        User authenticatedUser = (User) client.threadPool()
            .getThreadContext()
            .getPersistent(ConfigConstants.OPENDISTRO_SECURITY_AUTHENTICATED_USER);
        try (ThreadContext.StoredContext ignore = client.threadPool().getThreadContext().stashContext()) {
            SearchRequest searchRequest = new SearchRequest(RESOURCE_SHARING_INDEX);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery("resource_index", resourceIndex))
                .must(QueryBuilders.matchQuery("resource_id", resourceId));

            searchSourceBuilder.query(boolQuery);
            searchSourceBuilder.size(1); // Limit to 1 result
            searchRequest.source(searchSourceBuilder);

            ActionListener<SearchResponse> searchListener = new ActionListener<>() {
                @Override
                public void onResponse(SearchResponse searchResponse) {
                    SearchHit[] hits = searchResponse.getHits().getHits();
                    if (hits.length > 0) {
                        SearchHit hit = hits[0];
                        ResourceSharingEntry sharedWith = ResourceSharingEntry.fromSource(hit.getSourceAsMap());
                        if (hasPermissionsFor(authenticatedUser, sharedWith)) {
                            resourceSharingListener.onResponse(Boolean.TRUE);
                        } else {
                            resourceSharingListener.onResponse(Boolean.FALSE);
                        }
                    } else {
                        resourceSharingListener.onFailure(new ResourceNotFoundException("Resource not found"));
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

    private void finishGetResourceIfUserIsAllowed(String resourceId, ActionListener<T> getResourceListener) {
        try (ThreadContext.StoredContext ignore = client.threadPool().getThreadContext().stashContext()) {
            GetRequest gr = new GetRequest(resourceIndex);
            gr.id(resourceId);
            ActionListener<GetResponse> getListener = new ActionListener<>() {
                @Override
                public void onResponse(GetResponse getResponse) {
                    try {
                        XContentParser parser = XContentHelper.createParser(
                            xContentRegistry,
                            LoggingDeprecationHandler.INSTANCE,
                            getResponse.getSourceAsBytesRef(),
                            XContentType.JSON
                        );
                        T resource = resourceParser.parse(parser, getResponse.getId());
                        getResourceListener.onResponse(resource);
                    } catch (IOException e) {
                        throw new OpenSearchException("Caught exception while loading resources: " + e.getMessage());
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    getResourceListener.onFailure(new OpenSearchException("Caught exception while loading resources: " + e.getMessage()));
                }
            };
            client.get(gr, getListener);
        }
    }
}
