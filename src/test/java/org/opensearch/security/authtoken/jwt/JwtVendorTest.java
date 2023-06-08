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

package org.opensearch.security.authtoken.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.junit.Assert;
import org.junit.Test;

import org.opensearch.common.settings.Settings;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JwtVendorTest {

    @Test
    public void testCreateJwkFromSettings() throws Exception {
        Settings settings = Settings.builder().put("signing_key", "abc123").build();

        JsonWebKey jwk = JwtVendor.createJwkFromSettings(settings);
        Assert.assertEquals("HS512", jwk.getAlgorithm());
        Assert.assertEquals("sig", jwk.getPublicKeyUse().toString());
        Assert.assertEquals("abc123", jwk.getProperty("k"));
    }

    @Test(expected = Exception.class)
    public void testCreateJwkFromSettingsWithoutSigningKey() throws Exception {
        Settings settings = Settings.builder().put("jwt", "").build();
        JwtVendor.createJwkFromSettings(settings);
    }

    @Test
    public void testCreateJwtWithRoles() throws Exception {
        String issuer = "cluster_0";
        String subject = "admin";
        String audience = "audience_0";
        List<String> roles = List.of("IT", "HR");
        String expectedRoles = "IT,HR";
        Integer expirySeconds = 300;
        LongSupplier currentTime = () -> (int) 100;
        String claimsEncryptionKey = RandomStringUtils.randomAlphanumeric(16);
        Settings settings = Settings.builder().put("signing_key", "abc123").put("encryption_key", claimsEncryptionKey).build();
        Long expectedExp = currentTime.getAsLong() + (expirySeconds * 1000);

        JwtVendor jwtVendor = new JwtVendor(settings, Optional.of(currentTime));
        String encodedJwt = jwtVendor.createJwt(issuer, subject, audience, expirySeconds, roles);

        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(encodedJwt);
        JwtToken jwt = jwtConsumer.getJwtToken();

        Assert.assertEquals("cluster_0", jwt.getClaim("iss"));
        Assert.assertEquals("admin", jwt.getClaim("sub"));
        Assert.assertEquals("audience_0", jwt.getClaim("aud"));
        Assert.assertNotNull(jwt.getClaim("iat"));
        Assert.assertNotNull(jwt.getClaim("exp"));
        Assert.assertEquals(expectedExp, jwt.getClaim("exp"));
        Assert.assertNotEquals(expectedRoles, jwt.getClaim("er"));
        Assert.assertEquals(expectedRoles, EncryptionDecryptionUtil.decrypt(claimsEncryptionKey, jwt.getClaim("er").toString()));
    }

    @Test(expected = Exception.class)
    public void testCreateJwtWithBadExpiry() throws Exception {
        String issuer = "cluster_0";
        String subject = "admin";
        String audience = "audience_0";
        List<String> roles = List.of("admin");
        Integer expirySeconds = -300;
        String claimsEncryptionKey = RandomStringUtils.randomAlphanumeric(16);

        Settings settings =  Settings.builder().put("signing_key", "abc123").put("encryption_key", claimsEncryptionKey).build();
        JwtVendor jwtVendor = new JwtVendor(settings, Optional.empty());

        jwtVendor.createJwt(issuer, subject, audience, expirySeconds, roles);
    }

    @Test(expected = Exception.class)
    public void testCreateJwtWithBadEncryptionKey() throws Exception {
        String issuer = "cluster_0";
        String subject = "admin";
        String audience = "audience_0";
        List<String> roles = List.of("admin");
        Integer expirySeconds = 300;

        Settings settings =  Settings.builder().put("signing_key", "abc123").build();
        JwtVendor jwtVendor = new JwtVendor(settings, Optional.empty());

        jwtVendor.createJwt(issuer, subject, audience, expirySeconds, roles);
    }

    @Test(expected = Exception.class)
    public void testCreateJwtWithBadRoles() throws Exception {
        String issuer = "cluster_0";
        String subject = "admin";
        String audience = "audience_0";
        List<String> roles = null;
        Integer expirySecond = 300;
        String claimsEncryptionKey = RandomStringUtils.randomAlphanumeric(16);

        Settings settings = Settings.builder().put("signing_key", "abc123").put("encryption_key", claimsEncryptionKey).build();

        JwtVendor jwtVendor = new JwtVendor(settings, Optional.empty());

        jwtVendor.createJwt(issuer, subject, audience, expirySecond, roles);
    }

    //For Manual Testing
    @Test
    public void testCreateJwtWithRolesAndEncyptionKey() throws Exception {
        String issuer = "cluster_0";
        String subject = "craig";
        String audience = "audience_0";
        List<String> roles = List.of("admin", "HR");
        Integer expirySeconds = 10000;
        LongSupplier currentTime = () -> (System.currentTimeMillis() / 1000);
        String claimsEncryptionKey = RandomStringUtils.randomAlphanumeric(16);
        String signingKey = Base64.getEncoder().encodeToString("jwt signing key for an on behalf of token authentication backend for testing of extensions".getBytes(StandardCharsets.UTF_8));
        String encryptionKey = Base64.getEncoder().encodeToString("encryptionKey".getBytes(StandardCharsets.UTF_8));
        System.out.println("The encryptionkey is:" + encryptionKey);
        Settings settings =  Settings.builder().put("signing_key", signingKey).put("encryption_key", encryptionKey).build();

        JwtVendor jwtVendor = new JwtVendor(settings, currentTime);
        String encodedJwt = jwtVendor.createJwt(issuer, subject, audience, expirySeconds, roles);

        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(encodedJwt);
        JwtToken jwt = jwtConsumer.getJwtToken();

        System.out.println("JWT: " + encodedJwt);

        assertTrue(true);
    }
}