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
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.Client;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.core.action.ActionListener;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.security.spi.AbstractResource;
import org.opensearch.security.spi.AbstractResourceSharingService;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.user.User;

import static org.opensearch.security.resource.ResourceSharingListener.RESOURCE_SHARING_INDEX;

public class SecurityResourceSharingService<T extends AbstractResource> extends AbstractResourceSharingService<T> {
    public SecurityResourceSharingService(Client client, String resourceIndex, Class<T> resourceClass) {
        super(client, resourceIndex, resourceClass);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void listResources(ActionListener<List<T>> listResourceListener) {
        System.out.println("SecurityResourceSharingService.listResources");
        // TODO Flip this around. First query .resource-sharing and then use MGet to get all resources
        T resource = newResource();
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
                    for (SearchHit hit : searchResponse.getHits().getHits()) {
                        System.out.println("SearchHit: " + hit);
                        // resource.fromSource(hit.getId(), hit.getSourceAsMap());
                        // // TODO check what resources have been shared with the authenticatedUser
                        // System.out.println("authenticatedUser: " + authenticatedUser);
                        // System.out.println("resource.getResourceUser(): " + resource.getResourceUser());
                        // if (resource.getResourceUser() != null
                        // && authenticatedUser.getName().equals(resource.getResourceUser().getName())) {
                        // resources.add(resource);
                        // }
                    }
                    listResourceListener.onResponse(resources);
                }

                @Override
                public void onFailure(Exception e) {
                    throw new OpenSearchException("Caught exception while loading resources: " + e.getMessage());
                }
            };
            client.search(rsr, searchListener);

            // SearchRequest sr = new SearchRequest(resourceIndex);
            // SearchSourceBuilder matchAllQuery = new SearchSourceBuilder();
            // matchAllQuery.query(new MatchAllQueryBuilder());
            // sr.source(matchAllQuery);
            // /* Index already exists, ignore and continue */
            // ActionListener<SearchResponse> searchListener = new ActionListener<SearchResponse>() {
            // @Override
            // public void onResponse(SearchResponse searchResponse) {
            // List<T> resources = new ArrayList<>();
            // for (SearchHit hit : searchResponse.getHits().getHits()) {
            // System.out.println("SearchHit: " + hit);
            // resource.fromSource(hit.getId(), hit.getSourceAsMap());
            // // TODO check what resources have been shared with the authenticatedUser
            // System.out.println("authenticatedUser: " + authenticatedUser);
            // System.out.println("resource.getResourceUser(): " + resource.getResourceUser());
            // if (resource.getResourceUser() != null
            // && authenticatedUser.getName().equals(resource.getResourceUser().getName())) {
            // resources.add(resource);
            // }
            // }
            // listResourceListener.onResponse(resources);
            // }
            //
            // @Override
            // public void onFailure(Exception e) {
            // throw new OpenSearchException("Caught exception while loading resources: " + e.getMessage());
            // }
            // };
            // client.search(sr, searchListener);
        }
    }
}
