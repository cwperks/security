package org.opensearch.security.spi;

import org.opensearch.core.action.ActionListener;

public class NoopResourceSharingService implements ResourceSharingService {

    @Override
    public void isSharedWithCurrentUser(String resourceId, ActionListener<Boolean> resourceSharingListener) {
        resourceSharingListener.onResponse(Boolean.TRUE);
    }
}
