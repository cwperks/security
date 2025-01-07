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

import org.opensearch.action.admin.cluster.health.ClusterHealthRequest;
import org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.client.Client;
import org.opensearch.common.inject.Inject;
import org.opensearch.core.action.ActionListener;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

public class TransportRunClusterHealthAction extends HandledTransportAction<RunClusterHealthRequest, RunClusterHealthResponse> {

    private final Client client;
    private final Client pluginClient;

    @Inject
    public TransportRunClusterHealthAction(
        final TransportService transportService,
        final ActionFilters actionFilters,
        final Client client,
        final RunAsClientWrapper pluginClient
    ) {
        super(RunClusterHealthAction.NAME, transportService, actionFilters, RunClusterHealthRequest::new);
        this.client = client;
        this.pluginClient = pluginClient.get();
    }

    @Override
    protected void doExecute(Task task, RunClusterHealthRequest request, ActionListener<RunClusterHealthResponse> actionListener) {
        String runAs = request.getRunAs();
        if ("plugin".equalsIgnoreCase(runAs)) {
            ActionListener<ClusterHealthResponse> chr = ActionListener.wrap(
                r -> { actionListener.onResponse(new RunClusterHealthResponse(true)); },
                actionListener::onFailure
            );
            pluginClient.admin().cluster().health(new ClusterHealthRequest(), chr);
        } else {
            ActionListener<ClusterHealthResponse> chr = ActionListener.wrap(
                r -> { actionListener.onResponse(new RunClusterHealthResponse(true)); },
                actionListener::onFailure
            );
            client.admin().cluster().health(new ClusterHealthRequest(), chr);
        }
    }
}
