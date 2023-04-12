package org.opensearch.security.identity;

import org.apache.commons.lang3.RandomStringUtils;
import org.greenrobot.eventbus.Subscribe;
import org.opensearch.OpenSearchSecurityException;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.transport.TransportAddress;
import org.opensearch.identity.Principals;
import org.opensearch.identity.TokenManager;
import org.opensearch.identity.noop.NoopToken;
import org.opensearch.identity.tokens.AuthToken;
import org.opensearch.identity.tokens.BearerToken;
import org.opensearch.security.authtoken.jwt.JwtVendor;
import org.opensearch.security.securityconf.ConfigModel;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.user.User;
import org.opensearch.threadpool.ThreadPool;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

public class SecurityTokenManager implements TokenManager {

    private final ThreadPool threadPool;

    private final ClusterService clusterService;

    private ConfigModel configModel;

    public SecurityTokenManager(ThreadPool threadPool, ClusterService clusterService) {
        this.threadPool = threadPool;
        this.clusterService = clusterService;
    }
    @Override
    public AuthToken issueAccessTokenOnBehalfOfAuthenticatedUser(String extensionUniqueId) throws OpenSearchSecurityException {
        final User user = (User) threadPool.getThreadContext().getTransient(ConfigConstants.OPENDISTRO_SECURITY_USER);
        if (user == null) {
            throw new OpenSearchSecurityException("No authenticated user. Cannot issue an auth token.");
        }
        final TransportAddress caller = threadPool.getThreadContext().getTransient(ConfigConstants.OPENDISTRO_SECURITY_REMOTE_ADDRESS);
        Set<String> mappedRoles = mapRoles(user, caller);
        // TODO Extract these from dynamic config settings
        String signingKey = Base64.getEncoder().encodeToString("Secret signing key".getBytes(StandardCharsets.UTF_8));
        String encryptionKey = RandomStringUtils.randomAlphanumeric(16);
        Settings settings = Settings.builder().put("signing_key", signingKey).put("encryption_key", encryptionKey).build();
        JwtVendor jwtVendor = new JwtVendor(settings);

        String signedJwt = jwtVendor.createJwt(clusterService.getClusterName().value(), user.getName(), extensionUniqueId, null, mappedRoles);

        return new BearerToken(signedJwt);
    }

    @Override
    public AuthToken issueRefreshTokenOnBehalfOfAuthenticatedUser(String extensionUniqueId) {
        final User user = (User) threadPool.getThreadContext().getTransient(ConfigConstants.OPENDISTRO_SECURITY_USER);
        if (user == null) {
            throw new OpenSearchSecurityException("No authenticated user. Cannot issue an auth token.");
        }
        final TransportAddress caller = threadPool.getThreadContext().getTransient(ConfigConstants.OPENDISTRO_SECURITY_REMOTE_ADDRESS);
        Set<String> mappedRoles = mapRoles(user, caller);
        // TODO Extract these from dynamic config settings
        String signingKey = Base64.getEncoder().encodeToString("Secret signing key".getBytes(StandardCharsets.UTF_8));
        String encryptionKey = RandomStringUtils.randomAlphanumeric(16);
        Settings settings = Settings.builder().put("signing_key", signingKey).put("encryption_key", encryptionKey).build();
        JwtVendor jwtVendor = new JwtVendor(settings);

        String signedJwt = jwtVendor.createRefreshToken(clusterService.getClusterName().value(), user.getName(), extensionUniqueId, mappedRoles);

        return new BearerToken(signedJwt);
    }

    @Subscribe
    public void onConfigModelChanged(ConfigModel configModel) {
        this.configModel = configModel;
    }

    public Set<String> mapRoles(final User user, final TransportAddress caller) {
        return this.configModel.mapSecurityRoles(user, caller);
    }
}
