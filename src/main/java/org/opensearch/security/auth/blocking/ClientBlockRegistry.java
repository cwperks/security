/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.auth.blocking;

public interface ClientBlockRegistry<ClientIdType> {

    boolean isBlocked(ClientIdType clientId);

    void block(ClientIdType clientId);

    Class<ClientIdType> getClientIdType();
}
