/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sample.actions.impl.list;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.support.ActionFilters;
import org.opensearch.client.Client;
import org.opensearch.common.inject.Inject;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.security.sample.actions.generic.list.ListResourceTransportAction;
import org.opensearch.security.sample.resource.SampleResource;
import org.opensearch.security.sample.resource.SampleResourceParser;
import org.opensearch.security.sample.resource.SampleResourceType;
import org.opensearch.transport.TransportService;

import static org.opensearch.security.sample.SampleResourcePlugin.RESOURCE_INDEX_NAME;

/**
 * Transport action for ListSampleResource.
 */
public class ListSampleResourceTransportAction extends ListResourceTransportAction<SampleResource> {
    private static final Logger log = LogManager.getLogger(ListSampleResourceTransportAction.class);

    @Inject
    public ListSampleResourceTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        NamedXContentRegistry xContentRegistry,
        Client client
    ) {
        super(
            transportService,
            actionFilters,
            ListSampleResourceAction.NAME,
            RESOURCE_INDEX_NAME,
            SampleResourceType.getInstance().getResourceSharingService(),
            new SampleResourceParser(),
            client,
            xContentRegistry
        );
    }
}
