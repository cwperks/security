/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.auth;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.node.Node;
import org.opensearch.rule.attribute_extractor.AttributeExtractor;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.user.User;
import org.opensearch.threadpool.ThreadPool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class PrincipalExtractorTests {
    private ThreadPool threadPool;
    private Function<User, Set<String>> mappedRolesResolver;
    private PrincipalExtractor extractor;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        threadPool = new ThreadPool(Settings.builder().put(Node.NODE_NAME_SETTING.getKey(), "name").build());
        mappedRolesResolver = mock(Function.class);
        extractor = new PrincipalExtractor(threadPool, mappedRolesResolver);
    }

    @After
    public void tearDown() {
        threadPool.shutdown();
    }

    private User requestUser(String username, String... mappedRoles) {
        User user = new User(username).withRoles("backend_only");
        threadPool.getThreadContext().putTransient(ConfigConstants.OPENDISTRO_SECURITY_USER, user);
        when(mappedRolesResolver.apply(user)).thenReturn(Set.of(mappedRoles));
        return user;
    }

    private Set<String> extractedPrincipals() {
        return StreamSupport.stream(extractor.extract().spliterator(), false).collect(Collectors.toSet());
    }

    @Test
    public void testGetAttribute() {
        assertEquals(PrincipalAttribute.PRINCIPAL, extractor.getAttribute());
    }

    @Test
    public void testExtractWithNoUser() {
        assertTrue(extractedPrincipals().isEmpty());
        verifyNoInteractions(mappedRolesResolver);
    }

    @Test
    public void testExtractWithUser() {
        User user = requestUser("alice", "all_access");
        assertEquals(Set.of("username|alice", "role|all_access"), extractedPrincipals());
        verify(mappedRolesResolver).apply(user);
    }

    @Test
    public void testCombinationStyle() {
        assertEquals(AttributeExtractor.LogicalOperator.OR, extractor.getLogicalOperator());
    }

    @Test
    public void testPreservesPrincipalCharacters() {
        requestUser(" alice\\| ", "team,one", "team|two", "team\\", " role ");
        assertEquals(Set.of("username| alice\\| ", "role|team,one", "role|team|two", "role|team\\", "role| role "), extractedPrincipals());
    }

    @Test
    public void testUsernameEndingInBackslash() {
        requestUser("alice\\", "all_access");
        assertEquals(Set.of("username|alice\\", "role|all_access"), extractedPrincipals());
    }

    @Test
    public void testUsernameWithoutMappedRoles() {
        requestUser("alice");
        assertEquals(Set.of("username|alice"), extractedPrincipals());
    }

    @Test
    public void testLegacyUserInfoIsNotParsed() {
        threadPool.getThreadContext().putTransient(ConfigConstants.OPENDISTRO_SECURITY_USER_INFO_THREAD_CONTEXT, "wrong||wrong_role|");
        assertTrue(extractedPrincipals().isEmpty());
        verifyNoInteractions(mappedRolesResolver);
        requestUser("alice", "all_access");
        assertEquals(Set.of("username|alice", "role|all_access"), extractedPrincipals());
    }

    @Test
    public void testUsesCurrentRequestUser() {
        requestUser("alice", "all_access");
        try (ThreadContext.StoredContext ignored = threadPool.getThreadContext().stashContext()) {
            assertTrue(extractedPrincipals().isEmpty());
            requestUser("bob", "read_only");
            assertEquals(Set.of("username|bob", "role|read_only"), extractedPrincipals());
        }
        assertEquals(Set.of("username|alice", "role|all_access"), extractedPrincipals());
    }

    @Test
    public void testResolvesCurrentMappedRoles() {
        User user = requestUser("alice", "all_access");
        assertEquals(Set.of("username|alice", "role|all_access"), extractedPrincipals());
        when(mappedRolesResolver.apply(user)).thenReturn(Set.of("read_only"));
        assertEquals(Set.of("username|alice", "role|read_only"), extractedPrincipals());
    }

    @Test
    public void testDoesNotAddPrincipalContext() {
        User user = requestUser("alice", "all_access");
        ThreadContext context = threadPool.getThreadContext();
        var headers = context.getHeaders();
        extractedPrincipals();
        assertSame(user, context.getTransient(ConfigConstants.OPENDISTRO_SECURITY_USER));
        assertEquals(headers, context.getHeaders());
        assertNull(context.getTransient("_opendistro_security_principal_info"));
    }
}
