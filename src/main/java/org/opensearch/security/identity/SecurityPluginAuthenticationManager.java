package org.opensearch.security.identity;

import org.opensearch.authn.AccessTokenManager;
import org.opensearch.authn.AuthenticationManager;
import org.opensearch.authn.Subject;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.user.User;
import org.opensearch.threadpool.ThreadPool;

public class SecurityPluginAuthenticationManager implements AuthenticationManager {

    private ThreadPool threadPool;
    @Override
    public Subject getSubject() {
        final User user = getThreadContext().getTransient(ConfigConstants.OPENDISTRO_SECURITY_USER);
        SecurityPluginSubject sub = new SecurityPluginSubject(user.getName());
        return sub;
    }

    @Override
    public AccessTokenManager getAccessTokenManager() {
        return null;
    }

    public void setThreadPool(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    private ThreadContext getThreadContext() {
        return threadPool.getThreadContext();
    }
}
