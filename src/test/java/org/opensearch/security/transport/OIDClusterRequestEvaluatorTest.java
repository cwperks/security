/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.transport;

import java.security.cert.X509Certificate;

import org.junit.Test;

import org.opensearch.common.settings.Settings;
import org.opensearch.security.support.ConfigConstants;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OIDClusterRequestEvaluatorTest {
    @Test
    public void testExplicitOidRequired() {
        assertThrows(IllegalArgumentException.class, () -> new OIDClusterRequestEvaluator(Settings.EMPTY));
        for (String value : new String[] { "", " " }) {
            assertThrows(
                IllegalArgumentException.class,
                () -> new OIDClusterRequestEvaluator(Settings.builder().put(ConfigConstants.SECURITY_CERT_OID, value).build())
            );
        }
    }

    @Test
    public void testExplicitOidComparesExtensionValues() {
        String oid = "1.2.3.4.5.5";
        var evaluator = new OIDClusterRequestEvaluator(Settings.builder().put(ConfigConstants.SECURITY_CERT_OID, oid).build());
        X509Certificate local = mock(X509Certificate.class);
        X509Certificate peer = mock(X509Certificate.class);
        X509Certificate[] localChain = { local };
        X509Certificate[] peerChain = { peer };
        when(local.getExtensionValue(oid)).thenReturn(new byte[] { 4, 1, 1 });
        when(peer.getExtensionValue(oid)).thenReturn(new byte[] { 4, 1, 1 });
        assertTrue(evaluator.isInterClusterRequest(null, localChain, peerChain, null));
        when(peer.getExtensionValue(oid)).thenReturn(new byte[] { 4, 1, 2 });
        assertFalse(evaluator.isInterClusterRequest(null, localChain, peerChain, null));
        when(peer.getExtensionValue(oid)).thenReturn(null);
        assertFalse(evaluator.isInterClusterRequest(null, localChain, peerChain, null));
        assertFalse(evaluator.isInterClusterRequest(null, null, peerChain, null));
    }
}
