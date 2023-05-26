/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.privileges;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.index.query.MatchNoneQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.opensearch.security.resolver.IndexResolverReplacer.Resolved;
import org.opensearch.security.securityconf.SecurityRoles;
import org.opensearch.security.user.User;

public class TermsAggregationEvaluator {

    protected final Logger log = LogManager.getLogger(this.getClass());

    private static final String[] READ_ACTIONS = new String[] {
        "indices:data/read/msearch",
        "indices:data/read/mget",
        "indices:data/read/get",
        "indices:data/read/search",
        "indices:data/read/field_caps*"
        // "indices:admin/mappings/fields/get*"
    };

    private static final QueryBuilder NONE_QUERY = new MatchNoneQueryBuilder();

    public TermsAggregationEvaluator() {}

    public PrivilegesEvaluatorResponse evaluate(
        final Resolved resolved,
        final ActionRequest request,
        ClusterService clusterService,
        User user,
        SecurityRoles securityRoles,
        IndexNameExpressionResolver resolver,
        PrivilegesEvaluatorResponse presponse
    ) {
        try {
            if (request instanceof SearchRequest) {
                SearchRequest sr = (SearchRequest) request;

                if (sr.source() != null
                    && sr.source().query() == null
                    && sr.source().aggregations() != null
                    && sr.source().aggregations().getAggregatorFactories() != null
                    && sr.source().aggregations().getAggregatorFactories().size() == 1
                    && sr.source().size() == 0) {
                    AggregationBuilder ab = sr.source().aggregations().getAggregatorFactories().iterator().next();
                    if (ab instanceof TermsAggregationBuilder && "terms".equals(ab.getType()) && "indices".equals(ab.getName())) {
                        if ("_index".equals(((TermsAggregationBuilder) ab).field())
                            && ab.getPipelineAggregations().isEmpty()
                            && ab.getSubAggregations().isEmpty()) {

                            final Set<String> allPermittedIndices = securityRoles.getAllPermittedIndicesForDashboards(
                                resolved,
                                user,
                                READ_ACTIONS,
                                resolver,
                                clusterService
                            );
                            if (allPermittedIndices == null || allPermittedIndices.isEmpty()) {
                                sr.source().query(NONE_QUERY);
                            } else {
                                sr.source().query(new TermsQueryBuilder("_index", allPermittedIndices));
                            }

                            presponse.allowed = true;
                            return presponse.markComplete();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Unable to evaluate terms aggregation", e);
            return presponse;
        }

        return presponse;
    }
}
