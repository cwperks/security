/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import org.opensearch.security.DefaultObjectMapper;

public final class SecurityJsonNode {

    private final JsonNode node;

    public SecurityJsonNode(JsonNode node) {
        this.node = node;
    }

    public SecurityJsonNode get(String name) {
        if (isNull(node)) {
            return new SecurityJsonNode(null);
        }

        JsonNode val = node.get(name);
        return new SecurityJsonNode(val);
    }

    public String asString() {
        if (isNull(node)) {
            return null;
        } else {
            return node.asText(null);
        }
    }

    private static boolean isNull(JsonNode node) {
        return node == null || node.isNull();
    }

    public boolean isNull() {
        return isNull(this.node);
    }

    public SecurityJsonNode get(int i) {
        if (isNull(node) || node.getNodeType() != JsonNodeType.ARRAY || i > (node.size() - 1)) {
            return new SecurityJsonNode(null);
        }

        return new SecurityJsonNode(node.get(i));
    }

    public SecurityJsonNode getDotted(String string) {
        SecurityJsonNode tmp = this;
        for (String part : string.split("\\.")) {
            tmp = tmp.get(part);
        }

        return tmp;

    }

    public List<String> asList() {
        if (isNull(node) || node.getNodeType() != JsonNodeType.ARRAY) {
            return null;
        }

        List<String> retVal = new ArrayList<String>();

        for (int i = 0; i < node.size(); i++) {
            retVal.add(node.get(i).asText());
        }

        return Collections.unmodifiableList(retVal);
    }

    public static SecurityJsonNode fromJson(String json) throws IOException {
        return new SecurityJsonNode(DefaultObjectMapper.readTree(json));
    }
}
