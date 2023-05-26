/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.auth;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.security.auditlog.AuditLog;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.user.User;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.opensearch.security.support.ConfigConstants.OPENDISTRO_SECURITY_INJECTED_ROLES;

public class RolesInjectorTest {

    private TransportRequest transportRequest;
    private Task task;
    private AuditLog auditLog;

    @Before
    public void setup() {
        transportRequest = mock(TransportRequest.class);
        task = mock(Task.class);
        auditLog = mock(AuditLog.class);
    }

    @Test
    public void testNotInjected() {
        ThreadContext threadContext = new ThreadContext(Settings.EMPTY);
        RolesInjector rolesInjector = new RolesInjector(auditLog);
        Set<String> roles = rolesInjector.injectUserAndRoles(transportRequest, "action0", task, threadContext);
        assertEquals(null, roles);
        User user = threadContext.getTransient(ConfigConstants.OPENDISTRO_SECURITY_USER);
        assertEquals(null, user);
    }

    @Test
    public void testInjected() {
        ThreadContext threadContext = new ThreadContext(Settings.EMPTY);
        threadContext.putTransient(OPENDISTRO_SECURITY_INJECTED_ROLES, "user1|role_1,role_2");

        RolesInjector rolesInjector = new RolesInjector(auditLog);
        Set<String> roles = rolesInjector.injectUserAndRoles(transportRequest, "action0", task, threadContext);

        User user = threadContext.getTransient(ConfigConstants.OPENDISTRO_SECURITY_USER);
        assertEquals("user1", user.getName());
        assertEquals(0, user.getRoles().size());
        assertEquals(2, roles.size());
        assertEquals(true, roles.contains("role_1"));
        assertEquals(true, roles.contains("role_2"));
    }

    @Test
    public void testCorruptedInjection() {
        List<String> corruptedStrs = Arrays.asList("invalid", "role_1,role_2", " | ", "  ", "|");

        corruptedStrs.forEach(name -> {
            ThreadContext threadContext = new ThreadContext(Settings.EMPTY);
            threadContext.putTransient(OPENDISTRO_SECURITY_INJECTED_ROLES, name);

            RolesInjector rolesInjector = new RolesInjector(auditLog);
            Set<String> roles = rolesInjector.injectUserAndRoles(transportRequest, "action0", task, threadContext);

            assertEquals(null, roles);
            User user = threadContext.getTransient(ConfigConstants.OPENDISTRO_SECURITY_USER);
            assertEquals(null, user);
        });
    }
}
