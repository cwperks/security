/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.spi;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.ToXContentFragment;
import org.opensearch.core.xcontent.XContentBuilder;

public class ResourceUser implements ToXContentFragment {
    private final String name;

    private final Set<String> backendRoles;

    public ResourceUser(String name, Set<String> backendRoles) {
        this.name = name;
        this.backendRoles = backendRoles;
    }

    public String getName() {
        return name;
    }

    public Set<String> getBackendRoles() {
        return backendRoles;
    }

    @SuppressWarnings("unchecked")
    public static ResourceUser fromSource(Map<String, Object> sourceAsMap) {
        String name = (String) sourceAsMap.get("name");
        Set<String> backendRoles = new HashSet<>((List<String>) sourceAsMap.get("backend_roles"));
        return new ResourceUser(name, backendRoles);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        return builder.startObject().field("name", name).field("backend_roles", backendRoles).endObject();
    }
}
