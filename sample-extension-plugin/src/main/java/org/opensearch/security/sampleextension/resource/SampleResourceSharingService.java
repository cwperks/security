package org.opensearch.security.sampleextension.resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.security.sampleextension.actions.SampleResource;
import org.opensearch.security.spi.ResourceSharingService;

public class SampleResourceSharingService {
    private final static Logger log = LogManager.getLogger(SampleResourceSharingService.class);

    private static final SampleResourceSharingService INSTANCE = new SampleResourceSharingService();

    private boolean initialized;
    private ResourceSharingService<SampleResource> sharingService;

    private SampleResourceSharingService() {}

    public static SampleResourceSharingService getInstance() {
        return SampleResourceSharingService.INSTANCE;
    }

    public void initialize(ResourceSharingService<SampleResource> sharingService) {
        if (initialized) {
            return;
        }
        initialized = true;
        this.sharingService = sharingService;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public ResourceSharingService<SampleResource> getSharingService() {
        return sharingService;
    }
}
