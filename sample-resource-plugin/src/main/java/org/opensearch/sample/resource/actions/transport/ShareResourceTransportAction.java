/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sample.resource.actions.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.common.inject.Inject;
import org.opensearch.core.action.ActionListener;
import org.opensearch.sample.SampleResourceExtension;
import org.opensearch.sample.resource.actions.rest.share.ShareResourceAction;
import org.opensearch.sample.resource.actions.rest.share.ShareResourceRequest;
import org.opensearch.sample.resource.actions.rest.share.ShareResourceResponse;
import org.opensearch.security.spi.resources.client.ResourceSharingClient;
import org.opensearch.security.spi.resources.sharing.ShareWith;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

import static org.opensearch.sample.utils.Constants.RESOURCE_INDEX_NAME;

/**
 * Transport action implementation for sharing a resource.
 */
public class ShareResourceTransportAction extends HandledTransportAction<ShareResourceRequest, ShareResourceResponse> {
    private static final Logger log = LogManager.getLogger(ShareResourceTransportAction.class);
    private ThreadPool threadPool;
    private SampleResourceExtension resourceExtension;

    @Inject
    public ShareResourceTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        ThreadPool threadPool,
        SampleResourceExtension resourceExtension
    ) {
        super(ShareResourceAction.NAME, transportService, actionFilters, ShareResourceRequest::new);
        this.threadPool = threadPool;
        this.resourceExtension = resourceExtension;
    }

    @Override
    protected void doExecute(Task task, ShareResourceRequest request, ActionListener<ShareResourceResponse> listener) {
        if (request.getResourceId() == null || request.getResourceId().isEmpty()) {
            listener.onFailure(new IllegalArgumentException("Resource ID cannot be null or empty"));
            return;
        }

        final Object userSubject = (Object) threadPool.getThreadContext().getPersistent("_opendistro_security_authenticated_user");
        System.out.println("userSubject: " + userSubject);
        System.out.println("resourceExtension.getResourceSharingClientAccessor(): " + resourceExtension.getResourceSharingClientAccessor());

        ResourceSharingClient resourceSharingClient = resourceExtension.getResourceSharingClientAccessor().getResourceSharingClient();
        ShareWith shareWith = request.getShareWith();
        resourceSharingClient.share(request.getResourceId(), RESOURCE_INDEX_NAME, shareWith, ActionListener.wrap(sharing -> {
            ShareWith finalShareWith = sharing == null ? null : sharing.getShareWith();
            ShareResourceResponse response = new ShareResourceResponse(finalShareWith);
            log.debug("Shared resource: {}", response.toString());
            listener.onResponse(response);
        }, listener::onFailure));
    }

}
