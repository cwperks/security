/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.action.whoami;

import org.opensearch.action.ActionListener;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.settings.Settings;
import org.opensearch.security.configuration.AdminDNs;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.support.HeaderHelper;
import org.opensearch.security.user.User;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

public class TransportWhoAmIAction extends HandledTransportAction<WhoAmIRequest, WhoAmIResponse> {

    private final AdminDNs adminDNs;
    private final ThreadPool threadPool;

    @Inject
    public TransportWhoAmIAction(
        final Settings settings,
        final ThreadPool threadPool,
        final ClusterService clusterService,
        final TransportService transportService,
        final AdminDNs adminDNs,
        final ActionFilters actionFilters
    ) {

        super(WhoAmIAction.NAME, transportService, actionFilters, WhoAmIRequest::new);

        this.adminDNs = adminDNs;
        this.threadPool = threadPool;
    }

    @Override
    protected void doExecute(Task task, WhoAmIRequest request, ActionListener<WhoAmIResponse> listener) {
        final User user = threadPool.getThreadContext().getTransient(ConfigConstants.OPENDISTRO_SECURITY_USER);
        final String dn = user == null
            ? threadPool.getThreadContext().getTransient(ConfigConstants.OPENDISTRO_SECURITY_SSL_TRANSPORT_PRINCIPAL)
            : user.getName();
        final boolean isAdmin = adminDNs.isAdminDN(dn);
        final boolean isAuthenticated = isAdmin ? true : user != null;
        final boolean isNodeCertificateRequest = HeaderHelper.isInterClusterRequest(threadPool.getThreadContext())
            || HeaderHelper.isTrustedClusterRequest(threadPool.getThreadContext());

        listener.onResponse(new WhoAmIResponse(dn, isAdmin, isAuthenticated, isNodeCertificateRequest));

    }
}
