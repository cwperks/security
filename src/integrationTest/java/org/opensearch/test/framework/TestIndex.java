/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.test.framework;

import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.client.Client;
import org.opensearch.common.settings.Settings;

public class TestIndex {

    private final String name;
    private final Settings settings;

    public TestIndex(String name, Settings settings) {
        this.name = name;
        this.settings = settings;

    }

    public void create(Client client) {
        client.admin().indices().create(new CreateIndexRequest(name).settings(settings)).actionGet();
    }

    public String getName() {
        return name;
    }

    public static Builder name(String name) {
        return new Builder().name(name);
    }

    public static class Builder {
        private String name;
        private Settings.Builder settings = Settings.builder();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder setting(String name, int value) {
            settings.put(name, value);
            return this;
        }

        public Builder shards(int value) {
            settings.put("index.number_of_shards", 5);
            return this;
        }

        public TestIndex build() {
            return new TestIndex(name, settings.build());
        }

    }

}
