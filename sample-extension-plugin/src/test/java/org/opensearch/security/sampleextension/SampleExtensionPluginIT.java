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
import java.util.Optional;

import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.Assert;

import org.opensearch.client.Request;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.Response;
import org.opensearch.common.collect.Tuple;
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

    private Map<String, String> createSampleResource(String name, Optional<Tuple<String, String>> credentials) throws IOException {
        RequestOptions.Builder options = RequestOptions.DEFAULT.toBuilder();
        options.setWarningsHandler((warnings) -> false);
        credentials.ifPresent(
            stringStringTuple -> options.addHeader(
                "Authorization",
                "Basic "
                    + Base64.getEncoder()
                        .encodeToString((stringStringTuple.v1() + ":" + stringStringTuple.v2()).getBytes(StandardCharsets.UTF_8))
            )
        );

        Request request = new Request("POST", "/_plugins/resource_sharing_example/resource");
        request.setEntity(new StringEntity("{\"name\":\"" + name + "\"}"));
        request.setOptions(options);
        Response response = client().performRequest(request);
        Map<String, String> createResourceResponse = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            response.getEntity().getContent()
        ).mapStrings();
        System.out.println("createResourceResponse: " + createResourceResponse);
        return createResourceResponse;
    }

    private Map<String, Object> listSampleResource(Optional<Tuple<String, String>> credentials) throws IOException {
        RequestOptions.Builder options = RequestOptions.DEFAULT.toBuilder();
        options.setWarningsHandler((warnings) -> false);
        credentials.ifPresent(
            stringStringTuple -> options.addHeader(
                "Authorization",
                "Basic "
                    + Base64.getEncoder()
                        .encodeToString((stringStringTuple.v1() + ":" + stringStringTuple.v2()).getBytes(StandardCharsets.UTF_8))
            )
        );

        Request listRequest = new Request("GET", "/_plugins/resource_sharing_example/resource");
        listRequest.setOptions(options);
        Response listResponse = client().performRequest(listRequest);
        Map<String, Object> listResourceResponse = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            listResponse.getEntity().getContent()
        ).map();
        System.out.println("listResourceResponse: " + listResourceResponse);
        return listResourceResponse;
    }

    private Map<String, String> updateSharing(String resourceId, String payload, Optional<Tuple<String, String>> credentials)
        throws IOException {
        RequestOptions.Builder options = RequestOptions.DEFAULT.toBuilder();
        options.setWarningsHandler((warnings) -> false);
        credentials.ifPresent(
            stringStringTuple -> options.addHeader(
                "Authorization",
                "Basic "
                    + Base64.getEncoder()
                        .encodeToString((stringStringTuple.v1() + ":" + stringStringTuple.v2()).getBytes(StandardCharsets.UTF_8))
            )
        );
        Request updateSharingRequest = new Request("PUT", "/_plugins/_security/resource/sample_resource/" + resourceId + "/share_with");
        updateSharingRequest.setEntity(new StringEntity(payload));
        options.addHeader("Content-Type", "application/json");
        updateSharingRequest.setOptions(options);
        Response updateResponse = client().performRequest(updateSharingRequest);
        Map<String, String> updateSharingResponse = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            updateResponse.getEntity().getContent()
        ).mapStrings();
        return updateSharingResponse;
    }

    private static Map<String, Object> listSampleResourcesWithAdminClient() throws IOException {
        RequestOptions.Builder options = RequestOptions.DEFAULT.toBuilder();
        options.setWarningsHandler((warnings) -> false);
        Request listRequest = new Request("GET", "/_plugins/resource_sharing_example/resource");
        listRequest.setOptions(options);
        Response listResponse = client().performRequest(listRequest);
        Map<String, Object> listResourceResponse = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            listResponse.getEntity().getContent()
        ).map();
        System.out.println("listResourceResponse: " + listResourceResponse);
        return listResourceResponse;
    }

    public void testCreateSampleResource() throws IOException, InterruptedException {
        RequestOptions.Builder options = RequestOptions.DEFAULT.toBuilder();
        options.setWarningsHandler((warnings) -> false);

        String strongPassword = "myStrongPassword123!";
        Request createUserRequest = new Request("PUT", "/_opendistro/_security/api/internalusers/testuser");
        createUserRequest.setJsonEntity("{\"password\":\"" + strongPassword + "\",\"backend_roles\":[\"admin\"]}");
        client().performRequest(createUserRequest);

        createSampleResource("ExampleResource1", Optional.empty());
        String resourceId = createSampleResource("ExampleResource2", Optional.of(Tuple.tuple("testuser", strongPassword))).get(
            "resourceId"
        );

        // Sleep to give ResourceSharingListener time to create the .resource-sharing index
        Thread.sleep(1000);

        Map<String, Object> listResourceResponse = listSampleResource(Optional.empty());
        System.out.println("listResourceResponse: " + listResourceResponse);

        Map<String, Object> allSampleResources = listSampleResourcesWithAdminClient();
        System.out.println("allSampleResources: " + allSampleResources);

        Map<String, String> updateSharingResponse = updateSharing(
            resourceId,
            "{\"share_with\":{\"users\": [\"admin\"], \"backend_roles\": [], \"allowed_actions\": [\"*\"]}}",
            Optional.of(Tuple.tuple("testuser", strongPassword))
        );
        System.out.println("updateSharingResponse: " + updateSharingResponse);

        Thread.sleep(1000);

        allSampleResources = listSampleResourcesWithAdminClient();
        System.out.println("allSampleResources after update: " + allSampleResources);

        listResourceResponse = listSampleResource(Optional.empty());
        System.out.println("listResourceResponse after update: " + listResourceResponse);
    }
}
