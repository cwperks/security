package org.opensearch.security.identity;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionType;
import org.opensearch.client.Client;
import org.opensearch.client.FilterClient;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.action.ActionResponse;
import org.opensearch.identity.NamedPrincipal;
import org.opensearch.plugins.Plugin;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.user.User;

public class RunAsClient extends FilterClient {
    private final NamedPrincipal pluginPrincipal;
    private final User pluginUser;

    public RunAsClient(Client delegate, Plugin plugin) {
        super(delegate);
        String principal = "plugin:" + plugin.getClass().getCanonicalName();
        this.pluginPrincipal = new NamedPrincipal(principal);
        // Convention for plugin username. Prefixed with 'plugin:'. ':' is forbidden from usernames, so this
        // guarantees that a user with this username cannot be created by other means.
        this.pluginUser = new User(principal);
    }

    public NamedPrincipal getPrincipal() {
        return pluginPrincipal;
    }

    @Override
    protected <Request extends ActionRequest, Response extends ActionResponse> void doExecute(
        ActionType<Response> action,
        Request request,
        ActionListener<Response> actionListener
    ) {
        ThreadContext threadContext = threadPool().getThreadContext();

        try (ThreadContext.StoredContext ctx = threadContext.stashContext()) {

            ActionListener<Response> wrappedListener = ActionListener.wrap(r -> {
                ctx.restore();
                actionListener.onResponse(r);
            }, e -> {
                ctx.restore();
                actionListener.onFailure(e);
            });

            threadContext.putTransient(ConfigConstants.OPENDISTRO_SECURITY_USER, pluginUser);
            super.doExecute(action, request, wrappedListener);
        }
    }
}
