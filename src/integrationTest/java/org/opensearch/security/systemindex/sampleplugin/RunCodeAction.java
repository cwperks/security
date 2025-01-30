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

// CS-SUPPRESS-SINGLE: RegexpSingleline It is not possible to use phrase "cluster manager" instead of master here
import org.opensearch.action.ActionType;
import org.opensearch.action.support.master.AcknowledgedResponse;
// CS-ENFORCE-SINGLE

public class RunCodeAction extends ActionType<AcknowledgedResponse> {
    public static final RunCodeAction INSTANCE = new RunCodeAction();
    public static final String NAME = "cluster:monitor/code";

    private RunCodeAction() {
        super(NAME, AcknowledgedResponse::new);
    }
}
