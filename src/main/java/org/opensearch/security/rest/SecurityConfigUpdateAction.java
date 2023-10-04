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
import java.nio.file.Path;
import java.util.List;

import com.google.common.collect.ImmutableList;

import org.opensearch.client.node.NodeClient;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestController;
import org.opensearch.rest.RestRequest;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.rest.action.RestActions.NodesResponseRestListener;
import org.opensearch.security.action.configupdate.ConfigUpdateAction;
import org.opensearch.security.action.configupdate.ConfigUpdateRequest;
import org.opensearch.security.configuration.AdminDNs;
import org.opensearch.security.filter.SecurityRequestChannel;
import org.opensearch.security.filter.SecurityRequestFactory;
import org.opensearch.security.ssl.transport.PrincipalExtractor;
import org.opensearch.security.ssl.util.SSLRequestHelper;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.user.User;
import org.opensearch.threadpool.ThreadPool;

import static org.opensearch.rest.RestRequest.Method.PUT;
import static org.opensearch.security.dlic.rest.support.Utils.addRoutesPrefix;

public class SecurityConfigUpdateAction extends BaseRestHandler {

    private static final List<Route> routes = addRoutesPrefix(ImmutableList.of(new Route(PUT, "/configupdate")), "/_plugins/_security");

    private final ThreadContext threadContext;
    private final AdminDNs adminDns;
    private final Settings settings;
    private final Path configPath;
    private final PrincipalExtractor principalExtractor;

    public SecurityConfigUpdateAction(
        final Settings settings,
        final RestController controller,
        final ThreadPool threadPool,
        final AdminDNs adminDns,
        Path configPath,
        PrincipalExtractor principalExtractor
    ) {
        super();
        this.threadContext = threadPool.getThreadContext();
        this.adminDns = adminDns;
        this.settings = settings;
        this.configPath = configPath;
        this.principalExtractor = principalExtractor;
    }

    @Override
    public List<Route> routes() {
        return routes;
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        String[] configTypes = request.paramAsStringArrayOrEmptyIfAll("config_types");

        return channel -> {

            final SecurityRequestChannel securityRequest = SecurityRequestFactory.from(request, channel);
            SSLRequestHelper.SSLInfo sslInfo = SSLRequestHelper.getSSLInfo(settings, configPath, securityRequest, principalExtractor);

            if (sslInfo == null) {
                System.out.println("@84 - update config action 403");
                channel.sendResponse(new BytesRestResponse(RestStatus.FORBIDDEN, ""));
                return;
            }

            final User user = threadContext.getTransient(ConfigConstants.OPENDISTRO_SECURITY_USER);

            // only allowed for admins
            if (user == null || !adminDns.isAdmin(user)) {
                System.out.println("@93 - update config action 403");
                channel.sendResponse(new BytesRestResponse(RestStatus.FORBIDDEN, ""));
                return;
            } else {
                ConfigUpdateRequest configUpdateRequest = new ConfigUpdateRequest(configTypes);
                client.execute(ConfigUpdateAction.INSTANCE, configUpdateRequest, new NodesResponseRestListener<>(channel));
                return;
            }
        };
    }

    @Override
    public String getName() {
        return "Security config update";
    }

}
