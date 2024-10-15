package org.opensearch.security.spi;

import java.util.List;

import org.opensearch.core.action.ActionListener;

public interface ResourceSharingService<T extends Resource> {

    void listResources(ActionListener<List<T>> listResourceListener);
}
