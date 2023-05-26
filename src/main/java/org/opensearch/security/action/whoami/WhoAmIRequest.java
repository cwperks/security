/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.action.whoami;

import java.io.IOException;

import org.opensearch.action.support.nodes.BaseNodesRequest;
import org.opensearch.common.io.stream.StreamInput;

public class WhoAmIRequest extends BaseNodesRequest<WhoAmIRequest> {

    public WhoAmIRequest(final StreamInput in) throws IOException {
        super(in);
    }

    public WhoAmIRequest() throws IOException {
        super(new String[0]);
    }
}
