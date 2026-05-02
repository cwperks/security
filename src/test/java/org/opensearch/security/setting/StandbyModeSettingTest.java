/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.setting;

import java.util.Set;

import org.junit.Test;

import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.Settings;
import org.opensearch.security.support.ConfigConstants;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StandbyModeSettingTest {

    @Test
    public void testStandbyModeTracksClusterSettingsUpdates() {
        final Settings initialSettings = Settings.builder().put(ConfigConstants.SECURITY_STANDBY_MODE, true).build();
        final StandbyModeSetting standbyModeSetting = new StandbyModeSetting(initialSettings);
        final ClusterSettings clusterSettings = new ClusterSettings(initialSettings, Set.of(StandbyModeSetting.STANDBY_MODE));
        standbyModeSetting.registerClusterSettingsChangeListener(clusterSettings);

        assertTrue(standbyModeSetting.getDynamicSettingValue());

        clusterSettings.applySettings(Settings.builder().put(ConfigConstants.SECURITY_STANDBY_MODE, false).build());
        assertFalse(standbyModeSetting.getDynamicSettingValue());

        clusterSettings.applySettings(Settings.builder().put(ConfigConstants.SECURITY_STANDBY_MODE, true).build());
        assertTrue(standbyModeSetting.getDynamicSettingValue());
    }
}
