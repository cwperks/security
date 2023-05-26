/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.greenrobot.eventbus.Subscribe;

import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.security.securityconf.DynamicConfigModel;
import org.opensearch.security.setting.OpensearchDynamicSetting;
import org.opensearch.security.support.ConfigConstants;

import static org.opensearch.security.support.ConfigConstants.SECURITY_UNSUPPORTED_PASSIVE_INTERTRANSPORT_AUTH_INITIALLY;

public class CompatConfig {

    private final Logger log = LogManager.getLogger(getClass());
    private final Settings staticSettings;
    private DynamicConfigModel dcm;
    private final OpensearchDynamicSetting<Boolean> transportPassiveAuthSetting;

    public CompatConfig(final Environment environment, final OpensearchDynamicSetting<Boolean> transportPassiveAuthSetting) {
        super();
        this.staticSettings = environment.settings();
        this.transportPassiveAuthSetting = transportPassiveAuthSetting;
    }

    @Subscribe
    public void onDynamicConfigModelChanged(DynamicConfigModel dcm) {
        this.dcm = dcm;
        log.debug("dynamicSecurityConfig updated?: {}", (dcm != null));
    }

    // true is default
    public boolean restAuthEnabled() {
        final boolean restInitiallyDisabled = staticSettings.getAsBoolean(
            ConfigConstants.SECURITY_UNSUPPORTED_DISABLE_REST_AUTH_INITIALLY,
            false
        );
        final boolean isTraceEnabled = log.isTraceEnabled();
        if (restInitiallyDisabled) {
            if (dcm == null) {
                if (isTraceEnabled) {
                    log.trace("dynamicSecurityConfig is null, initially static restDisabled");
                }
                return false;
            } else {
                final boolean restDynamicallyDisabled = dcm.isRestAuthDisabled();
                if (isTraceEnabled) {
                    log.trace("opendistro_security.dynamic.disable_rest_auth {}", restDynamicallyDisabled);
                }
                return !restDynamicallyDisabled;
            }
        } else {
            return true;
        }

    }

    // true is default
    public boolean transportInterClusterAuthEnabled() {
        final boolean interClusterAuthInitiallyDisabled = staticSettings.getAsBoolean(
            ConfigConstants.SECURITY_UNSUPPORTED_DISABLE_INTERTRANSPORT_AUTH_INITIALLY,
            false
        );
        final boolean isTraceEnabled = log.isTraceEnabled();
        if (interClusterAuthInitiallyDisabled) {
            if (dcm == null) {
                if (isTraceEnabled) {
                    log.trace("dynamicSecurityConfig is null, initially static interClusterAuthDisabled");
                }
                return false;
            } else {
                final boolean interClusterAuthDynamicallyDisabled = dcm.isInterTransportAuthDisabled();
                if (isTraceEnabled) {
                    log.trace("plugins.security.dynamic.disable_intertransport_auth {}", interClusterAuthDynamicallyDisabled);
                }
                return !interClusterAuthDynamicallyDisabled;
            }
        } else {
            return true;
        }
    }

    /**
     * Returns true if passive transport auth is enabled
     */
    public boolean transportInterClusterPassiveAuthEnabled() {
        final boolean interClusterAuthInitiallyPassive = transportPassiveAuthSetting.getDynamicSettingValue();
        if (log.isTraceEnabled()) {
            log.trace("{} {}", SECURITY_UNSUPPORTED_PASSIVE_INTERTRANSPORT_AUTH_INITIALLY, interClusterAuthInitiallyPassive);
        }
        return interClusterAuthInitiallyPassive;
    }
}
