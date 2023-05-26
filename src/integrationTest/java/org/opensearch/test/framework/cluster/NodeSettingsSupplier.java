/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.test.framework.cluster;

import org.opensearch.common.settings.Settings;

@FunctionalInterface
public interface NodeSettingsSupplier {
    Settings get(int i);
}
