/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 */

package org.opensearch.security.systemindex.sampleplugin;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import org.opensearch.client.Client;
import org.opensearch.client.node.NodeClient;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;
import org.opensearch.security.DefaultObjectMapper;

import static java.util.Collections.singletonList;
import static org.opensearch.rest.RestRequest.Method.POST;

public class RestRunCodeAction extends BaseRestHandler {

    private final Client client;

    public RestRunCodeAction(Client client) {
        this.client = client;
    }

    @Override
    public List<Route> routes() {
        return singletonList(new Route(POST, "/run-code"));
    }

    @Override
    public String getName() {
        return "run_code";
    }

    @Override
    public RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) {
        final JsonNode json;
        try {
            json = DefaultObjectMapper.readTree(request.content().utf8ToString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        RunCodeRequest runRequest = new RunCodeRequest(json.get("code").asText());
        return channel -> client.execute(RunCodeAction.INSTANCE, runRequest, new RestToXContentListener<>(channel));
    }
}
