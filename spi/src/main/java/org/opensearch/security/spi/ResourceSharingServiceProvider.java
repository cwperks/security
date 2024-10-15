package org.opensearch.security.spi;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResourceSharingServiceProvider {
    private final static Logger log = LogManager.getLogger(ResourceSharingServiceProvider.class);

    private static final Map<ClassLoader, ResourceSharingServiceProvider> instances = new ConcurrentHashMap<>();

    private boolean initialized;
    private AbstractResourceSharingService resourceSharingService;

    private ResourceSharingServiceProvider() {}

    @SuppressWarnings("removal")
    public static ResourceSharingServiceProvider getInstance() {
        ClassLoader classLoader = AccessController.doPrivileged(
            (PrivilegedAction<ClassLoader>) () -> Thread.currentThread().getContextClassLoader()
        );
        instances.computeIfAbsent(classLoader, cl -> new ResourceSharingServiceProvider());
        return instances.get(classLoader);
    }

    public void initialize(AbstractResourceSharingService resourceSharingService) {
        if (initialized) {
            return;
        }
        initialized = true;
        this.resourceSharingService = resourceSharingService;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
