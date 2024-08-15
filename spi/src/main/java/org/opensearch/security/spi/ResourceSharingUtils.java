package org.opensearch.security.spi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.opensearch.client.Client;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.core.action.ActionListener;
import org.opensearch.threadpool.ThreadPool;

public class ResourceSharingUtils {
    private final static Logger log = LogManager.getLogger(ResourceSharingUtils.class);

    private static final ResourceSharingUtils INSTANCE = new ResourceSharingUtils();

    public static final String RESOURCE_SHARING_INDEX = ".resource-sharing";

    private boolean initialized;
    private ThreadPool threadPool;
    private Client client;

    private ResourceSharingUtils() {}

    public static ResourceSharingUtils getInstance() {
        return ResourceSharingUtils.INSTANCE;
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

    private void createResourceSharingIndexIfNotExists() {
        try (ThreadContext.StoredContext ctx = this.threadPool.getThreadContext().stashContext()) {
            CreateIndexRequest cir = new CreateIndexRequest(RESOURCE_SHARING_INDEX);
            ActionListener<CreateIndexResponse> cirListener = ActionListener.wrap(
                response -> { log.info(RESOURCE_SHARING_INDEX + " created."); },
                (failResponse) -> {
                    /* Index already exists, ignore and continue */
                    log.info(RESOURCE_SHARING_INDEX + " exists.");
                }
            );
            this.client.admin().indices().create(cir, cirListener);
        }
    }

    public boolean indexResourceSharing(Resource resource) {
        createResourceSharingIndexIfNotExists();
        return true;
    }
}
