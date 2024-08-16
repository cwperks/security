package org.opensearch.security.spi;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

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
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.threadpool.ThreadPool;

import static org.opensearch.common.xcontent.XContentFactory.jsonBuilder;

public class ResourceSharingUtils {
    private final static Logger log = LogManager.getLogger(ResourceSharingUtils.class);

    private static final Map<ClassLoader, ResourceSharingUtils> instances = new ConcurrentHashMap<>();

    public static final String RESOURCE_SHARING_INDEX = ".resource-sharing";

    private boolean initialized;
    private ThreadPool threadPool;
    private Client client;

    private ResourceSharingUtils() {}

    public static ResourceSharingUtils getInstance() {
        ClassLoader classLoader = AccessController.doPrivileged(
            (PrivilegedAction<ClassLoader>) () -> Thread.currentThread().getContextClassLoader()
        );
        instances.computeIfAbsent(classLoader, cl -> new ResourceSharingUtils());
        return instances.get(classLoader);
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

    public boolean indexResourceSharing(String resourceId, Resource resource, ShareWith shareWith, ActionListener<IndexResponse> listener)
        throws IOException {
        createResourceSharingIndexIfNotExists(() -> {
            ResourceSharingEntry entry = new ResourceSharingEntry(resource.getResourceIndex(), resourceId, shareWith);

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
        return true;
    }
}
