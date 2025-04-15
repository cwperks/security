/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions.create;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.support.ActionFilters;
import org.opensearch.common.inject.Inject;
import org.opensearch.security.sampleextension.resource.SampleResource;
import org.opensearch.security.spi.actions.resource.create.CreateResourceTransportAction;
import org.opensearch.transport.TransportService;
import org.opensearch.transport.client.Client;

import static org.opensearch.security.sampleextension.SampleExtensionPlugin.RESOURCE_INDEX_NAME;

/**
 * Transport action for CreateSampleResource.
 */
public class CreateSampleResourceTransportAction extends CreateResourceTransportAction<SampleResource> {
    private static final Logger log = LogManager.getLogger(CreateSampleResourceTransportAction.class);

    @Inject
    public CreateSampleResourceTransportAction(TransportService transportService, ActionFilters actionFilters, Client nodeClient) {
        super(transportService, actionFilters, nodeClient, CreateSampleResourceAction.NAME, RESOURCE_INDEX_NAME, SampleResource::new);
    }
}
