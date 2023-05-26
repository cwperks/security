/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.support;

import java.io.Serializable;

import com.google.common.base.Strings;

import org.opensearch.common.util.concurrent.ThreadContext;

public class HeaderHelper {

    public static boolean isInterClusterRequest(final ThreadContext context) {
        return context.getTransient(ConfigConstants.OPENDISTRO_SECURITY_SSL_TRANSPORT_INTERCLUSTER_REQUEST) == Boolean.TRUE;
    }

    public static boolean isDirectRequest(final ThreadContext context) {

        return "direct".equals(context.getTransient(ConfigConstants.OPENDISTRO_SECURITY_CHANNEL_TYPE))
            || context.getTransient(ConfigConstants.OPENDISTRO_SECURITY_CHANNEL_TYPE) == null;
    }

    // CS-SUPPRESS-SINGLE: RegexpSingleline Java Cryptography Extension is unrelated to OpenSearch extensions
    public static boolean isExtensionRequest(final ThreadContext context) {
        return context.getTransient(ConfigConstants.OPENDISTRO_SECURITY_SSL_TRANSPORT_EXTENSION_REQUEST) == Boolean.TRUE;
    }
    // CS-ENFORCE-SINGLE

    public static String getSafeFromHeader(final ThreadContext context, final String headerName) {

        if (context == null || headerName == null || headerName.isEmpty()) {
            return null;
        }

        if (isInterClusterRequest(context) || isTrustedClusterRequest(context) || isDirectRequest(context)) {
            return context.getHeader(headerName);
        }

        return null;
    }

    public static Serializable deserializeSafeFromHeader(final ThreadContext context, final String headerName) {

        final String objectAsBase64 = getSafeFromHeader(context, headerName);

        if (!Strings.isNullOrEmpty(objectAsBase64)) {
            return Base64Helper.deserializeObject(objectAsBase64);
        }

        return null;
    }

    public static boolean isTrustedClusterRequest(final ThreadContext context) {
        return context.getTransient(ConfigConstants.OPENDISTRO_SECURITY_SSL_TRANSPORT_TRUSTED_CLUSTER_REQUEST) == Boolean.TRUE;
    }
}
