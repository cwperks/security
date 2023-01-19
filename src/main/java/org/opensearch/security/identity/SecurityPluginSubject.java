package org.opensearch.security.identity;

import org.opensearch.authn.StringPrincipal;
import org.opensearch.authn.Subject;
import org.opensearch.authn.tokens.AuthenticationToken;

import java.security.Principal;

public class SecurityPluginSubject implements Subject {

    private StringPrincipal name;
    /**
     * Create a new authenticated user without attributes
     *
     * @param name The username (must not be null or empty)
     */
    public SecurityPluginSubject(final String name) {
        this.name = new StringPrincipal(name);
    }

    @Override
    public Principal getPrincipal() {
        return name;
    }

    @Override
    public void login(AuthenticationToken authenticationToken) {

    }
}
