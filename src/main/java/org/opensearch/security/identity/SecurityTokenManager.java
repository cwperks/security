package org.opensearch.security.identity;

import org.opensearch.OpenSearchSecurityException;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.settings.Settings;
import org.opensearch.identity.tokens.AuthToken;
import org.opensearch.identity.tokens.BearerAuthToken;
import org.opensearch.identity.tokens.TokenManager;
import org.opensearch.security.authtoken.jwt.JwtVendor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
        .put("encryption_key", "encryptionKey")
        .build();

    private ClusterService cs;

    public SecurityTokenManager(ClusterService cs) {
        this.cs = cs;
    }

    private JwtVendor jwtVendor = new JwtVendor(DEMO_SETTINGS, Optional.empty());

    @Override
    public AuthToken issueToken(String s) {
        return null;
    }

    @Override
    public AuthToken issueServiceAccountToken(String extensionUniqueId) throws OpenSearchSecurityException {
        try {
            return new BearerAuthToken(jwtVendor.issueServiceAccountToken(cs.getClusterName().value(), extensionUniqueId, null));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
