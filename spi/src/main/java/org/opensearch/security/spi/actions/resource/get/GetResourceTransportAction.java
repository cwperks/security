/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.spi.actions.resource.get;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.OpenSearchException;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
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
import org.opensearch.security.spi.Resource;
import org.opensearch.security.spi.ResourceParser;
import org.opensearch.security.spi.ResourceSharingService;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

/**
 * Transport action for GetResource.
 */
public class GetResourceTransportAction<T extends Resource> extends HandledTransportAction<GetResourceRequest, GetResourceResponse<T>> {
    private static final Logger log = LogManager.getLogger(GetResourceTransportAction.class);

    private final ResourceSharingService<T> resourceSharingService;

    private final ResourceParser<T> resourceParser;

    private final String resourceIndex;

    private final Client client;

    private final NamedXContentRegistry xContentRegistry;

    public GetResourceTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        String actionName,
        String resourceIndex,
        ResourceSharingService<T> resourceSharingService,
        ResourceParser<T> resourceParser,
        Client client,
        NamedXContentRegistry xContentRegistry
    ) {
        super(actionName, transportService, actionFilters, GetResourceRequest::new);
        this.resourceSharingService = resourceSharingService;
        this.resourceParser = resourceParser;
        this.resourceIndex = resourceIndex;
        this.client = client;
        this.xContentRegistry = xContentRegistry;
    }

    @Override
    protected void doExecute(Task task, GetResourceRequest request, ActionListener<GetResourceResponse<T>> actionListener) {
        getResource(request, actionListener);
    }

    private void getResource(GetResourceRequest request, ActionListener<GetResourceResponse<T>> listener) {
        ActionListener<T> getResourceListener = ActionListener.wrap(resource -> {
            System.out.println("resource: " + resource);
            listener.onResponse(new GetResourceResponse<T>(resource));
        }, listener::onFailure);

        try (ThreadContext.StoredContext ignore = client.threadPool().getThreadContext().stashContext()) {
            GetRequest gr = new GetRequest(resourceIndex);
            gr.id(request.getResourceId());
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
                        ActionListener<Boolean> shareListener = new ActionListener<>() {
                            @Override
                            public void onResponse(Boolean isShared) {
                                if (isShared) {
                                    getResourceListener.onResponse(resource);
                                } else {
                                    getResourceListener.onFailure(
                                        new OpenSearchException("User is not authorized to access this resource")
                                    );
                                }
                            }

                            @Override
                            public void onFailure(Exception e) {
                                getResourceListener.onFailure(
                                    new OpenSearchException("Failed to check sharing status: " + e.getMessage(), e)
                                );
                            }
                        };

                        resourceSharingService.hasResourceBeenSharedWith(request.getResourceId(), shareListener);
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
        // resourceSharingService.getResource(request.getResourceId(), getResourceListener);
    }
}
