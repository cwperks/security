package org.opensearch.security.resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.client.Client;
import org.opensearch.core.index.shard.ShardId;
import org.opensearch.index.engine.Engine;
import org.opensearch.index.shard.IndexingOperationListener;
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

    }

    @Override
    public void postDelete(ShardId shardId, Engine.Delete delete, Engine.DeleteResult result) {
        log.warn("postDelete called on " + shardId.getIndexName());
    }
}
