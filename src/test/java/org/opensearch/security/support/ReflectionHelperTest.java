/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.support;

import org.junit.Test;

import org.opensearch.OpenSearchException;
import org.opensearch.common.settings.Settings;
import org.opensearch.security.transport.OIDClusterRequestEvaluator;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ReflectionHelperTest {
    @Test
    public void testMissingEvaluatorFails() {
        String className = "org.opensearch.security.transport.MissingEvaluator";
        var exception = assertThrows(
            OpenSearchException.class,
            () -> ReflectionHelper.instantiateInterClusterRequestEvaluator(className, Settings.EMPTY)
        );
        assertTrue(exception.getMessage().contains(className));
        assertTrue(exception.getCause() instanceof ClassNotFoundException);
    }

    @Test
    public void testEvaluatorConstructorFailurePreservesCause() {
        var exception = assertThrows(
            OpenSearchException.class,
            () -> ReflectionHelper.instantiateInterClusterRequestEvaluator(OIDClusterRequestEvaluator.class.getName(), Settings.EMPTY)
        );
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertTrue(exception.getCause().getMessage().contains(ConfigConstants.SECURITY_CERT_OID));
    }

    @Test
    public void testExplicitEvaluatorLoads() {
        var evaluator = ReflectionHelper.instantiateInterClusterRequestEvaluator(
            OIDClusterRequestEvaluator.class.getName(),
            Settings.builder().put(ConfigConstants.SECURITY_CERT_OID, "1.2.3.4.5.5").build()
        );
        assertTrue(evaluator instanceof OIDClusterRequestEvaluator);
    }
}
