/*
 * Copyright 2015-2018 _floragunn_ GmbH
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

package org.opensearch.security.action.configupdate;

import java.io.IOException;

import org.opensearch.Version;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.action.support.nodes.BaseNodesRequest;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;

public class ConfigUpdateRequest extends BaseNodesRequest<ConfigUpdateRequest> {

    private String[] configTypes;
    private String[] entityNames;

    public ConfigUpdateRequest(StreamInput in) throws IOException {
        super(in);
        this.configTypes = in.readStringArray();
        if (in.getVersion().onOrAfter(Version.V_3_1_0)) {
            this.entityNames = in.readOptionalStringArray();
        }
    }

    public ConfigUpdateRequest() {
        super(new String[0]);
    }

    public ConfigUpdateRequest(String[] configTypes) {
        this();
        setConfigTypes(configTypes);
    }

    public ConfigUpdateRequest(String configType, String[] entityNames) {
        this();
        setConfigTypes(new String[] { configType });
        setEntityNames(entityNames);
    }

    @Override
    public void writeTo(final StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeStringArray(configTypes);
        if (out.getVersion().onOrAfter(Version.V_3_1_0)) {
            out.writeOptionalStringArray(entityNames);
        }
    }

    public String[] getConfigTypes() {
        return configTypes;
    }

    public void setConfigTypes(final String[] configTypes) {
        this.configTypes = configTypes;
    }

    public String[] getEntityNames() {
        return entityNames;
    }

    public void setEntityNames(final String[] entityNames) {
        this.entityNames = entityNames;
    }

    @Override
    public ActionRequestValidationException validate() {
        if (configTypes == null || configTypes.length == 0) {
            return new ActionRequestValidationException();
        } else if (configTypes.length > 1 && (entityNames != null && entityNames.length > 1)) {
            return new ActionRequestValidationException();
        }
        return null;
    }
}
