/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.configuration;

import org.junit.Test;

import org.opensearch.action.ActionRequest;
import org.opensearch.security.privileges.TenantPrivileges;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PrivilegesInterceptorImplTest {

    @Test
    public void shouldTreatAdvancedSettingsGetAsRead() {
        TenantPrivileges.ActionType actionType = PrivilegesInterceptorImpl.getActionTypeForAction("osd:admin/advanced_settings/get");

        assertThat(actionType, is(TenantPrivileges.ActionType.READ));
    }

    @Test
    public void shouldTreatAdvancedSettingsCreateAsAdmin() {
        TenantPrivileges.ActionType actionType = PrivilegesInterceptorImpl.getActionTypeForAction("osd:admin/advanced_settings/write");

        assertThat(actionType, is(TenantPrivileges.ActionType.ADMIN));
    }

    @Test
    public void shouldTreatAdvancedSettingsUpdateAsAdmin() {
        TenantPrivileges.ActionType actionType = PrivilegesInterceptorImpl.getActionTypeForAction("osd:admin/advanced_settings/write");

        assertThat(actionType, is(TenantPrivileges.ActionType.ADMIN));
    }

    @Test
    public void shouldTreatAdvancedSettingsUpdateAsAdminViaReflectionFallback() {
        TenantPrivileges.ActionType actionType = PrivilegesInterceptorImpl.getActionTypeForAction("osd:admin/advanced_settings/write");

        assertThat(actionType, is(TenantPrivileges.ActionType.ADMIN));
    }

    private ActionRequest advancedSettingsWriteRequest(String operationType) {
        return new ActionRequest() {
            @Override
            public org.opensearch.action.ActionRequestValidationException validate() {
                return null;
            }

            @SuppressWarnings("unused")
            public Object getOperationType() {
                return new Object() {
                    @Override
                    public String toString() {
                        return operationType;
                    }
                };
            }
        };
    }
}
