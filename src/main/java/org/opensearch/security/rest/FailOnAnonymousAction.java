/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.security.rest;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.client.node.NodeClient;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestChannel;
import org.opensearch.rest.RestRequest;
import org.opensearch.security.filter.SecurityRequest;

import static org.opensearch.rest.RestRequest.Method.GET;
import static org.opensearch.security.OpenSearchSecurityPlugin.LEGACY_OPENDISTRO_PREFIX;
import static org.opensearch.security.OpenSearchSecurityPlugin.PLUGINS_PREFIX;
import static org.opensearch.security.dlic.rest.support.Utils.addRoutesPrefix;

public class FailOnAnonymousAction extends BaseRestHandler {
    private static final List<Route> routes = addRoutesPrefix(
        ImmutableList.of(new Route(GET, "/failonanonymous")),
        "/_opendistro/_security",
        "/_plugins/_security"
    );

    private static final String FAILONANONYMOUS_SUFFIX = "failonanonymous";
    private static final String REGEX_PATH_PREFIX = "/(" + LEGACY_OPENDISTRO_PREFIX + "|" + PLUGINS_PREFIX + ")/" + "(.*)";
    private static final Pattern PATTERN_PATH_PREFIX = Pattern.compile(REGEX_PATH_PREFIX);

    public static boolean isFailOnAnonymousEndpoint(final SecurityRequest request) {
        Matcher matcher = PATTERN_PATH_PREFIX.matcher(request.path());
        final String suffix = matcher.matches() ? matcher.group(2) : null;
        return FAILONANONYMOUS_SUFFIX.equals(suffix);
    }

    private final Logger log = LogManager.getLogger(this.getClass());

    public FailOnAnonymousAction() {
        super();
    }

    @Override
    public List<Route> routes() {
        return routes;
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        return new RestChannelConsumer() {

            @Override
            public void accept(RestChannel channel) throws Exception {
                XContentBuilder builder = channel.newBuilder(); // NOSONAR
                BytesRestResponse response = null;

                builder.startObject();
                builder.field("success", "true");

                builder.endObject();

                response = new BytesRestResponse(RestStatus.OK, builder);

                channel.sendResponse(response);
            }
        };
    }

    @Override
    public String getName() {
        return "OpenSearch Security Fail on Anonymous Action";
    }
}
