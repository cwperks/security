/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.security.dlic.rest.api;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import org.opensearch.common.settings.Settings;
import org.opensearch.security.auditlog.integration.TestAuditlogImpl;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.test.helper.rest.RestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.opensearch.security.OpenSearchSecurityPlugin.PLUGINS_PREFIX;

public class TenantInfoActionTest extends AbstractRestApiUnitTest {
    private String payload = "{\"hosts\":[],\"users\":[\"sarek\"]," +
            "\"backend_roles\":[\"starfleet*\",\"ambassador\"],\"and_backend_roles\":[],\"description\":\"Migrated " +
            "from v6\"}";
    private final String BASE_ENDPOINT;
    private final String ENDPOINT; 
    protected String getEndpointPrefix() {
        return PLUGINS_PREFIX;
    }

    public TenantInfoActionTest(){
        BASE_ENDPOINT = getEndpointPrefix();
        ENDPOINT = getEndpointPrefix() + "/tenantinfo";
    }

    @Test
    public void testTenantInfoAPIAccess() throws Exception {
        Settings settings = Settings.builder().put(ConfigConstants.SECURITY_UNSUPPORTED_RESTAPI_ALLOW_SECURITYCONFIG_MODIFICATION, true).build();
        setup(settings);

        rh.keystore = "restapi/kirk-keystore.jks";
        rh.sendAdminCertificate = true;
        TestAuditlogImpl.doThenWaitForMessages(() -> {
            RestHelper.HttpResponse response = rh.executeGetRequest(ENDPOINT);
            Assert.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        }, 0);

        rh.sendAdminCertificate = false;
        TestAuditlogImpl.doThenWaitForMessages(() -> {
            RestHelper.HttpResponse response = rh.executeGetRequest(ENDPOINT);
            Assert.assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusCode());
        }, 0);

        rh.sendHTTPClientCredentials = true;
        TestAuditlogImpl.doThenWaitForMessages(() -> {
            RestHelper.HttpResponse response = rh.executeGetRequest(ENDPOINT);
            Assert.assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusCode());
        }, 0);
    }

    @Test
    public void testTenantInfoAPIUpdate() throws Exception {
        Settings settings = Settings.builder().put(ConfigConstants.SECURITY_UNSUPPORTED_RESTAPI_ALLOW_SECURITYCONFIG_MODIFICATION, true).build();
        setup(settings);
        rh.keystore = "restapi/kirk-keystore.jks";
        rh.sendHTTPClientCredentials = true;
        rh.sendAdminCertificate = true;

        //update security config
        TestAuditlogImpl.doThenWaitForMessages(() -> {
            RestHelper.HttpResponse response = rh.executePatchRequest(BASE_ENDPOINT + "/api/securityconfig", "[{\"op\": \"add\",\"path\": \"/config/dynamic/kibana/opendistro_role\"," +
                    "\"value\": \"opendistro_security_internal\"}]", new Header[0]);
            Assert.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        }, 0);

        TestAuditlogImpl.doThenWaitForMessages(() -> {
            RestHelper.HttpResponse response = rh.executePutRequest(BASE_ENDPOINT + "/api/rolesmapping/opendistro_security_internal", payload, new Header[0]);
            Assert.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        }, 0);

        rh.sendAdminCertificate = false;
        TestAuditlogImpl.doThenWaitForMessages(() -> {
            RestHelper.HttpResponse response = rh.executeGetRequest(ENDPOINT);
            Assert.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        }, 0);
    }
}
