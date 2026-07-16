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

package org.opensearch.security.privileges.actionlevel.legacy;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Test;

import org.opensearch.cluster.metadata.IndexAbstraction;
import org.opensearch.security.util.MockIndexMetadataBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

public class IndexResolverReplacerTest {

    private static final int ALIAS_COUNT = 10_000;
    private static final String INDEX = "alias-scale-index";
    private static final String ALIAS_PREFIX = "alias-scale-";
    private static final SortedMap<String, IndexAbstraction> ALIAS_HEAVY_LOOKUP = createAliasHeavyLookup();

    @Test
    public void exactConcreteIndexDoesNotTraverseAliasLookup() {
        TrackingSortedMap lookup = aliasHeavyLookup();

        assertThat(IndexResolverReplacer.resolveMatchingAliases(lookup, Set.of(INDEX)), empty());
        assertThat(lookup.getCalls, equalTo(1));
        assertThat(lookup.fullEntrySetCalls, equalTo(0));
        assertThat(lookup.subMapCalls, equalTo(0));
    }

    @Test
    public void exactAliasDoesNotTraverseAliasLookup() {
        TrackingSortedMap lookup = aliasHeavyLookup();
        String alias = ALIAS_PREFIX + (ALIAS_COUNT - 1);

        assertThat(IndexResolverReplacer.resolveMatchingAliases(lookup, Set.of(alias)), containsInAnyOrder(alias));
        assertThat(lookup.getCalls, equalTo(1));
        assertThat(lookup.fullEntrySetCalls, equalTo(0));
        assertThat(lookup.subMapCalls, equalTo(0));
    }

    @Test
    public void prefixAliasPatternUsesBoundedLookup() {
        TrackingSortedMap lookup = aliasHeavyLookup();

        assertThat(
            IndexResolverReplacer.resolveMatchingAliases(lookup, Set.of(ALIAS_PREFIX + "999*")),
            containsInAnyOrder(
                ALIAS_PREFIX + "999",
                ALIAS_PREFIX + "9990",
                ALIAS_PREFIX + "9991",
                ALIAS_PREFIX + "9992",
                ALIAS_PREFIX + "9993",
                ALIAS_PREFIX + "9994",
                ALIAS_PREFIX + "9995",
                ALIAS_PREFIX + "9996",
                ALIAS_PREFIX + "9997",
                ALIAS_PREFIX + "9998",
                ALIAS_PREFIX + "9999"
            )
        );
        assertThat(lookup.getCalls, equalTo(0));
        assertThat(lookup.fullEntrySetCalls, equalTo(0));
        assertThat(lookup.subMapCalls, equalTo(1));
        assertThat(lookup.subMapEntryCount, equalTo(11));
    }

    @Test
    public void complexAliasPatternFallsBackToFullLookup() {
        TrackingSortedMap lookup = aliasHeavyLookup();

        assertThat(
            IndexResolverReplacer.resolveMatchingAliases(lookup, Set.of(ALIAS_PREFIX + "9?9")),
            containsInAnyOrder(matchingAliases("9?9").toArray(new String[0]))
        );
        assertThat(lookup.fullEntrySetCalls, equalTo(1));
    }

    private static TrackingSortedMap aliasHeavyLookup() {
        return new TrackingSortedMap(ALIAS_HEAVY_LOOKUP);
    }

    private static SortedMap<String, IndexAbstraction> createAliasHeavyLookup() {
        MockIndexMetadataBuilder builder = MockIndexMetadataBuilder.indices(INDEX);
        for (int i = 0; i < ALIAS_COUNT; i++) {
            builder.alias(ALIAS_PREFIX + i).of(INDEX);
        }
        return new TreeMap<>(builder.build());
    }

    private static Collection<String> matchingAliases(String pattern) {
        return java.util.stream.IntStream.range(0, ALIAS_COUNT)
            .mapToObj(i -> ALIAS_PREFIX + i)
            .filter(org.opensearch.security.support.WildcardMatcher.from(ALIAS_PREFIX + pattern))
            .toList();
    }

    private static class TrackingSortedMap extends AbstractMap<String, IndexAbstraction> implements SortedMap<String, IndexAbstraction> {

        private final SortedMap<String, IndexAbstraction> delegate;
        private int getCalls;
        private int fullEntrySetCalls;
        private int subMapCalls;
        private int subMapEntryCount;

        private TrackingSortedMap(SortedMap<String, IndexAbstraction> delegate) {
            this.delegate = delegate;
        }

        @Override
        public IndexAbstraction get(Object key) {
            getCalls++;
            return delegate.get(key);
        }

        @Override
        public Set<Entry<String, IndexAbstraction>> entrySet() {
            fullEntrySetCalls++;
            return delegate.entrySet();
        }

        @Override
        public Comparator<? super String> comparator() {
            return delegate.comparator();
        }

        @Override
        public SortedMap<String, IndexAbstraction> subMap(String fromKey, String toKey) {
            subMapCalls++;
            SortedMap<String, IndexAbstraction> result = delegate.subMap(fromKey, toKey);
            subMapEntryCount += result.size();
            return result;
        }

        @Override
        public SortedMap<String, IndexAbstraction> headMap(String toKey) {
            return delegate.headMap(toKey);
        }

        @Override
        public SortedMap<String, IndexAbstraction> tailMap(String fromKey) {
            return delegate.tailMap(fromKey);
        }

        @Override
        public String firstKey() {
            return delegate.firstKey();
        }

        @Override
        public String lastKey() {
            return delegate.lastKey();
        }
    }
}
