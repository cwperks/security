/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.setting;

import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Setting.Property;
import org.opensearch.common.settings.Settings;
import org.opensearch.security.support.ConfigConstants;

public class StandbyModeSetting extends OpensearchDynamicSetting<Boolean> {

    public static final Setting<Boolean> STANDBY_MODE = Setting.boolSetting(
        ConfigConstants.SECURITY_STANDBY_MODE,
        false,
        Property.NodeScope,
        Property.Dynamic,
        Property.Sensitive
    );

    public StandbyModeSetting(final Settings settings) {
        super(STANDBY_MODE, STANDBY_MODE.get(settings));
    }

    @Override
    protected String getClusterChangeMessage(final Boolean standbyMode) {
        return String.format("Detected change in settings, security standby mode is %s", standbyMode ? "enabled" : "disabled");
    }
}
