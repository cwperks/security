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

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;

import com.google.common.base.Strings;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.KeyType;
import org.apache.cxf.rs.security.jose.jwk.PublicKeyUse;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JoseJwtProducer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.jose.jwt.JwtUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.OpenSearchSecurityException;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.transport.TransportAddress;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.security.securityconf.ConfigModel;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.user.User;
import org.opensearch.threadpool.ThreadPool;

public class JwtVendor {
    private static final Logger logger = LogManager.getLogger(JwtVendor.class);

    private static JsonMapObjectReaderWriter jsonMapReaderWriter = new JsonMapObjectReaderWriter();

    private String claimsEncryptionKey;
    private JsonWebKey signingKey;
    private JoseJwtProducer jwtProducer;
    private final LongSupplier timeProvider;

    //TODO: Relocate/Remove them at once we make the descisions about the `roles`
    private ConfigModel configModel;
    private ThreadContext threadContext;

    public JwtVendor(Settings settings) {
        JoseJwtProducer jwtProducer = new JoseJwtProducer();
        try {
            this.signingKey = createJwkFromSettings(settings);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.jwtProducer = jwtProducer;
        if (settings.get("encryption_key") == null) {
            throw new RuntimeException("encryption_key cannot be null");
        } else {
            this.claimsEncryptionKey = settings.get("encryption_key");
        }
        timeProvider = System::currentTimeMillis;
    }

    //For testing the expiration in the future
    public JwtVendor(Settings settings, final LongSupplier timeProvider) {
        JoseJwtProducer jwtProducer = new JoseJwtProducer();
        try {
            this.signingKey = createJwkFromSettings(settings);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.jwtProducer = jwtProducer;
        if (settings.get("encryption_key") == null) {
            throw new RuntimeException("encryption_key cannot be null");
        } else {
            this.claimsEncryptionKey = settings.get("encryption_key");
        }
        this.timeProvider = timeProvider;
    }

    /*
    * The default configuration of this web key should be:
    *   KeyType: OCTET
    *   PublicKeyUse: SIGN
    *   Encryption Algorithm: HS512
    * */
    static JsonWebKey createJwkFromSettings(Settings settings) throws Exception {
        String signingKey = settings.get("signing_key");

        if (!Strings.isNullOrEmpty(signingKey)) {

            JsonWebKey jwk = new JsonWebKey();

            jwk.setKeyType(KeyType.OCTET);
            jwk.setAlgorithm("HS512");
            jwk.setPublicKeyUse(PublicKeyUse.SIGN);
            jwk.setProperty("k", signingKey);

            return jwk;
        } else {
            Settings jwkSettings = settings.getAsSettings("jwt").getAsSettings("key");

            if (jwkSettings.isEmpty()) {
                throw new Exception(
                        "Settings for key is missing. Please specify at least the option signing_key with a shared secret.");
            }

            JsonWebKey jwk = new JsonWebKey();

            for (String key : jwkSettings.keySet()) {
                jwk.setProperty(key, jwkSettings.get(key));
            }

            return jwk;
        }
    }

    public Set<String> mapRoles(final User user, final TransportAddress caller) {
        return this.configModel.mapSecurityRoles(user, caller);
    }

    public String createJwt(String issuer, String subject, String audience, Integer expirySeconds, Collection<String> roles) throws OpenSearchSecurityException {
        long timeMillis = timeProvider.getAsLong();
        Instant now = Instant.ofEpochMilli(timeProvider.getAsLong());

        jwtProducer.setSignatureProvider(JwsUtils.getSignatureProvider(signingKey));
        JwtClaims jwtClaims = new JwtClaims();
        JwtToken jwt = new JwtToken(jwtClaims);

        jwtClaims.setIssuer(issuer);

        jwtClaims.setIssuedAt(timeMillis);

        jwtClaims.setSubject(subject);

        jwtClaims.setAudience(audience);

        jwtClaims.setNotBefore(timeMillis);

        if (expirySeconds == null) {
            long expiryTime = timeProvider.getAsLong() + (300 * 1000);
            jwtClaims.setExpiryTime(expiryTime);
        } else if (expirySeconds > 0) {
            long expiryTime = timeProvider.getAsLong() + (expirySeconds * 1000);
            jwtClaims.setExpiryTime(expiryTime);
        } else {
            throw new OpenSearchSecurityException("The expiration time should be a positive integer");
        }

        //TODO: IF USER ENABLES THE BWC MODE, WE ARE EXPECTING TO SET PLAIN TEXT ROLE AS `dr`
        if (roles != null) {
            String listOfRoles = String.join(",", roles);
            jwtClaims.setProperty("er", EncryptionDecryptionUtil.encrypt(claimsEncryptionKey, listOfRoles));
        } else {
            throw new OpenSearchSecurityException("Roles cannot be null");
        }

        String encodedJwt = jwtProducer.processJwt(jwt);

        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Created JWT: "
                            + encodedJwt
                            + "\n"
                            + jsonMapReaderWriter.toJson(jwt.getJwsHeaders())
                            + "\n"
                            + JwtUtils.claimsToJson(jwt.getClaims())
            );
        }

        return encodedJwt;
    }
}
