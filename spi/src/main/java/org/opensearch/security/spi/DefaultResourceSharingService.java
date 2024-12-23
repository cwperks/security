package org.opensearch.security.spi;

import org.opensearch.core.action.ActionListener;

public class DefaultResourceSharingService<T extends Resource> implements ResourceSharingService<T> {

    @Override
    public void isSharedWithCurrentUser(String resourceId, ActionListener<Boolean> resourceSharingListener) {
        resourceSharingListener.onResponse(Boolean.TRUE);
    }
}
