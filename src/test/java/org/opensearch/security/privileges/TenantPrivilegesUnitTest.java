/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.privileges;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import org.opensearch.common.settings.Settings;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.security.privileges.actionlevel.RoleBasedActionPrivileges;
import org.opensearch.security.privileges.dlsfls.FieldMasking;
import org.opensearch.security.securityconf.FlattenedActionGroups;
import org.opensearch.security.securityconf.impl.CType;
import org.opensearch.security.securityconf.impl.SecurityDynamicConfiguration;
import org.opensearch.security.securityconf.impl.v7.RoleV7;
import org.opensearch.security.securityconf.impl.v7.TenantV7;
import org.opensearch.security.user.User;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TenantPrivilegesUnitTest {

    @Test
    public void shouldNotTreatKibanaOnlyWriteAsTenantAdminForAdvancedSettings() throws Exception {
        FlattenedActionGroups actionGroups = new FlattenedActionGroups(SecurityDynamicConfiguration.fromYaml("""
            kibana_all_write:
              allowed_actions:
                - "kibana:saved_objects/*/write"
                - "osd:admin/advanced_settings/get"
                - "osd:admin/advanced_settings/write"
            kibana_only_write:
              allowed_actions:
                - "kibana:saved_objects/*/write"
                - "osd:admin/advanced_settings/get"
            """, CType.ACTIONGROUPS));

        SecurityDynamicConfiguration<RoleV7> rolesConfig = SecurityDynamicConfiguration.fromYaml("""
            kibana_all_write:
              tenant_permissions:
                - tenant_patterns:
                    - "*"
                  allowed_actions:
                    - "kibana_only_write"
            """, CType.ROLES);

        SecurityDynamicConfiguration<TenantV7> tenantsConfig = SecurityDynamicConfiguration.fromYaml("""
            admin_tenant: {}
            """, CType.TENANTS);

        TenantPrivileges tenantPrivileges = new TenantPrivileges(rolesConfig, tenantsConfig, actionGroups);

        PrivilegesEvaluationContext context = new PrivilegesEvaluationContext(
            new User("craig"),
            ImmutableSet.of("kibana_all_write"),
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        assertThat(tenantPrivileges.hasTenantPrivilege(context, "admin_tenant", TenantPrivileges.ActionType.READ), is(true));
        assertThat(tenantPrivileges.hasTenantPrivilege(context, "admin_tenant", TenantPrivileges.ActionType.WRITE), is(true));
        assertThat(tenantPrivileges.hasTenantPrivilege(context, "admin_tenant", TenantPrivileges.ActionType.ADMIN), is(false));

        RoleBasedActionPrivileges actionPrivileges = new RoleBasedActionPrivileges(
            new CompiledRoles(rolesConfig, actionGroups, NamedXContentRegistry.EMPTY, FieldMasking.Config.DEFAULT),
            Settings.EMPTY
        );

        assertThat(actionPrivileges.hasClusterPrivilege(context, "osd:admin/advanced_settings/get").isAllowed(), is(false));
        assertThat(actionPrivileges.hasClusterPrivilege(context, "osd:admin/advanced_settings/write").isAllowed(), is(false));
    }
}
