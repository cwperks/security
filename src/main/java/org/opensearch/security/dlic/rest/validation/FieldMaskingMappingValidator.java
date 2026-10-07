/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.dlic.rest.validation;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.security.DefaultObjectMapper;
import org.opensearch.security.privileges.dlsfls.FieldMaskingDiagnostics;
import org.opensearch.security.support.WildcardMatcher;

import tools.jackson.databind.JsonNode;

/**
 * Best-effort diagnostics against existing mappings, never a condition for saving a role.
 * Inspects at most 100 matching index/expression pairs and 10,000 properties for wildcard expressions per role validation.
 * Missing indices, user-dependent patterns, multi-fields, and unknown plugin field types are not diagnosed.
 * Mapping types cannot prove that source values are strings; runtime values are not inspected here.
 */
public final class FieldMaskingMappingValidator {
    // Work limits, not pagination: inspection stops when a limit is reached.
    private static final int MAX_INDEX_EXPRESSION_PAIRS = 100;
    private static final int MAX_PROPERTIES_INSPECTED = 10_000;
    private static final int MAX_MAPPING_DEPTH = 50;

    private static final Set<String> NON_STRING_TYPES = Set.of(
        "byte",
        "short",
        "integer",
        "long",
        "unsigned_long",
        "half_float",
        "float",
        "double",
        "scaled_float",
        "boolean",
        "date",
        "date_nanos",
        "ip",
        "binary",
        "object",
        "nested",
        "geo_point",
        "geo_shape",
        "knn_vector"
    );

    private FieldMaskingMappingValidator() {}

    public static void inspect(JsonNode role, Metadata metadata) {
        var diagnostics = new FieldMaskingDiagnostics();
        try {
            inspect(
                role,
                metadata,
                finding -> diagnostics.warn(finding.index(), finding.field(), finding.type(), "role mapping inspection")
            );
        } catch (RuntimeException e) {
            // Advisory only: incomplete metadata or unresolved patterns must not change role-write behavior.
        }
    }

    record Finding(String index, String field, String type) {
    }

    static void inspect(JsonNode role, Metadata metadata, Consumer<Finding> warning) {
        int[] remaining = { MAX_PROPERTIES_INSPECTED };
        int pairs = 0;
        for (JsonNode permission : role.path("index_permissions")) {
            if (permission.path("masked_fields").isEmpty()) continue;
            for (IndexMetadata index : metadata.indices().values()) {
                if (index.mapping() == null || !matchesIndex(permission, index)) continue;
                if (pairs >= MAX_INDEX_EXPRESSION_PAIRS) return;
                try {
                    // Decode once for all expressions in this permission block, not once per field.
                    JsonNode mapping = DefaultObjectMapper.objectMapper().valueToTree(index.mapping().sourceAsMap());
                    for (JsonNode expression : permission.path("masked_fields")) {
                        if (++pairs > MAX_INDEX_EXPRESSION_PAIRS) return;
                        String field = expression.asText().split("::", 2)[0];
                        BiConsumer<String, String> report = (name, type) -> warning.accept(
                            new Finding(index.getIndex().getName(), name, type)
                        );
                        if (WildcardMatcher.isExactPattern(field)) {
                            inspectExactField(mapping, field, field, 0, report);
                        } else {
                            inspectProperties(mapping, "", WildcardMatcher.from(field), remaining, 0, report);
                        }
                    }
                } catch (RuntimeException e) {
                    // Mapping inspection is advisory; inability to inspect must not reject an otherwise valid role.
                    return;
                }
            }
        }
    }

    static void inspectExactField(JsonNode mapping, String field, String remainingPath, int depth, BiConsumer<String, String> warning) {
        if (depth > MAX_MAPPING_DEPTH) return;
        JsonNode properties = mapping.path("properties");
        JsonNode definition = properties.path(remainingPath);
        if (definition.isObject()) {
            String type = definition.path("type").asText("object");
            if (NON_STRING_TYPES.contains(type)) warning.accept(field, type);
        }
        // Try dotted object prefixes as well as ordinary object paths; literal dots need not be separators.
        for (int dot = remainingPath.indexOf('.'); dot >= 0; dot = remainingPath.indexOf('.', dot + 1)) {
            JsonNode object = properties.path(remainingPath.substring(0, dot));
            if (object.path("properties").isObject()) {
                inspectExactField(object, field, remainingPath.substring(dot + 1), depth + 1, warning);
            }
        }
        // Multi-fields and field aliases do not establish the shape of the corresponding source value.
    }

    private static boolean matchesIndex(JsonNode permission, IndexMetadata index) {
        for (JsonNode pattern : permission.path("index_patterns")) {
            // Attribute substitution depends on the eventual user; do not guess during role creation.
            if (pattern.asText().contains("${")) continue;
            WildcardMatcher matcher = WildcardMatcher.from(pattern.asText());
            if (matcher.test(index.getIndex().getName()) || index.getAliases().keySet().stream().anyMatch(matcher::test)) return true;
        }
        return false;
    }

    static void inspectProperties(
        JsonNode mapping,
        String prefix,
        WildcardMatcher pattern,
        int[] remaining,
        int depth,
        BiConsumer<String, String> warning
    ) {
        if (depth > MAX_MAPPING_DEPTH || remaining[0] <= 0) return;
        JsonNode properties = mapping.path("properties");
        if (!properties.isObject()) return;
        for (var entry : properties.properties()) {
            if (--remaining[0] < 0) return;
            JsonNode definition = entry.getValue();
            if (!definition.isObject()) continue;
            String field = prefix + entry.getKey();
            String type = definition.path("type").asText("object");
            if (NON_STRING_TYPES.contains(type) && pattern.test(field)) warning.accept(field, type);
            inspectProperties(definition, field + ".", pattern, remaining, depth + 1, warning);
        }
    }
}
