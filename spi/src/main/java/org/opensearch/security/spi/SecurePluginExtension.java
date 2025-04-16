package org.opensearch.security.spi;

public interface SecurePluginExtension {

    /**
     * This method returns a brief description of this plugin's use-case for
     * @return A description of the use-case
     */
    default String getDescription() {
        return """
                Plugin that requires additional privileges to operate independently in addition to direct system index access.

                The permissions this plugin requests are located in the plugin-permissions.yml file of the plugin.
            """;
    };
}
