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

package org.opensearch.security.resource;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.update.UpdateRequest;
import org.opensearch.action.update.UpdateResponse;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.index.shard.ShardId;
import org.opensearch.index.engine.Engine;
import org.opensearch.index.shard.IndexingOperationListener;
import org.opensearch.security.auth.UserSubjectImpl;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.user.User;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.client.Client;

public class ResourceSharingListener implements IndexingOperationListener {
    private final static Logger log = LogManager.getLogger(ResourceSharingListener.class);

    private static final ResourceSharingListener INSTANCE = new ResourceSharingListener();

    public static final String RESOURCE_SHARING_INDEX = ".resource-sharing";

    private static final String UNLIMITED = "unlimited";

    private boolean initialized;
    private ThreadPool threadPool;
    private Client client;

    private ResourceSharingListener() {}

    public static ResourceSharingListener getInstance() {
        return ResourceSharingListener.INSTANCE;
    }

    public void initialize(ThreadPool threadPool, Client client) {
        if (initialized) {
            return;
        }
        initialized = true;
        this.threadPool = threadPool;
        this.client = client;
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void postIndex(ShardId shardId, Engine.Index index, Engine.IndexResult result) {
        log.warn("postIndex called on " + shardId.getIndexName());
        String resourceId = index.id();
        String resourceIndex = shardId.getIndexName();
        UpdateRequest ur = new UpdateRequest(resourceIndex, resourceId);
        System.out.println("postIndex called on " + shardId.getIndexName());
        System.out.println("resourceId: " + resourceId);
        System.out.println("resourceIndex: " + resourceIndex);
        User authenticatedUser = ((UserSubjectImpl) client.threadPool()
            .getThreadContext()
            .getPersistent(ConfigConstants.OPENDISTRO_SECURITY_AUTHENTICATED_USER)).getUser();
        System.out.println("resourceUser: " + authenticatedUser);
        ResourceUser resourceUser = new ResourceUser(
            authenticatedUser.getName(),
            authenticatedUser.getSecurityRoles().stream().toList(),
            authenticatedUser.getRoles().stream().toList()
        );
        ur.doc(Map.of("resource_user", resourceUser));
        if (result.isCreated()) {
            ActionListener<UpdateResponse> urListener = ActionListener.wrap(
                updateResponse -> { log.info("Updated " + resourceIndex + " entry."); },
                (failResponse) -> {
                    log.error(failResponse.getMessage());
                    log.error("Failed to update " + resourceIndex + " entry.");
                }
            );
            client.update(ur, urListener);
        }
    }

    @Override
    public void postDelete(ShardId shardId, Engine.Delete delete, Engine.DeleteResult result) {
        log.warn("postDelete called on " + shardId.getIndexName());
    }
}
