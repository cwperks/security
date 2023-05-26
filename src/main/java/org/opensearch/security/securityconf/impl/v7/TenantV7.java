/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.securityconf.impl.v7;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.opensearch.security.securityconf.Hideable;
import org.opensearch.security.securityconf.StaticDefinable;

public class TenantV7 implements Hideable, StaticDefinable {

    private boolean reserved;
    private boolean hidden;
    @JsonProperty(value = "static")
    private boolean _static;
    private String description;

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isReserved() {
        return reserved;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
    }

    @JsonProperty(value = "static")
    public boolean isStatic() {
        return _static;
    }

    @JsonProperty(value = "static")
    public void setStatic(boolean _static) {
        this._static = _static;
    }

    @Override
    public String toString() {
        return "TenantV7 [reserved=" + reserved + ", hidden=" + hidden + ", _static=" + _static + ", description=" + description + "]";
    }

}
