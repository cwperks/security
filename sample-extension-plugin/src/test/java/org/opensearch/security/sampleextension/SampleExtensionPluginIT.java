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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.Assert;

import org.opensearch.client.Request;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.Response;
import org.opensearch.common.xcontent.LoggingDeprecationHandler;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.core.xcontent.NamedXContentRegistry;

public class SampleExtensionPluginIT extends ODFERestTestCase {

    // @BeforeClass
    // public static void createTestUsers() throws IOException {
    // Request createUserRequest = new Request("POST", "/_opendistro/_security/api/internalusers/craig");
    // createUserRequest.setJsonEntity("{\"password\":\"changeme\",\"roles\":[\"all_access\"]}");
    // client().performRequest(createUserRequest);
    // }

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

    public void testCreateSampleResource() throws IOException, InterruptedException {
        String strongPassword = "myStrongPassword123!";
        Request createUserRequest = new Request("PUT", "/_opendistro/_security/api/internalusers/craig");
        createUserRequest.setJsonEntity("{\"password\":\"" + strongPassword + "\",\"backend_roles\":[\"admin\"]}");
        client().performRequest(createUserRequest);

        RequestOptions.Builder requestOptions = RequestOptions.DEFAULT.toBuilder();
        requestOptions.setWarningsHandler((warnings) -> false);

        Request createRequest = new Request("POST", "/_plugins/resource_sharing_example/resource");
        createRequest.setEntity(new StringEntity("{\"name\":\"ExampleResource1\"}"));
        createRequest.setOptions(requestOptions);
        Response response = client().performRequest(createRequest);
        Map<String, String> createResourceResponse = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            response.getEntity().getContent()
        ).mapStrings();
        System.out.println("createResourceResponse: " + createResourceResponse);

        Request createRequest2 = new Request("POST", "/_plugins/resource_sharing_example/resource");
        createRequest2.setEntity(new StringEntity("{\"name\":\"ExampleResource2\"}"));
        RequestOptions.Builder requestOptions2 = RequestOptions.DEFAULT.toBuilder();
        requestOptions2.setWarningsHandler((warnings) -> false);
        requestOptions2.addHeader(
            "Authorization",
            "Basic " + Base64.getEncoder().encodeToString(("craig:" + strongPassword).getBytes(StandardCharsets.UTF_8))
        );
        createRequest2.setOptions(requestOptions2);
        Response response2 = client().performRequest(createRequest2);
        Map<String, String> createResourceResponse2 = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            response2.getEntity().getContent()
        ).mapStrings();
        System.out.println("createResourceResponse2: " + createResourceResponse2);

        // Sleep to give ResourceSharingListener time to create the .resource-sharing index
        Thread.sleep(1000);

        Request listRequest = new Request("GET", "/_plugins/resource_sharing_example/resource");
        listRequest.setOptions(requestOptions);
        Response listResponse = client().performRequest(listRequest);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode resNode = objectMapper.readTree(listResponse.getEntity().getContent());
        System.out.println("resNode: " + resNode);
        Map<String, Object> listResourceResponse = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            listResponse.getEntity().getContent()
        ).map();
        System.out.println("listResourceResponse: " + listResourceResponse);

        Request resourceSharingRequest = new Request("POST", "/.resource-sharing/_search");
        resourceSharingRequest.setOptions(requestOptions);
        Response resourceSharingResponse = adminClient().performRequest(resourceSharingRequest);
        Map<String, Object> resourceSharingResponseMap = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            resourceSharingResponse.getEntity().getContent()
        ).map();
        System.out.println("resourceSharingResponse: " + resourceSharingResponseMap);
    }
}
