/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.configuration;

import java.util.Map;

import org.opensearch.security.securityconf.impl.CType;
import org.opensearch.security.securityconf.impl.SecurityDynamicConfiguration;

/**
 * Callback function on change particular configuration
 */
public interface ConfigurationChangeListener {

    /**
     * @param configuration not null updated configuration on that was subscribe current listener
     */
    void onChange(Map<CType, SecurityDynamicConfiguration<?>> typeToConfig);
}
