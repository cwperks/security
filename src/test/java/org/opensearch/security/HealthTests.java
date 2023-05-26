/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security;

import org.apache.hc.core5.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import org.opensearch.common.settings.Settings;
import org.opensearch.security.test.DynamicSecurityConfig;
import org.opensearch.security.test.SingleClusterTest;
import org.opensearch.security.test.helper.rest.RestHelper;
import org.opensearch.security.test.helper.rest.RestHelper.HttpResponse;

public class HealthTests extends SingleClusterTest {

    @Test
    public void testHealth() throws Exception {
        setup(Settings.EMPTY, new DynamicSecurityConfig(), Settings.EMPTY);

        RestHelper rh = nonSslRestHelper();
        HttpResponse res;
        Assert.assertEquals(
            HttpStatus.SC_OK,
            (res = rh.executeGetRequest("_opendistro/_security/health?pretty&mode=lenient")).getStatusCode()
        );
        System.out.println(res.getBody());
        assertContains(res, "*UP*");
        assertNotContains(res, "*DOWN*");
        assertNotContains(res, "*strict*");

        Assert.assertEquals(HttpStatus.SC_OK, (res = rh.executeGetRequest("_opendistro/_security/health?pretty")).getStatusCode());
        System.out.println(res.getBody());
        assertContains(res, "*UP*");
        assertContains(res, "*strict*");
        assertNotContains(res, "*DOWN*");
    }

    @Test
    public void testHealthUnitialized() throws Exception {
        setup(Settings.EMPTY, null, Settings.EMPTY, false);

        RestHelper rh = nonSslRestHelper();
        HttpResponse res;
        Assert.assertEquals(
            HttpStatus.SC_OK,
            (res = rh.executeGetRequest("_opendistro/_security/health?pretty&mode=lenient")).getStatusCode()
        );
        System.out.println(res.getBody());
        assertContains(res, "*UP*");
        assertNotContains(res, "*DOWN*");
        assertNotContains(res, "*strict*");

        Assert.assertEquals(
            HttpStatus.SC_SERVICE_UNAVAILABLE,
            (res = rh.executeGetRequest("_opendistro/_security/health?pretty")).getStatusCode()
        );
        System.out.println(res.getBody());
        assertContains(res, "*DOWN*");
        assertContains(res, "*strict*");
        assertNotContains(res, "*UP*");
    }
}
