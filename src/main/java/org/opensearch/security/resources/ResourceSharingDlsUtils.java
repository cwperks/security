/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.security.privileges.dlsfls.DlsRestriction;
import org.opensearch.security.privileges.dlsfls.DocumentPrivileges;
import org.opensearch.security.privileges.dlsfls.IndexToRuleMap;
import org.opensearch.security.user.User;

public class ResourceSharingDlsUtils {
    private static final Logger LOGGER = LogManager.getLogger(ResourceSharingDlsUtils.class);

    public static IndexToRuleMap<DlsRestriction> resourceRestrictions(
        NamedXContentRegistry xContentRegistry,
        Collection<String> resolvedIndices,
        User user,
        Collection<String> protectedTypes,
        String typeField
    ) {

        List<String> principals = new ArrayList<>();
        principals.add("public"); // matches resources shared via general_access
        principals.add("user:" + user.getName()); // owner

        // Security roles (OpenSearch Security roles)
        if (user.getSecurityRoles() != null) {
            user.getSecurityRoles().forEach(r -> principals.add("role:" + r));
        }

        // Backend roles (LDAP/SAML/etc)
        if (user.getRoles() != null) {
            user.getRoles().forEach(br -> principals.add("backend:" + br));
        }

        XContentBuilder builder = null;
        DlsRestriction restriction;
        try {
            // Build a bool query: match shared principals OR document type is not a protected resource type
            builder = XContentFactory.jsonBuilder();
            builder.startObject()
                .startObject("bool")
                .startArray("should")
                // Documents shared with this user (handle both keyword and text mappings)
                .startObject()
                .startObject("bool")
                .startArray("should")
                .startObject()
                .startObject("terms")
                .array("all_shared_principals", principals.toArray())
                .endObject()
                .endObject()
                .startObject()
                .startObject("terms")
                .array("all_shared_principals.keyword", principals.toArray())
                .endObject()
                .endObject()
                .endArray()
                .field("minimum_should_match", 1)
                .endObject()
                .endObject()
                // Documents whose type is NOT a protected resource type (pass through)
                .startObject()
                .startObject("bool")
                .startArray("must_not")
                .startObject()
                .startObject("terms")
                .array(typeField, protectedTypes.toArray())
                .endObject()
                .endObject()
                .startObject()
                .startObject("terms")
                .array(typeField + ".keyword", protectedTypes.toArray())
                .endObject()
                .endObject()
                .endArray()
                .endObject()
                .endObject()
                .endArray()
                .field("minimum_should_match", 1)
                .endObject()
                .endObject();

            String dlsJson = builder.toString();
            LOGGER.info(
                "[DLS] Resource sharing DLS query: principals={}, protectedTypes={}, query={}",
                principals,
                protectedTypes,
                dlsJson
            );
            restriction = new DlsRestriction(List.of(DocumentPrivileges.getRenderedDlsQuery(xContentRegistry, dlsJson)));
        } catch (IOException e) {
            LOGGER.warn("Received error while applying resource restrictions.", e);
            restriction = DlsRestriction.FULL;
        }

        ImmutableMap.Builder<String, DlsRestriction> mapBuilder = ImmutableMap.builder();
        for (String index : resolvedIndices) {
            mapBuilder.put(index, restriction);
        }
        return new IndexToRuleMap<>(mapBuilder.build());
    }
}
