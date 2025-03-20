/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions.generic.get;

import java.io.IOException;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.OpenSearchException;
import org.opensearch.ResourceNotFoundException;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.common.CheckedFunction;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.common.xcontent.LoggingDeprecationHandler;
import org.opensearch.common.xcontent.XContentHelper;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.security.spi.SharableResource;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;
import org.opensearch.transport.client.Client;

/**
 * Transport action for GetResource.
 */
public class GetResourceTransportAction<T extends SharableResource> extends HandledTransportAction<
    GetResourceRequest,
    GetResourceResponse<T>> {
    private static final Logger log = LogManager.getLogger(GetResourceTransportAction.class);

    private final CheckedFunction<XContentParser, T, IOException> resourceParser;

    private final String resourceIndex;

    private final Client client;

    private final NamedXContentRegistry xContentRegistry;

    public GetResourceTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        String actionName,
        String resourceIndex,
        CheckedFunction<XContentParser, T, IOException> resourceParser,
        Client client,
        NamedXContentRegistry xContentRegistry
    ) {
        super(actionName, transportService, actionFilters, GetResourceRequest::new);
        Objects.requireNonNull(resourceParser);
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
        ActionListener<T> getResourceListener = ActionListener.wrap(
            resource -> { listener.onResponse(new GetResourceResponse<T>(resource)); },
            listener::onFailure
        );

        try (ThreadContext.StoredContext ignore = client.threadPool().getThreadContext().stashContext()) {
            GetRequest gr = new GetRequest(resourceIndex);
            gr.id(request.getResourceId());
            ActionListener<GetResponse> getListener = new ActionListener<>() {
                @Override
                public void onResponse(GetResponse getResponse) {
                    System.out.println("Get response: " + getResponse.isExists());
                    if (!getResponse.isExists()) {
                        getResourceListener.onFailure(new ResourceNotFoundException("Resource not found"));
                        return;
                    }
                    try {
                        XContentParser parser = XContentHelper.createParser(
                            xContentRegistry,
                            LoggingDeprecationHandler.INSTANCE,
                            getResponse.getSourceAsBytesRef(),
                            XContentType.JSON
                        );
                        T resource = resourceParser.apply(parser);
                        System.out.println("resource: " + resource);
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
        // resourceSharingService.getResource(request.getResourceId(), getResourceListener);
    }
}
