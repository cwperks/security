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

import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.client.Client;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.index.shard.ShardId;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.index.engine.Engine;
import org.opensearch.index.shard.IndexingOperationListener;
import org.opensearch.security.spi.Resource;
import org.opensearch.security.spi.ShareWith;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.user.User;
import org.opensearch.threadpool.ThreadPool;

import static org.opensearch.common.xcontent.XContentFactory.jsonBuilder;

public class ResourceSharingListener implements IndexingOperationListener {
    private final static Logger log = LogManager.getLogger(ResourceSharingListener.class);

    private static final ResourceSharingListener INSTANCE = new ResourceSharingListener();

    public static final String RESOURCE_SHARING_INDEX = ".resource-sharing";

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
        System.out.println("postIndex called on " + shardId.getIndexName());
        System.out.println("resourceId: " + resourceId);
        System.out.println("resourceIndex: " + resourceIndex);
        User resourceUser = (User) client.threadPool()
            .getThreadContext()
            .getPersistent(ConfigConstants.OPENDISTRO_SECURITY_AUTHENTICATED_USER);
        System.out.println("resourceUser: " + resourceUser);
        try {
            indexResourceSharing(resourceId, resourceIndex, resourceUser, ShareWith.PRIVATE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void postDelete(ShardId shardId, Engine.Delete delete, Engine.DeleteResult result) {
        log.warn("postDelete called on " + shardId.getIndexName());
    }

    private void createResourceSharingIndexIfNotExists(Callable<Boolean> callable) {
        try (ThreadContext.StoredContext ctx = this.threadPool.getThreadContext().stashContext()) {
            CreateIndexRequest cir = new CreateIndexRequest(RESOURCE_SHARING_INDEX);
            ActionListener<CreateIndexResponse> cirListener = ActionListener.wrap(response -> {
                log.warn(RESOURCE_SHARING_INDEX + " created.");
                callable.call();
            }, (failResponse) -> {
                /* Index already exists, ignore and continue */
                log.warn(RESOURCE_SHARING_INDEX + " exists.");
                try {
                    callable.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            this.client.admin().indices().create(cir, cirListener);
        }
    }

    public void indexResourceSharing(
        String resourceId,
        Resource resource,
        User resourceUser,
        ShareWith shareWith,
        ActionListener<IndexResponse> listener
    ) throws IOException {
        createResourceSharingIndexIfNotExists(() -> {
            ResourceSharingEntry entry = new ResourceSharingEntry(resource.getResourceIndex(), resourceId, resourceUser, shareWith);

            IndexRequest ir = client.prepareIndex(RESOURCE_SHARING_INDEX)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                .setSource(entry.toXContent(jsonBuilder(), ToXContent.EMPTY_PARAMS))
                .request();

            log.warn("Index Request: " + ir.toString());

            ActionListener<IndexResponse> irListener = ActionListener.wrap(idxResponse -> {
                log.warn("Created " + RESOURCE_SHARING_INDEX + " entry.");
                listener.onResponse(idxResponse);
            }, (failResponse) -> {
                log.error(failResponse.getMessage());
                log.error("Failed to create " + RESOURCE_SHARING_INDEX + " entry.");
                listener.onFailure(failResponse);
            });
            client.index(ir, irListener);
            return null;
        });
    }

    public void indexResourceSharing(String resourceId, String resourceIndex, User resourceUser, ShareWith shareWith) throws IOException {
        createResourceSharingIndexIfNotExists(() -> {
            ResourceSharingEntry entry = new ResourceSharingEntry(resourceIndex, resourceId, resourceUser, shareWith);

            IndexRequest ir = client.prepareIndex(RESOURCE_SHARING_INDEX)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                .setSource(entry.toXContent(jsonBuilder(), ToXContent.EMPTY_PARAMS))
                .request();

            log.warn("Index Request: " + ir.toString());

            ActionListener<IndexResponse> irListener = ActionListener.wrap(
                idxResponse -> { log.warn("Created " + RESOURCE_SHARING_INDEX + " entry."); },
                (failResponse) -> {
                    log.error(failResponse.getMessage());
                    log.error("Failed to create " + RESOURCE_SHARING_INDEX + " entry.");
                }
            );
            client.index(ir, irListener);
            return null;
        });
    }
}
