package org.opensearch.security.sample.resource;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opensearch.plugins.resource.ResourceSharingService;
import org.opensearch.plugins.resource.ResourceType;

import static org.opensearch.security.sample.SampleResourcePlugin.RESOURCE_INDEX_NAME;

public class SampleResourceType implements ResourceType {
    private volatile ResourceSharingService resourceSharingService;

    private static final Map<ClassLoader, SampleResourceType> instances = new ConcurrentHashMap<>();

    private SampleResourceType() {}

    @SuppressWarnings("removal")
    public static SampleResourceType getInstance() {
        ClassLoader classLoader = AccessController.doPrivileged(
            (PrivilegedAction<ClassLoader>) () -> Thread.currentThread().getContextClassLoader()
        );
        instances.computeIfAbsent(classLoader, cl -> new SampleResourceType());
        return instances.get(classLoader);
    }

    @Override
    public String getResourceType() {
        return "sample_resource";
    }

    @Override
    public String getResourceIndex() {
        return RESOURCE_INDEX_NAME;
    }

    @Override
    public void assignResourceSharingService(ResourceSharingService resourceSharingService) {
        this.resourceSharingService = resourceSharingService;
    }

    public ResourceSharingService getResourceSharingService() {
        return this.resourceSharingService;
    }
}
