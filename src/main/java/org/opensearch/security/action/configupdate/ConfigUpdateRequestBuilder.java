/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.action.configupdate;

import org.opensearch.action.ActionType;
import org.opensearch.action.support.nodes.NodesOperationRequestBuilder;
import org.opensearch.client.OpenSearchClient;

public class ConfigUpdateRequestBuilder extends NodesOperationRequestBuilder<
    ConfigUpdateRequest,
    ConfigUpdateResponse,
    ConfigUpdateRequestBuilder> {

    protected ConfigUpdateRequestBuilder(OpenSearchClient client, ActionType<ConfigUpdateResponse> action) {
        super(client, action, new ConfigUpdateRequest());
    }

    public ConfigUpdateRequestBuilder setShardId(final String[] configTypes) {
        request.setConfigTypes(configTypes);
        return this;
    }
}
