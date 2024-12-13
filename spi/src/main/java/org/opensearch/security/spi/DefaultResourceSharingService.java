package org.opensearch.security.spi;

import org.opensearch.client.Client;

public class DefaultResourceSharingService<T extends Resource> extends AbstractResourceSharingService<T> {

    public DefaultResourceSharingService(Client client, String resourceIndex, ResourceFactory<T> resourceFactory) {
        super(client, resourceIndex, resourceFactory);
    }
}
