package org.opensearch.security.spi;

import org.opensearch.client.Client;

public class DefaultResourceSharingService<T extends AbstractResource> extends AbstractResourceSharingService<T> {

    public DefaultResourceSharingService(Client client, String resourceIndex, Class<T> resourceClass) {
        super(client, resourceIndex, resourceClass);
    }
}
