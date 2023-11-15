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

package org.opensearch.security.filter;

import org.apache.http.HttpStatus;
import org.junit.Test;

import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.rest.RestResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class SecurityResponseTests {

    @Test
    public void testSecurityResponseHasSingleContentType() {
        final SecurityResponse response = new SecurityResponse(HttpStatus.SC_OK, null, "foo bar", XContentType.JSON.mediaType());

        final RestResponse restResponse = response.asRestResponse();
        assertThat(restResponse.status(), equalTo(RestStatus.OK));
        assertThat(restResponse.contentType(), equalTo(XContentType.JSON.mediaType()));
    }
}
