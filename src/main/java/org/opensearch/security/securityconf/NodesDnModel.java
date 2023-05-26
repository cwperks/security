/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.securityconf;

import java.util.Map;

import org.opensearch.security.support.WildcardMatcher;

public abstract class NodesDnModel {
    public abstract Map<String, WildcardMatcher> getNodesDn();
}
