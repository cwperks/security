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

package org.opensearch.security.http;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.security.WeakKeyException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.OpenSearchSecurityException;
import org.opensearch.SpecialPermission;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.rest.RestChannel;
import org.opensearch.rest.RestRequest;
import org.opensearch.security.auth.HTTPAuthenticator;
import org.opensearch.security.authtoken.jwt.EncryptionDecryptionUtil;
import org.opensearch.security.user.AuthCredentials;
import org.opensearch.security.util.KeyUtil;

public class OnBehalfOfAuthenticator implements HTTPAuthenticator {

    protected final Logger log = LogManager.getLogger(this.getClass());

    private static final Pattern BEARER = Pattern.compile("^\\s*Bearer\\s.*", Pattern.CASE_INSENSITIVE);
    private static final String BEARER_PREFIX = "bearer ";
    private static final String SUBJECT_CLAIM = "sub";

    private final JwtParser jwtParser;
    private final String encryptionKey;

    public OnBehalfOfAuthenticator(Settings settings) {
        encryptionKey = settings.get("encryption_key");
        jwtParser = initParser(settings.get("signing_key"));
    }

    private JwtParser initParser(final String signingKey) {
        JwtParser _jwtParser = KeyUtil.keyAlgorithmCheck(signingKey, log);
        if (_jwtParser != null) {
            return _jwtParser;
        } else {
            throw new RuntimeException("Unable to find on behalf of authenticator signing key");
        }
    }

    private List<String> extractSecurityRolesFromClaims(Claims claims) {
        Object rolesObject = ObjectUtils.firstNonNull(claims.get("er"), claims.get("dr"));
        List<String> roles;

        if (rolesObject == null) {
            log.warn("This is a malformed On-behalf-of Token");
            roles = List.of();
        } else {
            final String rolesClaim = rolesObject.toString();

            // Extracting roles based on the compatbility mode
            String decryptedRoles = rolesClaim;
            if (rolesObject == claims.get("er")) {
                decryptedRoles = EncryptionDecryptionUtil.decrypt(encryptionKey, rolesClaim);
            }
            roles = Arrays.stream(decryptedRoles.split(",")).map(String::trim).collect(Collectors.toList());
        }

        return roles;
    }

    private String[] extractBackendRolesFromClaims(Claims claims) {
        // Object backendRolesObject = ObjectUtils.firstNonNull(claims.get("ebr"), claims.get("dbr"));
        if (!claims.containsKey("dbr")) {
            return null;
        }

        Object backendRolesObject = claims.get("dbr");
        String[] backendRoles;

        if (backendRolesObject == null) {
            log.warn("This is a malformed On-behalf-of Token");
            backendRoles = new String[0];
        } else {
            final String backendRolesClaim = backendRolesObject.toString();

            // Extracting roles based on the compatibility mode
            String decryptedBackendRoles = backendRolesClaim;
            backendRoles = Arrays.stream(decryptedBackendRoles.split(",")).map(String::trim).toArray(String[]::new);
        }

        return backendRoles;
    }

    @Override
    @SuppressWarnings("removal")
    public AuthCredentials extractCredentials(RestRequest request, ThreadContext context) throws OpenSearchSecurityException {
        final SecurityManager sm = System.getSecurityManager();

        if (sm != null) {
            sm.checkPermission(new SpecialPermission());
        }

        AuthCredentials creds = AccessController.doPrivileged(new PrivilegedAction<AuthCredentials>() {
            @Override
            public AuthCredentials run() {
                return extractCredentials0(request);
            }
        });

        return creds;
    }

    private AuthCredentials extractCredentials0(final RestRequest request) {
        if (jwtParser == null) {
            log.error("Missing Signing Key. JWT authentication will not work");
            return null;
        }

        String jwtToken = request.header(HttpHeaders.AUTHORIZATION);

        if (jwtToken == null || jwtToken.length() == 0) {
            if (log.isDebugEnabled()) {
                log.debug("No JWT token found in '{}' header", HttpHeaders.AUTHORIZATION);
            }
            return null;
        }

        if (!BEARER.matcher(jwtToken).matches()) {
            jwtToken = null;
        }

        if (jwtToken != null && Pattern.compile(BEARER_PREFIX).matcher(jwtToken.toLowerCase()).find()) {
            jwtToken = jwtToken.substring(jwtToken.toLowerCase().indexOf(BEARER_PREFIX) + BEARER_PREFIX.length());
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No Bearer scheme found in header");
            }
        }

        if (jwtToken == null) {
            return null;
        }

        try {
            final Claims claims = jwtParser.parseClaimsJws(jwtToken).getBody();

            final String subject = claims.getSubject();
            if (Objects.isNull(subject)) {
                log.error("Valid jwt on behalf of token with no subject");
                return null;
            }

            if (claims.get("typ") != null && "sat".equals(claims.get("typ"))) {
                System.out.println("Received Service Account Token for " + claims.getSubject());

                final AuthCredentials ac = new AuthCredentials(subject, List.of()).markComplete();

                return ac;
            }

            final String audience = claims.getAudience();
            if (Objects.isNull(audience)) {
                log.error("Valid jwt on behalf of token with no audience");
                return null;
            }

            List<String> roles = extractSecurityRolesFromClaims(claims);
            String[] backendRoles = extractBackendRolesFromClaims(claims);

            final AuthCredentials ac = new AuthCredentials(subject, roles, backendRoles).markComplete();

            for (Entry<String, Object> claim : claims.entrySet()) {
                ac.addAttribute("attr.jwt." + claim.getKey(), String.valueOf(claim.getValue()));
            }

            return ac;

        } catch (WeakKeyException e) {
            log.error("Cannot authenticate user with JWT because of ", e);
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            if (log.isDebugEnabled()) {
                log.debug("Invalid or expired JWT token.", e);
            }
            return null;
        }
    }

    @Override
    public boolean reRequestAuthentication(final RestChannel channel, AuthCredentials creds) {
        return false;
    }

    @Override
    public String getType() {
        return "onbehalfof_jwt";
    }

}
