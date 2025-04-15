package org.opensearch.security.spi;

import org.opensearch.core.action.ActionListener;

public interface ResourceSharingService {
    void isSharedWithCurrentUser(String resourceId, ActionListener<Boolean> shareListener);
}
