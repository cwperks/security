/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.action.whoami;

import java.io.IOException;

import org.opensearch.action.ActionRequestBuilder;
import org.opensearch.client.ClusterAdminClient;
import org.opensearch.client.OpenSearchClient;

public class WhoAmIRequestBuilder extends ActionRequestBuilder<WhoAmIRequest, WhoAmIResponse> {
    public WhoAmIRequestBuilder(final ClusterAdminClient client) throws IOException {
        this(client, WhoAmIAction.INSTANCE);
    }

    public WhoAmIRequestBuilder(final OpenSearchClient client, final WhoAmIAction action) throws IOException {
        super(client, action, new WhoAmIRequest());
    }
}
