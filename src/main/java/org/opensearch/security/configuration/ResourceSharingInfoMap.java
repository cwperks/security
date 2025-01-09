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

package org.opensearch.security.configuration;

import java.util.Set;

import com.google.common.collect.ImmutableMap;

import org.opensearch.security.spi.ResourceSharingInfo;

/**
 * Data structure that holds the result when loading resource by id
 */
public class ResourceSharingInfoMap {
    public static final ResourceSharingInfoMap EMPTY = new ResourceSharingInfoMap(ImmutableMap.of());

    private final ImmutableMap<String, ResourceSharingInfo> map;

    private ResourceSharingInfoMap(ImmutableMap<String, ResourceSharingInfo> map) {
        this.map = map;
    }

    public ResourceSharingInfo get(String resourceId) {
        ResourceSharingInfo sharingInfo = map.get(resourceId);

        if (sharingInfo == null) {
            return null;
        }

        // TODO Add resource_id to the sharingInfo object and perform this check
        // if (!config.getCType().equals(ctype)) {
        // throw new RuntimeException("Stored configuration does not match type: " + ctype + "; " + config);
        // }

        return sharingInfo;
    }

    public boolean containsKey(String resourceId) {
        return map.containsKey(resourceId);
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public int size() {
        return this.map.size();
    }

    public ImmutableMap<String, ResourceSharingInfo> rawMap() {
        return this.map;
    }

    public static class Builder {
        private ImmutableMap.Builder<String, ResourceSharingInfo> map = new ImmutableMap.Builder<>();

        public Builder() {}

        public Builder with(String resourceId, ResourceSharingInfo sharingInfo) {
            map.put(resourceId, sharingInfo);
            return this;
        }

        public Builder with(ResourceSharingInfoMap sharingInfoMap) {
            map.putAll(sharingInfoMap.map);
            return this;
        }

        public ResourceSharingInfoMap build() {
            return new ResourceSharingInfoMap(this.map.build());
        }
    }
}
