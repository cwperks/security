/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.security.identity;

import java.security.Principal;

import org.opensearch.identity.NamedPrincipal;
import org.opensearch.identity.Principals;
import org.opensearch.identity.Subject;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.user.User;
import org.opensearch.threadpool.ThreadPool;

public class SecuritySubject implements Subject {

    private final ThreadPool threadPool;

    public SecuritySubject(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }
    @Override
    public Principal getPrincipal() {
        final User user = (User) threadPool.getThreadContext().getTransient(ConfigConstants.OPENDISTRO_SECURITY_USER);
        if (user == null) {
            return Principals.UNAUTHENTICATED.getPrincipal();
        }
        return new NamedPrincipal(user.getName());
    }

    @Override
    public boolean isAuthenticated() {
        final User user = (User) threadPool.getThreadContext().getTransient(ConfigConstants.OPENDISTRO_SECURITY_USER);
        return user != null;
    }
}
