package org.opensearch.security.identity;

import org.opensearch.OpenSearchSecurityException;
import org.opensearch.identity.Principals;
import org.opensearch.identity.TokenManager;
import org.opensearch.identity.noop.NoopToken;
import org.opensearch.identity.tokens.AuthToken;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.user.User;
import org.opensearch.threadpool.ThreadPool;

public class SecurityTokenManager implements TokenManager {

    private final ThreadPool threadPool;

    public SecurityTokenManager(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }
    @Override
    public AuthToken issueAccessTokenOnBehalfOfAuthenticatedUser(String extensionUniqueId) {
        final User user = (User) threadPool.getThreadContext().getTransient(ConfigConstants.OPENDISTRO_SECURITY_USER);
        if (user == null) {
            throw new OpenSearchSecurityException("No authenticated user. Cannot issue an authentication token");
        }
        return null;
    }

    @Override
    public AuthToken issueRefreshTokenOnBehalfOfAuthenticatedUser(String extensionUniqueId) {
        return null;
    }
}
