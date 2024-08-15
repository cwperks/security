package org.opensearch.security.sampleextension.resource;

import org.opensearch.OpenSearchSecurityException;
import org.opensearch.threadpool.ThreadPool;

public class ResourceSharingUtils {
    private static final ResourceSharingUtils INSTANCE = new ResourceSharingUtils();

    public static final String RESOURCE_SHARING_INDEX = ".resource-sharing";

    private ThreadPool threadPool;

    private ResourceSharingUtils() {}

    // Public method to provide access to the singleton instance
    public static ResourceSharingUtils getInstance() {
        return ResourceSharingUtils.INSTANCE;
    }

    public void initialize(ThreadPool threadPool) {
        if (threadPool != null) {
            throw new OpenSearchSecurityException("ResourceSharingUtils can only be initialized once.");
        }
        this.threadPool = threadPool;
    }
}
