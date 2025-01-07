package org.opensearch.security.systemindex.sampleplugin;

import java.util.Objects;

import org.opensearch.client.Client;

/**
 * Wrapper that holds an instance of a RunAsClient to transfer make this instance injectable to a
 * transport action for the SystemIndex1Plugin for testing
 */
public class RunAsClientWrapper {
    private Client pluginClient;

    public RunAsClientWrapper() {}

    public void initialize(Client pluginClient) {
        Objects.requireNonNull(pluginClient);
        this.pluginClient = pluginClient;
    }

    public Client get() {
        return pluginClient;
    }
}
