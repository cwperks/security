/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 */

package org.opensearch.security.systemindex.sampleplugin;

import java.io.IOException;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.core.common.io.stream.StreamInput;

public class RunCodeRequest extends ActionRequest {

    private final String code;

    public RunCodeRequest(String code) {
        this.code = code;
    }

    public RunCodeRequest(StreamInput in) throws IOException {
        super(in);
        this.code = in.readString();
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    public String getCode() {
        return this.code;
    }
}
