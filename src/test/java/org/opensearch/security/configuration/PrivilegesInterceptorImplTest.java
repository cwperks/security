/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.configuration;

import java.util.Map;

import org.junit.Test;

import org.opensearch.action.ActionRequest;
import org.opensearch.dashboards.action.WriteAdvancedSettingsRequest;
import org.opensearch.security.privileges.TenantPrivileges;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

public class PrivilegesInterceptorImplTest {

    @Test
    public void shouldTreatAdvancedSettingsGetAsRead() {
        TenantPrivileges.ActionType actionType = PrivilegesInterceptorImpl.getActionTypeForAction(
            "osd:admin/advanced_settings/get",
            mock(ActionRequest.class)
        );

        assertThat(actionType, is(TenantPrivileges.ActionType.READ));
    }

    @Test
    public void shouldTreatAdvancedSettingsCreateAsRead() {
        WriteAdvancedSettingsRequest request = new WriteAdvancedSettingsRequest(
            ".kibana_1",
            "config:3.0.0",
            Map.of("type", "config", "config", Map.of("buildNum", 1)),
            WriteAdvancedSettingsRequest.OperationType.CREATE
        );

        TenantPrivileges.ActionType actionType = PrivilegesInterceptorImpl.getActionTypeForAction(
            "osd:admin/advanced_settings/write",
            request
        );

        assertThat(actionType, is(TenantPrivileges.ActionType.READ));
    }

    @Test
    public void shouldTreatAdvancedSettingsUpdateAsAdmin() {
        WriteAdvancedSettingsRequest request = new WriteAdvancedSettingsRequest(
            ".kibana_1",
            "config:3.0.0",
            Map.of("type", "config", "config", Map.of("buildNum", 1)),
            WriteAdvancedSettingsRequest.OperationType.UPDATE
        );

        TenantPrivileges.ActionType actionType = PrivilegesInterceptorImpl.getActionTypeForAction(
            "osd:admin/advanced_settings/write",
            request
        );

        assertThat(actionType, is(TenantPrivileges.ActionType.ADMIN));
    }

    @Test
    public void shouldTreatAdvancedSettingsUpdateAsAdminViaReflectionFallback() {
        ActionRequest request = new ActionRequest() {
            @Override
            public org.opensearch.action.ActionRequestValidationException validate() {
                return null;
            }

            @SuppressWarnings("unused")
            public Object getOperationType() {
                return new Object() {
                    @Override
                    public String toString() {
                        return "UPDATE";
                    }
                };
            }
        };

        TenantPrivileges.ActionType actionType = PrivilegesInterceptorImpl.getActionTypeForAction(
            "osd:admin/advanced_settings/write",
            request
        );

        assertThat(actionType, is(TenantPrivileges.ActionType.ADMIN));
    }
}
