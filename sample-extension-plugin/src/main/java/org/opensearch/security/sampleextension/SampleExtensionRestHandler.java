/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.sampleextension;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.opensearch.client.node.NodeClient;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestRequest;

/**
 * A sample rest handler that supports schedule and deschedule job operation
 *
 * Users need to provide "id", "index", "job_name", and "interval" parameter to schedule
 * a job. e.g.
 * {@code
 * POST /_plugins/scheduler_sample/watch?id=dashboards-job-id&job_name=watch dashboards index&index=.opensearch_dashboards_1&interval=1
 * }
 *
 * creates a job with id "dashboards-job-id" and job name "watch dashboards index",
 * which logs ".opensearch_dashboards_1" index's shards info every 1 minute
 *
 * Users can remove that job by calling
 * {@code DELETE /_plugins/scheduler_sample/watch?id=dashboards-job-id}
 */
public class SampleExtensionRestHandler extends BaseRestHandler {
    public static final String LIST_RESOURCE_URI = "/_plugins/resource_sample/resource";

    @Override
    public String getName() {
        return "Sample Security Resource Sharing extension handler";
    }

    @Override
    public List<Route> routes() {
        return Collections.unmodifiableList(Arrays.asList(new Route(RestRequest.Method.GET, LIST_RESOURCE_URI)));
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        if (request.method().equals(RestRequest.Method.GET)) {
            return restChannel -> { restChannel.sendResponse(new BytesRestResponse(RestStatus.OK, "List Resources called")); };
        } else {
            return restChannel -> {
                restChannel.sendResponse(new BytesRestResponse(RestStatus.METHOD_NOT_ALLOWED, request.method() + " is not allowed."));
            };
        }
    }
}
