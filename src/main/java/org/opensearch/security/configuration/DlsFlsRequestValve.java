/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.configuration;

import org.opensearch.action.ActionListener;
import org.opensearch.action.ActionRequest;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.search.internal.SearchContext;
import org.opensearch.search.query.QuerySearchResult;
import org.opensearch.security.resolver.IndexResolverReplacer.Resolved;
import org.opensearch.security.securityconf.EvaluatedDlsFlsConfig;
import org.opensearch.threadpool.ThreadPool;

public interface DlsFlsRequestValve {

    boolean invoke(
        String action,
        ActionRequest request,
        ActionListener<?> listener,
        EvaluatedDlsFlsConfig evaluatedDlsFlsConfig,
        Resolved resolved
    );

    void handleSearchContext(SearchContext context, ThreadPool threadPool, NamedXContentRegistry namedXContentRegistry);

    void onQueryPhase(QuerySearchResult queryResult);

    public static class NoopDlsFlsRequestValve implements DlsFlsRequestValve {

        @Override
        public boolean invoke(
            String action,
            ActionRequest request,
            ActionListener<?> listener,
            EvaluatedDlsFlsConfig evaluatedDlsFlsConfig,
            Resolved resolved
        ) {
            return true;
        }

        @Override
        public void handleSearchContext(SearchContext context, ThreadPool threadPool, NamedXContentRegistry namedXContentRegistry) {

        }

        @Override
        public void onQueryPhase(QuerySearchResult queryResult) {

        }
    }

}
