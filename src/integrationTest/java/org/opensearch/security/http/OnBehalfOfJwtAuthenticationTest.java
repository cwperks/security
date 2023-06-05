package org.opensearch.security.http;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.message.BasicHeader;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.Client;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.settings.Settings;
import org.opensearch.security.authtoken.jwt.JwtVendor;
import org.opensearch.test.framework.JwtConfigBuilder;
import org.opensearch.test.framework.OnBehalfOfConfig;
import org.opensearch.test.framework.TestSecurityConfig;
import org.opensearch.test.framework.cluster.ClusterManager;
import org.opensearch.test.framework.cluster.LocalCluster;
import org.opensearch.test.framework.cluster.TestRestClient;
import org.opensearch.test.framework.log.LogsRule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.opensearch.action.support.WriteRequest.RefreshPolicy.IMMEDIATE;
import static org.opensearch.client.RequestOptions.DEFAULT;
import static org.opensearch.rest.RestStatus.FORBIDDEN;
import static org.opensearch.security.Song.*;
import static org.opensearch.test.framework.TestSecurityConfig.AuthcDomain.AUTHC_HTTPBASIC_INTERNAL;
import static org.opensearch.test.framework.TestSecurityConfig.AuthcDomain.BASIC_AUTH_DOMAIN_ORDER;
import static org.opensearch.test.framework.TestSecurityConfig.Role.ALL_ACCESS;
import static org.opensearch.test.framework.cluster.SearchRequestFactory.queryStringQueryRequest;
import static org.opensearch.test.framework.matcher.ExceptionMatcherAssert.assertThatThrownBy;
import static org.opensearch.test.framework.matcher.OpenSearchExceptionMatchers.statusException;
import static org.opensearch.test.framework.matcher.SearchResponseMatchers.*;
import static org.opensearch.test.framework.matcher.SearchResponseMatchers.searchHitContainsFieldWithValue;


@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class OnBehalfOfJwtAuthenticationTest {
    public static final String POINTER_USERNAME = "/user_name";
    private static String SIGNING_KEY = Base64.getEncoder().encodeToString("jwt signing key for an on behalf of token authentication backend for testing of extensions".getBytes(StandardCharsets.UTF_8));
    private static String ENCRYPTION_KEY = Base64.getEncoder().encodeToString("encryptionKey".getBytes(StandardCharsets.UTF_8));

    @ClassRule
    public static final LocalCluster cluster = new LocalCluster.Builder()
            .clusterManager(ClusterManager.SINGLENODE).anonymousAuth(false)
            .authc(AUTHC_HTTPBASIC_INTERNAL).onBehalfOf(new OnBehalfOfConfig().signing_key(SIGNING_KEY).encryption_key(ENCRYPTION_KEY))
            .build();

    @Test
    public void shouldAuthenticateWithOnBehalfOfJwtToken_positive() throws Exception {
        String issuer = "cluster_0";
        String subject = "craig";
        String audience = "audience_0";
        List<String> roles = List.of("admin", "HR");
        Integer expirySeconds = 10000;
        LongSupplier currentTime = () -> (System.currentTimeMillis() / 1000);
        Settings settings =  Settings.builder().put("signing_key", SIGNING_KEY).put("encryption_key", ENCRYPTION_KEY).build();

        JwtVendor jwtVendor = new JwtVendor(settings, currentTime);
        String encodedJwt = jwtVendor.createJwt(issuer, subject, audience, expirySeconds, roles);

        try(TestRestClient client = cluster.getRestClient(new BasicHeader("Authorization", "Bearer " + encodedJwt))){

            TestRestClient.HttpResponse response = client.getAuthInfo();

            response.assertStatusCode(200);
            String username = response.getTextFromJsonBody(POINTER_USERNAME);
            assertThat("craig", equalTo(username));
        }
    }
}
