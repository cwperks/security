/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.sampleextension.actions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.support.ActionFilters;
import org.opensearch.client.Client;
import org.opensearch.common.inject.Inject;
import org.opensearch.security.spi.ShareWith;
import org.opensearch.security.spi.actions.UpdateResourceSharingTransportAction;
import org.opensearch.transport.TransportService;

import static org.opensearch.security.sampleextension.SampleExtensionPlugin.RESOURCE_INDEX_NAME;

/**
 * Transport action for UpdateSampleResourceSharing.
 */
public class UpdateSampleResourceSharingTransportAction extends UpdateResourceSharingTransportAction<SampleResource> {
    private static final Logger log = LogManager.getLogger(UpdateSampleResourceSharingTransportAction.class);

    @Inject
    public UpdateSampleResourceSharingTransportAction(TransportService transportService, ActionFilters actionFilters, Client nodeClient) {
        super(transportService, actionFilters, nodeClient, UpdateSampleResourceSharingAction.NAME, RESOURCE_INDEX_NAME, ShareWith::new);
    }
}
