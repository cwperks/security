/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.dlic.rest.validation;

import java.util.ArrayList;
import java.util.Map;

import org.apache.lucene.tests.util.LuceneTestCase;

import org.opensearch.Version;
import org.opensearch.cluster.metadata.AliasMetadata;
import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.cluster.metadata.MappingMetadata;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.common.compress.CompressedXContent;
import org.opensearch.common.settings.Settings;
import org.opensearch.security.DefaultObjectMapper;
import org.opensearch.security.support.WildcardMatcher;

public class FieldMaskingMappingValidatorTests extends LuceneTestCase {
    public void testExactLookupWithDisabledObjects() {
        var mapping = DefaultObjectMapper.objectMapper().readTree("""
            {"disable_objects": true, "properties": {
              "metrics": {"type": "keyword"},
              "metrics.count": {"type": "long"}
            }}
            """);
        var warnings = new ArrayList<String>();
        FieldMaskingMappingValidator.inspectExactField(
            mapping,
            "metrics.count",
            "metrics.count",
            0,
            (name, type) -> warnings.add(name + ":" + type)
        );
        assertEquals(java.util.List.of("metrics.count:long"), warnings);
    }

    public void testExactQualifiedNamesAndLiteralDots() {
        var mapping = DefaultObjectMapper.objectMapper().readTree("""
            {"properties": {
              "customer": {"type": "nested", "properties": {
                "address": {"properties": {"zip": {"type": "long"}}}
              }},
              "literal.dot": {"type": "boolean"},
              "dotted.object": {"properties": {"number": {"type": "integer"}}},
              "text": {"type": "text", "fields": {"count": {"type": "long"}}},
              "alias": {"type": "alias", "path": "customer.address.zip"}
            }}
            """);
        var warnings = new ArrayList<String>();
        for (String field : new String[] {
            "customer.address.zip",
            "literal.dot",
            "dotted.object.number",
            "zip",
            "missing",
            "text.count",
            "alias" }) {
            FieldMaskingMappingValidator.inspectExactField(mapping, field, field, 0, (name, type) -> warnings.add(name + ":" + type));
        }
        assertEquals(java.util.List.of("customer.address.zip:long", "literal.dot:boolean", "dotted.object.number:integer"), warnings);
    }

    public void testExactLookupAfterWildcardBudgetExhausted() throws Exception {
        var properties = DefaultObjectMapper.objectMapper().createObjectNode();
        for (int i = 0; i < 10_001; i++) {
            properties.putObject("unrelated" + i).put("type", "keyword");
        }
        properties.putObject("target").put("type", "long");
        var mapping = DefaultObjectMapper.objectMapper().createObjectNode().set("properties", properties);
        var metadata = Metadata.builder()
            .put(
                IndexMetadata.builder("test")
                    .settings(Settings.builder().put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT))
                    .numberOfShards(1)
                    .numberOfReplicas(0)
                    .putMapping(new MappingMetadata(new CompressedXContent("{\"_doc\":" + mapping + "}")))
            )
            .build();
        var role = DefaultObjectMapper.objectMapper().readTree("""
            {"index_permissions":[{"index_patterns":["test"],"masked_fields":["unrelated*","target"]}]}
            """);
        var warnings = new ArrayList<FieldMaskingMappingValidator.Finding>();
        FieldMaskingMappingValidator.inspect(role, metadata, warnings::add);
        assertEquals(java.util.List.of(new FieldMaskingMappingValidator.Finding("test", "target", "long")), warnings);
    }

    public void testUnrelatedIndicesDoNotConsumeInspectionLimit() {
        var builder = Metadata.builder();
        for (int i = 0; i < 101; i++) {
            builder.put(
                IndexMetadata.builder("test-" + i)
                    .settings(Settings.builder().put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT))
                    .numberOfShards(1)
                    .numberOfReplicas(0)
                    .putMapping(new MappingMetadata("_doc", Map.of("properties", Map.of("value", Map.of("type", "long")))))
            );
        }
        var metadata = builder.build();
        String lastIndex = new ArrayList<>(metadata.indices().keySet()).getLast();
        var role = DefaultObjectMapper.objectMapper()
            .readTree("{\"index_permissions\":[{\"index_patterns\":[\"" + lastIndex + "\"],\"masked_fields\":[\"value\"]}]}");
        var warnings = new ArrayList<FieldMaskingMappingValidator.Finding>();
        FieldMaskingMappingValidator.inspect(role, metadata, warnings::add);
        assertEquals(java.util.List.of(new FieldMaskingMappingValidator.Finding(lastIndex, "value", "long")), warnings);
    }

    public void testMixedMappingsAndAliases() throws Exception {
        Metadata.Builder metadata = Metadata.builder();
        for (String type : new String[] { "keyword", "long" }) {
            metadata.put(
                IndexMetadata.builder("test-" + type)
                    .settings(Settings.builder().put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT))
                    .numberOfShards(1)
                    .numberOfReplicas(0)
                    .putAlias(AliasMetadata.builder("shared-alias"))
                    .putMapping(new MappingMetadata("_doc", Map.of("properties", Map.of("value", Map.of("type", type)))))
            );
        }
        for (String pattern : new String[] { "test-*", "shared-alias" }) {
            var role = DefaultObjectMapper.objectMapper()
                .readTree("{\"index_permissions\":[{\"index_patterns\":[\"" + pattern + "\"],\"masked_fields\":[\"value\"]}]}");
            var warnings = new ArrayList<FieldMaskingMappingValidator.Finding>();
            FieldMaskingMappingValidator.inspect(role, metadata.build(), warnings::add);
            assertEquals(java.util.List.of(new FieldMaskingMappingValidator.Finding("test-long", "value", "long")), warnings);
        }
    }

    public void testMappedTypesAndNestedFields() {
        var mapping = DefaultObjectMapper.objectMapper().readTree("""
            {"properties": {
              "text": {"type": "text"},
              "keyword": {"type": "keyword"},
              "number": {"type": "long"},
              "object": {"properties": {"flag": {"type": "boolean"}}}
            }}
            """);
        var warnings = new ArrayList<String>();
        FieldMaskingMappingValidator.inspectProperties(
            mapping,
            "",
            WildcardMatcher.from("*"),
            new int[] { 100 },
            0,
            (field, type) -> warnings.add(field + ":" + type)
        );
        assertEquals(3, warnings.size());
        assertTrue(warnings.contains("number:long"));
        assertTrue(warnings.contains("object:object"));
        assertTrue(warnings.contains("object.flag:boolean"));
    }

    public void testInspectionIsBounded() {
        var warnings = new ArrayList<String>();
        FieldMaskingMappingValidator.inspectProperties(DefaultObjectMapper.objectMapper().readTree("""
            {"properties": {"number": {"type": "long"}}}
            """), "", WildcardMatcher.from("*"), new int[] { 0 }, 0, (field, type) -> warnings.add(field));
        assertTrue(warnings.isEmpty());
    }

    public void testFutureIndicesAreAllowed() {
        var role = DefaultObjectMapper.objectMapper()
            .readTree("{\"index_permissions\":[{\"index_patterns\":[\"future-*\"],\"masked_fields\":[\"number\"]}]}");
        FieldMaskingMappingValidator.inspect(role, Metadata.EMPTY_METADATA);
    }
}
