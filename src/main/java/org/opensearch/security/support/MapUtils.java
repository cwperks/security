/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MapUtils {

    public static void deepTraverseMap(final Map<String, Object> map, final Callback cb) {
        deepTraverseMap(map, cb, null);
    }

    private static void deepTraverseMap(final Map<String, Object> map, final Callback cb, final List<String> stack) {
        final List<String> localStack;
        if (stack == null) {
            localStack = new ArrayList<String>(30);
        } else {
            localStack = stack;
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() != null && entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                final Map<String, Object> inner = (Map<String, Object>) entry.getValue();
                localStack.add(entry.getKey());
                deepTraverseMap(inner, cb, localStack);
                if (!localStack.isEmpty()) {
                    localStack.remove(localStack.size() - 1);
                }
            } else {
                cb.call(entry.getKey(), map, Collections.unmodifiableList(localStack));
            }
        }
    }

    public static interface Callback {
        public void call(String key, Map<String, Object> map, List<String> stack);
    }
}
