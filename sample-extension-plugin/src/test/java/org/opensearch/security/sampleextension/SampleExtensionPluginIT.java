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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode indexResponseNode = objectMapper.readTree(response2.getEntity().getContent());
        // Regular expression to capture the value of "id"
        Pattern pattern = Pattern.compile("id=([a-zA-Z0-9_-]+)");
        Matcher matcher = pattern.matcher(indexResponseNode.get("message").asText());

        String resourceId = "";
        if (matcher.find()) {
            resourceId = matcher.group(1); // Extract the ID
            System.out.println("Extracted ID: " + resourceId);
        } else {
            System.out.println("ID not found.");
        }
        System.out.println("indexResponseNode: " + indexResponseNode);
        Map<String, String> createResourceResponse2 = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            response2.getEntity().getContent()
        ).mapStrings();
        System.out.println("createResourceResponse2: " + createResourceResponse2);

        // Sleep to give ResourceSharingListener time to create the .resource-sharing index
        Thread.sleep(1000);

        // TODO Expose this via API to update sharing
        // try {
        // // Create the request
        // Request request = new Request("POST", "/.resource-sharing/_update_by_query");
        //
        // // Build the request body using XContentBuilder
        // XContentBuilder builder = XContentFactory.jsonBuilder();
        // builder.startObject();
        // {
        // builder.startObject("query");
        // {
        // builder.startObject("term");
        // {
        // builder.field("resource_user", "craig");
        // }
        // builder.endObject();
        // }
        // builder.endObject();
        //
        // builder.startObject("script");
        // {
        // builder.field("source", "ctx._source.share_with.users = ['admin']");
        // builder.field("lang", "painless");
        // }
        // builder.endObject();
        // }
        // builder.endObject();
        //
        // // Set the request body
        // request.setJsonEntity(builder.toString());
        //
        // System.out.println("updateByQueryRequest: " + request);
        //
        // // Execute the request using the admin client
        // Response updateByQueryResponse = adminClient().performRequest(request);
        //
        // // Handle the response
        // System.out.println("Update request executed successfully. Status: " + response.getStatusLine().getStatusCode());
        // System.out.println("updateByQueryResponse: " + updateByQueryResponse.toString());
        //
        // } catch (IOException e) {
        // // Handle the exception (e.g., log it or throw a custom exception)
        // System.err.println("Error executing update request: " + e.getMessage());
        // }

        Request listRequest = new Request("GET", "/_plugins/resource_sharing_example/resource");
        listRequest.setOptions(requestOptions);
        Response listResponse = client().performRequest(listRequest);
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

        Request updateSharingRequest = new Request("PUT", "/_plugins/resource_sharing_example/resource/update_sharing/" + resourceId);
        updateSharingRequest.setEntity(new StringEntity("{\"share_with\":{\"users\": [\"admin\"], \"backend_roles\": []}}"));
        requestOptions.addHeader("Content-Type", "application/json");
        updateSharingRequest.setOptions(requestOptions);
        Response updateResponse = client().performRequest(updateSharingRequest);
        Map<String, String> updateSharingResponse = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            updateResponse.getEntity().getContent()
        ).mapStrings();
        System.out.println("updateSharingResponse: " + updateSharingResponse);

        Thread.sleep(1000);

        resourceSharingResponse = adminClient().performRequest(resourceSharingRequest);
        resourceSharingResponseMap = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            resourceSharingResponse.getEntity().getContent()
        ).map();
        System.out.println("resourceSharingResponse after update: " + resourceSharingResponseMap);

        listResponse = client().performRequest(listRequest);
        listResourceResponse = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            listResponse.getEntity().getContent()
        ).map();
        System.out.println("listResourceResponse after update: " + listResourceResponse);
    }
}
