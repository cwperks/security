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
import org.apache.hc.core5.http.message.BasicHeader;
import org.junit.Assert;
import org.junit.Test;

import org.opensearch.common.settings.Settings;
import org.opensearch.security.test.DynamicSecurityConfig;
import org.opensearch.security.test.SingleClusterTest;
import org.opensearch.security.test.helper.rest.RestHelper;
import org.opensearch.security.test.helper.rest.RestHelper.HttpResponse;
import org.opensearch.tasks.Task;

public class TaskTests extends SingleClusterTest {

    @Test
    public void testXOpaqueIdHeader() throws Exception {
        setup(Settings.EMPTY, new DynamicSecurityConfig(), Settings.EMPTY);

        RestHelper rh = nonSslRestHelper();
        HttpResponse res;
        Assert.assertEquals(
            HttpStatus.SC_OK,
            (res = rh.executeGetRequest(
                "_tasks?group_by=parents&pretty",
                encodeBasicHeader("nagilum", "nagilum"),
                new BasicHeader(Task.X_OPAQUE_ID, "myOpaqueId12")
            )).getStatusCode()
        );
        System.out.println(res.getBody());
        Assert.assertTrue(res.getBody().split("X-Opaque-Id").length > 2);
        Assert.assertTrue(!res.getBody().contains("failures"));
    }
}
