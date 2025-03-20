/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.rest.resource;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.action.update.UpdateRequest;
import org.opensearch.action.update.UpdateResponse;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;
import org.opensearch.transport.client.Client;

/**
 * Transport action for UpdateResourceSharing.
 */
public class ShareWithTransportAction extends HandledTransportAction<ShareWithRequest, ShareWithResponse> {
    private static final Logger log = LogManager.getLogger(ShareWithTransportAction.class);

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
            GetRequest getRequest = new GetRequest(request.getResourceIndex());
            getRequest.id(request.getResourceId());

            nodeClient.get(getRequest, new ActionListener<>() {
                @Override
                public void onResponse(GetResponse getResponse) {
                    if (getResponse.isExists()) {
                        // TODO ensure the update does not overwrite existing values
                        UpdateRequest updateRequest = new UpdateRequest(request.getResourceIndex(), request.getResourceId());
                        updateRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
                        try {
                            XContentBuilder builder = XContentFactory.jsonBuilder();
                            builder.startObject();
                            {
                                builder.startArray("share_with");
                                {
                                    builder.startObject();
                                    builder.field("allowed_actions", request.getShareWith().getAllowedActions());
                                    builder.field("users", request.getShareWith().getUsers());
                                    builder.field("backend_roles", request.getShareWith().getBackendRoles());
                                    builder.endObject();
                                }
                                builder.endArray();
                            }
                            builder.endObject();
                            updateRequest.doc(builder);

                            nodeClient.update(updateRequest, new ActionListener<>() {
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
                        listener.onFailure(new IllegalStateException("Resource not found"));
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
