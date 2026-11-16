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
package org.opensearch.security.privileges.dlsfls;

import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FlsDocumentFilterTest {

    @Test
    public void generatedObjectsHonorExcludedOwners() throws Exception {
        var rule = FieldPrivileges.FlsRule.of("~title").withGeneratedFieldOwners(field -> {
            if (field.equals("custom") || field.startsWith("custom.")) return "title";
            return null;
        });
        Assert.assertFalse(rule.isAllowedRecursive("custom.chunks.text"));
        Assert.assertFalse(rule.isAllowedRecursive("custom.embedding.token"));
        Assert.assertTrue(rule.isAllowedRecursive("custom_other"));
        assertJsonStructurallyEquivalent("{\"public\":1}", FlsDocumentFilter.filter("""
            {"title":"secret","custom":{"chunks":[{"text":"secret","embedding":{"token":1}}]},"public":1}
            """.getBytes(UTF_8), rule, FieldMasking.FieldMaskingRule.ALLOW_ALL, ImmutableSet.of()));
    }

    @Test
    public void generatedObjectInclusionRequiresItsOwner() throws Exception {
        java.util.function.Function<String, String> owner = field -> field.equals("custom") || field.startsWith("custom.") ? "title" : null;
        var denied = FieldPrivileges.FlsRule.of("custom.chunks.text").withGeneratedFieldOwners(owner);
        Assert.assertFalse(denied.isAllowedRecursive("custom.chunks.text"));
        assertJsonStructurallyEquivalent("{}", FlsDocumentFilter.filter("""
            {"custom":{"chunks":[{"text":"secret"}]}}
            """.getBytes(UTF_8), denied, FieldMasking.FieldMaskingRule.ALLOW_ALL, ImmutableSet.of()));
        var allowed = FieldPrivileges.FlsRule.of("title", "custom.chunks.text").withGeneratedFieldOwners(owner);
        Assert.assertTrue(allowed.isAllowedRecursive("custom.chunks.text"));
        assertJsonStructurallyEquivalent("{\"custom\":{\"chunks\":[{\"text\":\"secret\"}]}}", FlsDocumentFilter.filter("""
            {"custom":{"chunks":[{"text":"secret","embedding":{"token":1}}]}}
            """.getBytes(UTF_8), allowed, FieldMasking.FieldMaskingRule.ALLOW_ALL, ImmutableSet.of()));
    }

    @Test
    public void generatedFieldChecksEveryOwner() throws Exception {
        var owners = java.util.Map.of("prefix", "custom.text", "custom.text", "title");
        Assert.assertFalse(FieldPrivileges.FlsRule.of("~title").withGeneratedFieldOwners(owners::get).isAllowedRecursive("prefix"));
        Assert.assertFalse(FieldPrivileges.FlsRule.of("~custom.text").withGeneratedFieldOwners(owners::get).isAllowedRecursive("prefix"));
        Assert.assertFalse(FieldPrivileges.FlsRule.of("~prefix").withGeneratedFieldOwners(owners::get).isAllowedRecursive("prefix"));
        Assert.assertTrue(FieldPrivileges.FlsRule.of("~other").withGeneratedFieldOwners(owners::get).isAllowedRecursive("prefix"));
        Assert.assertSame(FieldPrivileges.FlsRule.ALLOW_ALL, FieldPrivileges.FlsRule.ALLOW_ALL.withGeneratedFieldOwners(owners::get));
    }

    @Test
    public void identity() throws Exception {
        String sourceDocument = """
            {
                "big_integer": 12345678901234567890123456789012345678901234567890,
                "string": "x",
                "big_float": 12345678901234567890123456789012345678901234567890.123456789,
                "object": {
                   "attribute": "x",
                   "nested_object": {
                      "x": "y"
                   },
                   "nested_array": [1,2,3]
                },
                "array": [
                   1,
                   "x",
                   {
                      "foo": "bar"
                   },
                   [1,2,3,4]
                ]
            }
            """;

        byte[] result = FlsDocumentFilter.filter(
            sourceDocument.getBytes(UTF_8),
            FieldPrivileges.FlsRule.ALLOW_ALL,
            FieldMasking.FieldMaskingRule.ALLOW_ALL,
            ImmutableSet.of()
        );

        assertJsonStructurallyEquivalent(sourceDocument, result);
    }

    @Test
    public void filterSimpleAttribute_exclusion() throws Exception {
        String sourceDocument = """
            {
                "a": 41,
                "b": 42,
                "c": 43
            }
            """;

        byte[] result = FlsDocumentFilter.filter(
            sourceDocument.getBytes(UTF_8),
            FieldPrivileges.FlsRule.of("~b"),
            FieldMasking.FieldMaskingRule.ALLOW_ALL,
            ImmutableSet.of()
        );

        String expectedDocument = """
            {
                "a": 41,
                "c": 43
            }
            """;

        assertJsonStructurallyEquivalent(expectedDocument, result);
    }

    @Test
    public void filterSimpleAttribute_inclusion() throws Exception {
        String sourceDocument = """
            {
                "a": 41,
                "b": 42,
                "c": 43
            }
            """;

        byte[] result = FlsDocumentFilter.filter(
            sourceDocument.getBytes(UTF_8),
            FieldPrivileges.FlsRule.of("b"),
            FieldMasking.FieldMaskingRule.ALLOW_ALL,
            ImmutableSet.of()
        );

        String expectedDocument = """
            {
                "b": 42
            }
            """;

        assertJsonStructurallyEquivalent(expectedDocument, result);
    }

    @Test
    public void filterObject_exclusion() throws Exception {
        String sourceDocument = """
            {
                "a": 41,
                "b": {
                    "x": 123,
                    "y": 456
                },
                "c": 43
            }
            """;

        byte[] result = FlsDocumentFilter.filter(
            sourceDocument.getBytes(UTF_8),
            FieldPrivileges.FlsRule.of("~b"),
            FieldMasking.FieldMaskingRule.ALLOW_ALL,
            ImmutableSet.of()
        );

        String expectedDocument = """
            {
                "a": 41,
                "c": 43
            }
            """;

        assertJsonStructurallyEquivalent(expectedDocument, result);
    }

    @Test
    public void filterObjectAttribute_exclusion() throws Exception {
        String sourceDocument = """
            {
                "a": 41,
                "b": {
                    "x": 123,
                    "y": 456
                },
                "c": 43
            }
            """;

        byte[] result = FlsDocumentFilter.filter(
            sourceDocument.getBytes(UTF_8),
            FieldPrivileges.FlsRule.of("~b.x"),
            FieldMasking.FieldMaskingRule.ALLOW_ALL,
            ImmutableSet.of()
        );

        String expectedDocument = """
            {
                "a": 41,
                "b": {
                    "y": 456
                },
                "c": 43
            }
            """;

        assertJsonStructurallyEquivalent(expectedDocument, result);
    }

    @Test
    public void filterObjectAttribute_inclusion() throws Exception {
        String sourceDocument = """
            {
                "a": 41,
                "b": {
                    "x": 123,
                    "y": 456
                },
                "c": 43,
                "d": {}
            }
            """;

        byte[] result = FlsDocumentFilter.filter(
            sourceDocument.getBytes(UTF_8),
            FieldPrivileges.FlsRule.of("b.x", "c"),
            FieldMasking.FieldMaskingRule.ALLOW_ALL,
            ImmutableSet.of()
        );

        String expectedDocument = """
            {
                "b": {
                    "x": 123
                },
                "c": 43
            }
            """;

        assertJsonStructurallyEquivalent(expectedDocument, result);
    }

    @Test
    public void filterArrayContainingObject_exclusion() throws Exception {
        String sourceDocument = """
            {
                "a": 41,
                "b": [
                    {"x": 12, "y": 34},
                    {"x": 56, "y": 78}
                ],
                "c": 43
            }
            """;

        byte[] result = FlsDocumentFilter.filter(
            sourceDocument.getBytes(UTF_8),
            FieldPrivileges.FlsRule.of("~b.x"),
            FieldMasking.FieldMaskingRule.ALLOW_ALL,
            ImmutableSet.of()
        );

        String expectedDocument = """
            {
                "a": 41,
                "b": [
                    {"y": 34},
                    {"y": 78}
                ],
                "c": 43
            }
            """;

        assertJsonStructurallyEquivalent(expectedDocument, result);
    }

    @Test
    public void filterArrayContainingObject_inclusion() throws Exception {
        String sourceDocument = """
            {
                "a": 41,
                "b": [
                    {"x": 12, "y": 34},
                    {"x": 56, "y": 78}
                ],
                "c": 43
            }
            """;

        byte[] result = FlsDocumentFilter.filter(
            sourceDocument.getBytes(UTF_8),
            FieldPrivileges.FlsRule.of("b.y", "c"),
            FieldMasking.FieldMaskingRule.ALLOW_ALL,
            ImmutableSet.of()
        );

        String expectedDocument = """
            {
                "b": [
                    {"y": 34},
                    {"y": 78}
                ],
                "c": 43
            }
            """;

        assertJsonStructurallyEquivalent(expectedDocument, result);
    }

    @Test
    public void keepMetadata() throws Exception {
        String sourceDocument = """
            {
                "a": 41,
                "b": 42,
                "c": 43
            }
            """;

        byte[] result = FlsDocumentFilter.filter(
            sourceDocument.getBytes(UTF_8),
            FieldPrivileges.FlsRule.of("~b"),
            FieldMasking.FieldMaskingRule.ALLOW_ALL,
            ImmutableSet.of("b")
        );

        String expectedDocument = """
            {
                "a": 41,
                "b": 42,
                "c": 43
            }
            """;

        assertJsonStructurallyEquivalent(expectedDocument, result);
    }

    @Test
    public void maskSimpleAttribute() throws Exception {
        String sourceDocument = """
            {
                "a": "x",
                "b": "y",
                "c": "z"
            }
            """;

        byte[] result = FlsDocumentFilter.filter(
            sourceDocument.getBytes(UTF_8),
            FieldPrivileges.FlsRule.ALLOW_ALL,
            FieldMasking.FieldMaskingRule.of(FieldMaskingTestHelper.DEFAULT, "b"),
            ImmutableSet.of()
        );

        String expectedDocument = """
            {
                "a": "x",
                "b": "4b694e9cb9ce9e0983fbe4c5df2d464949610f074460adc76bda5a9d0bcc38a5",
                "c": "z"
            }
            """;

        assertJsonStructurallyEquivalent(expectedDocument, result);
    }

    @Test
    public void maskObjectAttribute() throws Exception {
        String sourceDocument = """
            {
                "a": "x",
                "b": {
                   "b1": "y1",
                   "b2": "y2"
                },
                "c": "z"
            }
            """;

        byte[] result = FlsDocumentFilter.filter(
            sourceDocument.getBytes(UTF_8),
            FieldPrivileges.FlsRule.ALLOW_ALL,
            FieldMasking.FieldMaskingRule.of(FieldMaskingTestHelper.DEFAULT, "b.b1"),
            ImmutableSet.of()
        );

        String expectedDocument = """
            {
                "a": "x",
                "b": {
                   "b1": "f16d01664d4270a4f39cdba8c89ac024380b5f249f0fbec1049497bc745cf30f",
                   "b2": "y2"
                },
                "c": "z"
            }
            """;

        assertJsonStructurallyEquivalent(expectedDocument, result);
    }

    private static void assertJsonStructurallyEquivalent(String expected, byte[] actual) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode expectedTree = objectMapper.readTree(expected);
        JsonNode actualTree = objectMapper.readTree(actual);

        Assert.assertEquals("JSON is not structurally equivalent", expectedTree, actualTree);
    }

}
