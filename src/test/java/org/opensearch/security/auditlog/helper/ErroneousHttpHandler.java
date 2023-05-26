/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.auditlog.helper;

import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpRequestHandler;
import org.apache.hc.core5.http.protocol.HttpContext;

public class ErroneousHttpHandler implements HttpRequestHandler {
    @Override
    public void handle(ClassicHttpRequest request, ClassicHttpResponse response, HttpContext context) {
        response.setCode(404);
    }
}
