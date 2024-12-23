package org.opensearch.security.spi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opensearch.OpenSearchException;
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
import org.opensearch.index.query.MatchAllQueryBuilder;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;

public class DefaultResourceSharingService<T extends Resource> implements ResourceSharingService<T> {
    private final Client client;
    private final String resourceIndex;
    private final ResourceParser<T> resourceParser;
    private final NamedXContentRegistry xContentRegistry;

    public DefaultResourceSharingService(
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

    @SuppressWarnings("unchecked")
    @Override
    public void listResources(ActionListener<List<T>> listResourceListener) {
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
                        try {
                            XContentParser parser = XContentHelper.createParser(
                                xContentRegistry,
                                LoggingDeprecationHandler.INSTANCE,
                                hit.getSourceRef(),
                                XContentType.JSON
                            );
                            T resource = resourceParser.parse(parser, hit.getId());
                            resources.add(resource);
                        } catch (IOException e) {
                            throw new OpenSearchException("Caught exception while loading resources: " + e.getMessage());
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

    @SuppressWarnings("unchecked")
    @Override
    public void getResource(String resourceId, ActionListener<T> getResourceListener) {
        try (ThreadContext.StoredContext ignore = client.threadPool().getThreadContext().stashContext()) {
            GetRequest gr = new GetRequest(resourceIndex);
            gr.id(resourceId);
            /* Index already exists, ignore and continue */
            ActionListener<GetResponse> getListener = new ActionListener<GetResponse>() {
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
                    throw new OpenSearchException("Caught exception while loading resources: " + e.getMessage());
                }
            };
            client.get(gr, getListener);
        }
    }
}
