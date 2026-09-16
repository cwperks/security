/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.auth;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.node.Node;
import org.opensearch.rule.attribute_extractor.AttributeExtractor;
import org.opensearch.rule.autotagging.Attribute;
import org.opensearch.security.privileges.PrivilegesConfiguration;
import org.opensearch.security.privileges.PrivilegesEvaluationContext;
import org.opensearch.security.privileges.TenantPrivileges;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.user.ThreadContextUserInfo;
import org.opensearch.security.user.User;
import org.opensearch.threadpool.ThreadPool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PrincipalExtractorTests {

    private ThreadPool threadPool;

    @Before
    public void setup() {
        threadPool = new ThreadPool(Settings.builder().put(Node.NODE_NAME_SETTING.getKey(), "name").build());
    }

    @After
    public void tearDown() {
        threadPool.shutdown();
    }

    private void publishPrincipal(String username, String... mappedRoles) {
        PrivilegesEvaluationContext context = mock(PrivilegesEvaluationContext.class);
        User user = new User(username).withRoles("backend_only");
        when(context.getUser()).thenReturn(user);
        when(context.getMappedRoles()).thenReturn(ImmutableSet.copyOf(mappedRoles));
        PrivilegesConfiguration configuration = mock(PrivilegesConfiguration.class);
        when(configuration.tenantPrivileges()).thenReturn(mock(TenantPrivileges.class));
        new ThreadContextUserInfo(threadPool.getThreadContext(), configuration, null, Settings.EMPTY).setUserInfoInThreadContext(context);
    }

    private Set<String> extractedPrincipals() {
        return StreamSupport.stream(new PrincipalExtractor(threadPool).extract().spliterator(), false)
            .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    public void testGetAttribute() {
        PrincipalExtractor extractor = new PrincipalExtractor(threadPool);
        Attribute attribute = extractor.getAttribute();
        assertEquals(PrincipalAttribute.PRINCIPAL, attribute);
    }

    @Test
    public void testExtractWithNoUser() {
        PrincipalExtractor extractor = new PrincipalExtractor(threadPool);
        assertFalse(extractor.extract().iterator().hasNext());
    }

    @Test
    public void testExtractWithUser() {
        publishPrincipal("alice", "all_access");
        PrincipalExtractor extractor = new PrincipalExtractor(threadPool);
        Iterable<String> principalIter = extractor.extract();
        List<String> principals = StreamSupport.stream(principalIter.spliterator(), false).toList();
        assertTrue(principals.contains("username|alice"));
        assertTrue(principals.contains("role|all_access"));
        assertEquals(2, principals.size());
    }

    @Test
    public void testCombinationStyle() {
        PrincipalExtractor extractor = new PrincipalExtractor(threadPool);
        assertEquals(AttributeExtractor.LogicalOperator.OR, extractor.getLogicalOperator());
    }

    @Test
    public void testPreservesPrincipalCharacters() {
        publishPrincipal(" alice\\| ", "team,one", "team|two", "team\\", " role ");
        assertEquals(Set.of("username| alice\\| ", "role|team,one", "role|team|two", "role|team\\", "role| role "), extractedPrincipals());
    }

    @Test
    public void testUsernameEndingInBackslash() {
        publishPrincipal("alice\\", "all_access");
        assertEquals(Set.of("username|alice\\", "role|all_access"), extractedPrincipals());
    }

    @Test
    public void testUsernameWithoutMappedRoles() {
        publishPrincipal("alice");
        assertEquals(Set.of("username|alice"), extractedPrincipals());
    }

    @Test
    public void testRetainsRequestPrincipalSnapshot() {
        publishPrincipal("alice", "all_access");
        publishPrincipal("bob", "read_only");
        assertEquals(Set.of("username|alice", "role|all_access"), extractedPrincipals());
    }

    @Test
    public void testLegacyUserInfoIsNotParsed() {
        threadPool.getThreadContext().putTransient(ConfigConstants.OPENDISTRO_SECURITY_USER_INFO_THREAD_CONTEXT, "wrong||wrong_role|");
        assertTrue(extractedPrincipals().isEmpty());
        publishPrincipal("alice", "all_access");
        assertEquals(Set.of("username|alice", "role|all_access"), extractedPrincipals());
    }

    @Test
    public void testPrincipalSnapshotIsImmutable() {
        Set<String> roles = new HashSet<>(Set.of("all_access"));
        ThreadContextUserInfo.PrincipalInfo principal = new ThreadContextUserInfo.PrincipalInfo("alice", roles);
        roles.add("another_role");
        assertEquals(Set.of("all_access"), principal.mappedRoles());
        assertThrows(UnsupportedOperationException.class, () -> principal.mappedRoles().add("another_role"));
    }

    @Test
    public void testPrincipalSnapshotFollowsRequestContext() {
        publishPrincipal("alice", "all_access");
        try (ThreadContext.StoredContext ignored = threadPool.getThreadContext().stashContext()) {
            assertTrue(extractedPrincipals().isEmpty());
            publishPrincipal("bob", "read_only");
            assertEquals(Set.of("username|bob", "role|read_only"), extractedPrincipals());
        }
        assertEquals(Set.of("username|alice", "role|all_access"), extractedPrincipals());
    }

    @Test
    public void testPreservesLegacyUserInfoFormat() {
        publishPrincipal("alice", "all_access");
        assertEquals(
            "alice|backend_only|all_access||NONE",
            threadPool.getThreadContext().getTransient(ConfigConstants.OPENDISTRO_SECURITY_USER_INFO_THREAD_CONTEXT)
        );
    }
}
