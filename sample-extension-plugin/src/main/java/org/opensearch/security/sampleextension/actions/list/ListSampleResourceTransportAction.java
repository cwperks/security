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
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.security.sampleextension.actions.generic.list.ListResourceTransportAction;
import org.opensearch.security.sampleextension.resource.SampleResource;
import org.opensearch.transport.TransportService;
import org.opensearch.transport.client.Client;

import static org.opensearch.security.sampleextension.SampleExtensionPlugin.RESOURCE_INDEX_NAME;

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
            SampleResource::fromXContent,
            client,
            xContentRegistry
        );
    }
}
