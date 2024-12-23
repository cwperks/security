package org.opensearch.security.spi;

import org.opensearch.core.action.ActionListener;

public interface ResourceSharingService<T extends Resource> {
    void hasResourceBeenSharedWith(String resourceId, ActionListener<Boolean> resourceSharingListener);
}
