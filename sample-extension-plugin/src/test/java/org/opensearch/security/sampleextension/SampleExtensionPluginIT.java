/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.sampleextension;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.Assert;

import org.opensearch.client.Request;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.Response;
import org.opensearch.common.xcontent.LoggingDeprecationHandler;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.core.xcontent.NamedXContentRegistry;

public class SampleExtensionPluginIT extends ODFERestTestCase {

    @SuppressWarnings("unchecked")
    public void testPluginsAreInstalled() throws IOException {
        Request request = new Request("GET", "/_cat/plugins?s=component&h=name,component,version,description&format=json");
        Response response = client().performRequest(request);
        List<Object> pluginsList = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            response.getEntity().getContent()
        ).list();
        Assert.assertTrue(
            pluginsList.stream().map(o -> (Map<String, Object>) o).anyMatch(plugin -> plugin.get("component").equals("opensearch-security"))
        );
        Assert.assertTrue(
            pluginsList.stream()
                .map(o -> (Map<String, Object>) o)
                .anyMatch(plugin -> plugin.get("component").equals("opensearch-security-sample-extension"))
        );
    }

    public void testCreateSampleResource() throws IOException {
        Request createRequest = new Request("POST", "/_plugins/resource_sharing_example/resource");
        createRequest.setEntity(new StringEntity("{\"name\":\"Craig\"}"));
        RequestOptions.Builder requestOptions = RequestOptions.DEFAULT.toBuilder();
        requestOptions.setWarningsHandler((warnings) -> false);
        createRequest.setOptions(requestOptions);
        Response response = client().performRequest(createRequest);
        Map<String, String> createResourceResponse = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            response.getEntity().getContent()
        ).mapStrings();
        System.out.println("createResourceResponse: " + createResourceResponse);

        Request listRequest = new Request("GET", "/_plugins/resource_sharing_example/resource");
        RequestOptions.Builder listRequestOptions = RequestOptions.DEFAULT.toBuilder();
        requestOptions.setWarningsHandler((warnings) -> false);
        listRequest.setOptions(listRequestOptions);
        Response listResponse = client().performRequest(listRequest);
        Map<String, String> listResourceResponse = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            listResponse.getEntity().getContent()
        ).mapStrings();
        System.out.println("listResourceResponse: " + listResourceResponse);
    }
}
