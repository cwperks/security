package org.opensearch.security.sampleextension.resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.OpenSearchSecurityException;
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

    private ThreadPool threadPool;

    private ResourceSharingUtils() {}

    // Public method to provide access to the singleton instance
    public static ResourceSharingUtils getInstance() {
        return ResourceSharingUtils.INSTANCE;
    }

    public void initialize(ThreadPool threadPool, Client client) {
        if (this.threadPool != null) {
            throw new OpenSearchSecurityException("ResourceSharingUtils can only be initialized once.");
        }
        this.threadPool = threadPool;

        try (ThreadContext.StoredContext ctx = threadPool.getThreadContext().stashContext()) {
            CreateIndexRequest cir = new CreateIndexRequest(RESOURCE_SHARING_INDEX);
            ActionListener<CreateIndexResponse> cirListener = ActionListener.wrap(
                response -> { log.info(RESOURCE_SHARING_INDEX + " created."); },
                (failResponse) -> {
                    /* Index already exists, ignore and continue */
                    log.info(RESOURCE_SHARING_INDEX + " exists.");
                }
            );
            client.admin().indices().create(cir, cirListener);
        }
    }
}
