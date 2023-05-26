/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.configuration;

import org.opensearch.action.get.MultiGetResponse.Failure;
import org.opensearch.security.securityconf.impl.SecurityDynamicConfiguration;

public interface ConfigCallback {

    void success(SecurityDynamicConfiguration<?> dConf);

    void noData(String id);

    void singleFailure(Failure failure);

    void failure(Throwable t);

}
