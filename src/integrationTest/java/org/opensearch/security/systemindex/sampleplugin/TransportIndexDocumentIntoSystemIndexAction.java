/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 */

package org.opensearch.security.systemindex.sampleplugin;

import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.client.Client;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.action.ActionListener;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

public class TransportIndexDocumentIntoSystemIndexAction extends HandledTransportAction<
    IndexDocumentIntoSystemIndexRequest,
    IndexDocumentIntoSystemIndexResponse> {

    private final Client client;
    private final ThreadPool threadPool;
    private final Client pluginClient;

    @Inject
    public TransportIndexDocumentIntoSystemIndexAction(
        final TransportService transportService,
        final ActionFilters actionFilters,
        final Client client,
        final ThreadPool threadPool,
        final RunAsClientWrapper pluginClient
    ) {
        super(IndexDocumentIntoSystemIndexAction.NAME, transportService, actionFilters, IndexDocumentIntoSystemIndexRequest::new);
        this.client = client;
        this.threadPool = threadPool;
        this.pluginClient = pluginClient.get();
    }

    @Override
    protected void doExecute(
        Task task,
        IndexDocumentIntoSystemIndexRequest request,
        ActionListener<IndexDocumentIntoSystemIndexResponse> actionListener
    ) {
        String indexName = request.getIndexName();
        String runAs = request.getRunAs();
        try {
            pluginClient.admin().indices().create(new CreateIndexRequest(indexName), ActionListener.wrap(r -> {
                if ("user".equalsIgnoreCase(runAs)) {
                    client.index(
                        new IndexRequest(indexName).setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                            .source("{\"content\":1}", XContentType.JSON),
                        ActionListener.wrap(r2 -> {
                            actionListener.onResponse(
                                new IndexDocumentIntoSystemIndexResponse(true, "successfully indexed document into system index")
                            );
                        }, actionListener::onFailure)
                    );
                } else {
                    pluginClient.index(
                        new IndexRequest(indexName).setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                            .source("{\"content\":1}", XContentType.JSON),
                        ActionListener.wrap(r2 -> {
                            actionListener.onResponse(
                                new IndexDocumentIntoSystemIndexResponse(true, "successfully indexed document into system index")
                            );
                        }, actionListener::onFailure)
                    );
                }
            }, actionListener::onFailure));
        } catch (Exception ex) {
            throw new RuntimeException("Unexpected error: " + ex.getMessage());
        }
    }
}
