/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions.get;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.support.ActionFilters;
import org.opensearch.client.Client;
import org.opensearch.common.inject.Inject;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.security.sampleextension.resource.SampleResource;
import org.opensearch.security.sampleextension.resource.SampleResourceParser;
import org.opensearch.security.sampleextension.resource.SampleResourceSharingServiceProvider;
import org.opensearch.security.spi.actions.resource.get.GetResourceTransportAction;
import org.opensearch.transport.TransportService;

import static org.opensearch.security.sampleextension.SampleExtensionPlugin.RESOURCE_INDEX_NAME;

/**
 * Transport action for GetSampleResource.
 */
public class GetSampleResourceTransportAction extends GetResourceTransportAction<SampleResource> {
    private static final Logger log = LogManager.getLogger(GetSampleResourceTransportAction.class);

    @Inject
    public GetSampleResourceTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        SampleResourceSharingServiceProvider resourceSharingService,
        Client client,
        NamedXContentRegistry xContentRegistry
    ) {
        super(
            transportService,
            actionFilters,
            GetSampleResourceAction.NAME,
            RESOURCE_INDEX_NAME,
            resourceSharingService.get(),
            new SampleResourceParser(),
            client,
            xContentRegistry
        );
    }
}
