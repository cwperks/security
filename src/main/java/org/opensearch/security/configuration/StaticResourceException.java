/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.configuration;

import java.io.IOException;

import org.opensearch.OpenSearchException;
import org.opensearch.common.io.stream.StreamInput;

public class StaticResourceException extends OpenSearchException {

    public StaticResourceException(StreamInput in) throws IOException {
        super(in);
    }

    public StaticResourceException(String msg, Object... args) {
        super(msg, args);
    }

    public StaticResourceException(String msg, Throwable cause, Object... args) {
        super(msg, cause, args);
    }

    public StaticResourceException(Throwable cause) {
        super(cause);
    }

}
