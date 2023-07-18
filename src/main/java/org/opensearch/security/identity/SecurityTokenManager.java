package org.opensearch.security.identity;

import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.opensearch.OpenSearchSecurityException;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.settings.Settings;
import org.opensearch.identity.Subject;
import org.opensearch.identity.tokens.AuthToken;
import org.opensearch.identity.tokens.BearerAuthToken;
import org.opensearch.identity.tokens.TokenManager;
import org.opensearch.security.authtoken.jwt.JwtVendor;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.user.User;
import org.opensearch.threadpool.ThreadPool;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

public class SecurityTokenManager implements TokenManager {

    public static Settings DEMO_SETTINGS = Settings.builder()
        .put(
            "signing_key",
            Base64.getEncoder()
                .encodeToString(
                    "This is my super secret that no one in the universe will ever be able to guess in a bajillion years".getBytes(
                        StandardCharsets.UTF_8
                    )
                )
        )
        .put("encryption_key",  Base64.getEncoder().encodeToString("encryptionKey".getBytes(StandardCharsets.UTF_8)))
        .build();

    private ClusterService cs;

    private ThreadPool threadPool;

    public SecurityTokenManager(ClusterService cs, ThreadPool threadPool) {
        this.cs = cs;
        this.threadPool = threadPool;
    }

    private JwtVendor jwtVendor = new JwtVendor(DEMO_SETTINGS, Optional.empty());

    @Override
    public AuthToken issueOnBehalfOfToken(Map<String, Object> claims) {
        User user = threadPool.getThreadContext().getTransient(ConfigConstants.OPENDISTRO_SECURITY_USER);
        if (user == null) {
            throw new OpenSearchSecurityException("Cannot issue on behalf of token.");
        }
        if (!claims.containsKey(JwtConstants.CLAIM_AUDIENCE)) {
            throw new OpenSearchSecurityException("Cannot issue on behalf of token without an audience claim.");
        }

        String encodedJwt = null;
        try {
            encodedJwt = jwtVendor.issueOnBehalfOfToken(
                cs.getClusterName().value(),
                user.getName(),
                (String) claims.get(JwtConstants.CLAIM_AUDIENCE),
                null,
                user.getSecurityRoles(),
                user.getRoles()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new BearerAuthToken(encodedJwt);
    }

    @Override
    public AuthToken issueServiceAccountToken(String extensionUniqueId) throws OpenSearchSecurityException {
        try {
            return new BearerAuthToken(jwtVendor.issueServiceAccountToken(cs.getClusterName().value(), extensionUniqueId, null));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Subject authenticateToken(AuthToken authToken) {
        return null;
    }
}
