/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.configuration;

import java.util.Set;

import org.opensearch.index.IndexService;
import org.opensearch.index.mapper.MapperService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class MockIndexServices {
    private MockIndexServices() {}

    /**
     * An index service whose mapping has no multi-fields or copy_to targets, so every field is its own source field.
     */
    static IndexService withoutDerivedFields() {
        final MapperService mapperService = mock(MapperService.class);
        when(mapperService.sourcePath(anyString())).thenAnswer(invocation -> Set.of(invocation.<String>getArgument(0)));
        final IndexService indexService = mock(IndexService.class);
        when(indexService.mapperService()).thenReturn(mapperService);
        return indexService;
    }
}
