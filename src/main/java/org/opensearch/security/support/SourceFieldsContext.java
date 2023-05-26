/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.support;

import java.io.Serializable;
import java.util.Arrays;

import org.opensearch.action.get.GetRequest;
import org.opensearch.action.search.SearchRequest;

public class SourceFieldsContext implements Serializable {

    private String[] includes;
    private String[] excludes;
    // private String[] storedFields;
    private boolean fetchSource = true;

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public static boolean isNeeded(SearchRequest request) {
        return (request.source() != null
            && request.source().fetchSource() != null
            && (request.source().fetchSource().includes() != null || request.source().fetchSource().excludes() != null))
            || (request.source() != null
                && request.source().storedFields() != null
                && request.source().storedFields().fieldNames() != null
                && !request.source().storedFields().fieldNames().isEmpty());
    }

    public static boolean isNeeded(GetRequest request) {
        return (request.fetchSourceContext() != null
            && (request.fetchSourceContext().includes() != null || request.fetchSourceContext().excludes() != null))
            || (request.storedFields() != null && request.storedFields().length > 0);
    }

    public SourceFieldsContext() {
        super();
    }

    public SourceFieldsContext(SearchRequest request) {
        if (request.source() != null && request.source().fetchSource() != null) {
            includes = request.source().fetchSource().includes();
            excludes = request.source().fetchSource().excludes();
            fetchSource = request.source().fetchSource().fetchSource();
        }

        // if (request.source() != null && request.source().storedFields() != null && request.source().storedFields().fieldNames() != null)
        // {
        // storedFields = request.source().storedFields().fieldNames().toArray(new String[0]);
        // }
    }

    public SourceFieldsContext(GetRequest request) {
        if (request.fetchSourceContext() != null) {
            includes = request.fetchSourceContext().includes();
            excludes = request.fetchSourceContext().excludes();
            fetchSource = request.fetchSourceContext().fetchSource();
        }

        // storedFields = request.storedFields();
    }

    public String[] getIncludes() {
        return includes;
    }

    public String[] getExcludes() {
        return excludes;
    }

    // public String[] getStoredFields() {
    // return storedFields;
    // }

    public boolean hasIncludesOrExcludes() {
        return (includes != null && includes.length > 0) || (excludes != null && excludes.length > 0);
    }

    public boolean isFetchSource() {
        return fetchSource;
    }

    @Override
    public String toString() {
        return "SourceFieldsContext [includes="
            + Arrays.toString(includes)
            + ", excludes="
            + Arrays.toString(excludes)
            + ", fetchSource="
            + fetchSource
            + "]";
    }
}
