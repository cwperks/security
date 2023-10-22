/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.ssl.http.netty;

import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.security.filter.SecurityResponse;

public class Netty4RequestContext {
    public SecurityResponse earlyResponse;
    public ThreadContext.StoredContext storedContext;
    public Boolean shouldDecompress;
    public Boolean isAuthenticated;

    public Netty4RequestContext() {
        this.shouldDecompress = Boolean.FALSE;
        this.isAuthenticated = Boolean.FALSE;
    }
}
