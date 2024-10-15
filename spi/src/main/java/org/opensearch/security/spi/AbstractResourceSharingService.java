package org.opensearch.security.spi;

import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import org.opensearch.OpenSearchException;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.Client;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.core.action.ActionListener;
import org.opensearch.index.query.MatchAllQueryBuilder;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;

public abstract class AbstractResourceSharingService<T extends Resource> implements ResourceSharingService<T> {
    private final Client client;
    private final String resourceIndex;
    private final Class<T> resourceClass;

    public AbstractResourceSharingService(Client client, String resourceIndex, Class<T> resourceClass) {
        this.client = client;
        this.resourceIndex = resourceIndex;
        this.resourceClass = resourceClass;
    }

    private T newResource() {
        return AccessController.doPrivileged(new PrivilegedAction<T>() {
            @Override
            public T run() {
                try {
                    return resourceClass.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public void listResources(ActionListener<List<T>> listResourceListener) {
        T resource = newResource();
        try (ThreadContext.StoredContext ignore = client.threadPool().getThreadContext().stashContext()) {
            SearchRequest sr = new SearchRequest(resourceIndex);
            SearchSourceBuilder matchAllQuery = new SearchSourceBuilder();
            matchAllQuery.query(new MatchAllQueryBuilder());
            sr.source(matchAllQuery);
            /* Index already exists, ignore and continue */
            ActionListener<SearchResponse> searchListener = new ActionListener<SearchResponse>() {
                @Override
                public void onResponse(SearchResponse searchResponse) {
                    List<T> resources = new ArrayList<>();
                    for (SearchHit hit : searchResponse.getHits().getHits()) {
                        System.out.println("SearchHit: " + hit);
                        resources.add((T) resource.fromSource(hit.getSourceAsMap()));
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
    };
}
