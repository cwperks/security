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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestChannel;
import org.opensearch.rest.RestRequest;
import org.opensearch.security.configuration.ConfigurationRepository;
import org.opensearch.security.privileges.PrivilegesEvaluationContext;
import org.opensearch.security.privileges.PrivilegesEvaluator;
import org.opensearch.security.privileges.PrivilegesConfiguration;
import org.opensearch.security.securityconf.DynamicConfigFactory;
import org.opensearch.security.securityconf.impl.CType;
import org.opensearch.security.securityconf.impl.SecurityDynamicConfiguration;
import org.opensearch.security.securityconf.impl.v7.RoleV7;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.user.User;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.client.node.NodeClient;

import static org.opensearch.rest.RestRequest.Method.GET;
import static org.opensearch.security.dlic.rest.support.Utils.PLUGIN_ROUTE_PREFIX;
import static org.opensearch.security.dlic.rest.support.Utils.addRoutesPrefix;

/**
 * Returns the list of application IDs the current user has access to, based on
 * their mapped roles. Dashboards uses this to show/hide menu items.
 *
 * <p>Users mapped to {@code all_access} receive {@code ["*"]}.</p>
 */
public class ApplicationPermissionsInfoAction extends BaseRestHandler {

    private static final List<Route> routes = addRoutesPrefix(
        ImmutableList.of(new Route(GET, "/applicationpermissions")),
        PLUGIN_ROUTE_PREFIX
    );

    private final Logger log = LogManager.getLogger(this.getClass());
    private final PrivilegesConfiguration privilegesConfiguration;
    private final ConfigurationRepository configurationRepository;
    private final ThreadContext threadContext;

    private static final String ALL_ACCESS_ROLE = "all_access";

    public ApplicationPermissionsInfoAction(
        final PrivilegesConfiguration privilegesConfiguration,
        final ConfigurationRepository configurationRepository,
        final ThreadPool threadPool
    ) {
        super();
        this.privilegesConfiguration = privilegesConfiguration;
        this.configurationRepository = configurationRepository;
        this.threadContext = threadPool.getThreadContext();
    }

    @Override
    public List<Route> routes() {
        return routes;
    }

    @Override
    public String getName() {
        return "Application Permissions Info Action";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        return channel -> {
            XContentBuilder builder = channel.newBuilder();
            BytesRestResponse response = null;

            try {
                final User user = (User) threadContext.getTransient(ConfigConstants.OPENDISTRO_SECURITY_USER);

                if (user == null) {
                    builder.startObject();
                    builder.field("error", "No user found in thread context");
                    builder.endObject();
                    response = new BytesRestResponse(RestStatus.FORBIDDEN, builder);
                } else {
                    PrivilegesEvaluator privilegesEvaluator = privilegesConfiguration.privilegesEvaluator();
                    PrivilegesEvaluationContext context = privilegesEvaluator.createContext(user, "dummy:action");
                    Set<String> mappedRoles = context.getMappedRoles();

                    Set<String> applicationIds;

                    if (mappedRoles.contains(ALL_ACCESS_ROLE)) {
                        applicationIds = Set.of("*");
                    } else {
                        SecurityDynamicConfiguration<RoleV7> rolesConfig = configurationRepository.getConfiguration(CType.ROLES);
                        DynamicConfigFactory.addStatics(rolesConfig);

                        applicationIds = new HashSet<>();
                        for (Map.Entry<String, RoleV7> entry : rolesConfig.getCEntries().entrySet()) {
                            if (!mappedRoles.contains(entry.getKey())) {
                                continue;
                            }
                            String appId = entry.getValue().getApplication_id();
                            if (appId != null && !appId.isEmpty()) {
                                applicationIds.add(appId);
                            }
                        }
                    }

                    builder.startObject();
                    builder.field("user_name", user.getName());
                    builder.field("application_ids", applicationIds);
                    builder.endObject();
                    response = new BytesRestResponse(RestStatus.OK, builder);
                }
            } catch (final Exception e1) {
                log.error("Error building application permissions response", e1);
                builder = channel.newBuilder();
                builder.startObject();
                builder.field("error", e1.toString());
                builder.endObject();
                response = new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, builder);
            } finally {
                if (builder != null) {
                    builder.close();
                }
            }

            channel.sendResponse(response);
        };
    }
}
