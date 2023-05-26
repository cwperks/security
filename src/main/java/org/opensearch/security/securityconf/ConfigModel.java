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
import java.util.Set;

import org.opensearch.common.transport.TransportAddress;
import org.opensearch.security.user.User;

public abstract class ConfigModel {

    public abstract Map<String, Boolean> mapTenants(User user, Set<String> roles);

    public abstract Set<String> mapSecurityRoles(User user, TransportAddress caller);

    public abstract SecurityRoles getSecurityRoles();

    public abstract Set<String> getAllConfiguredTenantNames();
}
