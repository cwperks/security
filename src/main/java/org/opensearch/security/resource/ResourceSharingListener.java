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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.client.Client;
import org.opensearch.core.index.shard.ShardId;
import org.opensearch.index.engine.Engine;
import org.opensearch.index.shard.IndexingOperationListener;
import org.opensearch.security.spi.ResourceSharingUtils;
import org.opensearch.security.spi.ShareWith;
import org.opensearch.threadpool.ThreadPool;

public class ResourceSharingListener implements IndexingOperationListener {
    private final static Logger log = LogManager.getLogger(ResourceSharingListener.class);

    private static final ResourceSharingListener INSTANCE = new ResourceSharingListener();

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
        try {
            ResourceSharingUtils.getInstance().indexResourceSharing(resourceId, resourceIndex, ShareWith.PRIVATE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void postDelete(ShardId shardId, Engine.Delete delete, Engine.DeleteResult result) {
        log.warn("postDelete called on " + shardId.getIndexName());
    }
}
