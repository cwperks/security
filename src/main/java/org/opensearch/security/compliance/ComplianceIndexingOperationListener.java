/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.compliance;

import org.opensearch.index.IndexService;
import org.opensearch.index.shard.IndexingOperationListener;

/**
 * noop impl
 *
 *
 */
public class ComplianceIndexingOperationListener implements IndexingOperationListener {

    public void setIs(IndexService is) {
        // noop
    }
}
