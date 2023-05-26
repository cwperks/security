/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.securityconf.impl.v6;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.opensearch.security.securityconf.Hideable;

public class ActionGroupsV6 implements Hideable {

    private boolean readonly;
    private boolean hidden;
    private List<String> permissions = Collections.emptyList();

    public ActionGroupsV6() {
        super();
    }

    @JsonIgnore
    public boolean isReserved() {
        return readonly;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    @Override
    public String toString() {
        return "ActionGroups [readonly=" + readonly + ", hidden=" + hidden + ", permissions=" + permissions + "]";
    }

}
