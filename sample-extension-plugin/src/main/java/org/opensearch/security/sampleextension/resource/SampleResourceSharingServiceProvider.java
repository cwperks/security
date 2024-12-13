package org.opensearch.security.sampleextension.resource;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opensearch.common.inject.Provider;
import org.opensearch.security.spi.ResourceSharingService;

/**
 * Provider for ResourceSharingService that handles SampleResource instances.
 * This provider allows for flexible injection of different ResourceSharingService
 * implementations based on runtime conditions.
 */
public final class SampleResourceSharingServiceProvider implements Provider<ResourceSharingService<SampleResource>> {

    private volatile ResourceSharingService<SampleResource> resourceSharingService;

    private static final Map<ClassLoader, SampleResourceSharingServiceProvider> instances = new ConcurrentHashMap<>();

    private SampleResourceSharingServiceProvider() {}

    @SuppressWarnings("removal")
    public static SampleResourceSharingServiceProvider getInstance() {
        ClassLoader classLoader = AccessController.doPrivileged(
            (PrivilegedAction<ClassLoader>) () -> Thread.currentThread().getContextClassLoader()
        );
        instances.computeIfAbsent(classLoader, cl -> new SampleResourceSharingServiceProvider());
        return instances.get(classLoader);
    }

    /**
     * Sets the resource sharing service implementation.
     * This method is thread-safe and ensures the service is only set once.
     *
     * @param resourceSharingService the service implementation to use
     * @throws IllegalStateException if the service has already been set
     * @throws IllegalArgumentException if the provided service is null
     */
    public void set(ResourceSharingService<SampleResource> resourceSharingService) {
        if (resourceSharingService == null) {
            throw new IllegalArgumentException("ResourceSharingService cannot be null");
        }

        if (this.resourceSharingService != null) {
            throw new IllegalStateException("ResourceSharingService has already been set");
        }

        this.resourceSharingService = resourceSharingService;
    }

    /**
     * {@inheritDoc}
     *
     * @return the configured ResourceSharingService
     */
    @Override
    public ResourceSharingService<SampleResource> get() {
        return resourceSharingService;
    }
}
