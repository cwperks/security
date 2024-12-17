/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions.list;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.support.ActionFilters;
import org.opensearch.common.inject.Inject;
import org.opensearch.security.sampleextension.resource.SampleResource;
import org.opensearch.security.sampleextension.resource.SampleResourceSharingServiceProvider;
import org.opensearch.security.spi.actions.resource.list.ListResourceTransportAction;
import org.opensearch.transport.TransportService;

/**
 * Transport action for ListSampleResource.
 */
public class ListSampleResourceTransportAction extends ListResourceTransportAction<SampleResource> {
    private static final Logger log = LogManager.getLogger(ListSampleResourceTransportAction.class);

    @Inject
    public ListSampleResourceTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        SampleResourceSharingServiceProvider resourceSharingService
    ) {
        super(transportService, actionFilters, ListSampleResourceAction.NAME, resourceSharingService.get());
    }
}
