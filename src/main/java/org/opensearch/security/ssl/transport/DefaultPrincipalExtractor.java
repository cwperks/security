/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.ssl.transport;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.SpecialPermission;

public class DefaultPrincipalExtractor implements PrincipalExtractor {

    protected final Logger log = LogManager.getLogger(this.getClass());

    @Override
    @SuppressWarnings("removal")
    public String extractPrincipal(final X509Certificate x509Certificate, final Type type) {
        if (x509Certificate == null) {
            return null;
        }

        final SecurityManager sm = System.getSecurityManager();

        if (sm != null) {
            sm.checkPermission(new SpecialPermission());
        }

        String dnString = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                final X500Principal principal = x509Certificate.getSubjectX500Principal();
                return principal.toString();
            }
        });

        // remove whitespaces
        try {
            final LdapName ln = new LdapName(dnString);
            final List<Rdn> rdns = new ArrayList<>(ln.getRdns());
            Collections.reverse(rdns);
            dnString = String.join(",", rdns.stream().map(r -> r.toString()).collect(Collectors.toList()));
        } catch (InvalidNameException e) {
            log.error("Unable to parse: {}", dnString, e);
        }

        if (log.isTraceEnabled()) {
            log.trace("principal: {}", dnString);
        }

        return dnString;
    }

}
