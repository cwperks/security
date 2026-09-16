/*
 * SPDX-License-Identifier: Apache-2.0
 *   Copyright OpenSearch Contributors
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package org.opensearch.security.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.rule.attribute_extractor.AttributeExtractor;
import org.opensearch.rule.autotagging.Attribute;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.user.User;
import org.opensearch.threadpool.ThreadPool;

/**
 * Extracts the exact username and mapped Security roles from the current request's user.
 */
public class PrincipalExtractor implements AttributeExtractor<String> {
    private final ThreadPool threadPool;
    private final Function<User, Set<String>> mappedRolesResolver;

    public PrincipalExtractor(ThreadPool threadPool, Function<User, Set<String>> mappedRolesResolver) {
        this.threadPool = threadPool;
        this.mappedRolesResolver = mappedRolesResolver;
    }

    @Override
    public Attribute getAttribute() {
        return PrincipalAttribute.PRINCIPAL;
    }

    @Override
    public Iterable<String> extract() {
        ThreadContext threadContext = threadPool.getThreadContext();
        User user = threadContext.getTransient(ConfigConstants.OPENDISTRO_SECURITY_USER);
        List<String> principals = new ArrayList<>();
        if (user != null) {
            principals.add(String.join("|", PrincipalAttribute.USERNAME, user.getName()));
            // Backend roles are not necessarily the effective Security roles used by WLM rules.
            for (String role : mappedRolesResolver.apply(user)) {
                principals.add(String.join("|", PrincipalAttribute.ROLE, role));
            }
        }
        return principals;
    }

    @Override
    public LogicalOperator getLogicalOperator() {
        return LogicalOperator.OR;
    }

}
