/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.test.helper.rules;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class SecurityTestWatcher extends TestWatcher {

    @Override
    protected void starting(final Description description) {
        final String methodName = description.getMethodName();
        String className = description.getClassName();
        className = className.substring(className.lastIndexOf('.') + 1);
        System.out.println("---------------- Starting JUnit-test: " + className + " " + methodName + " ----------------");
    }

    @Override
    protected void failed(final Throwable e, final Description description) {
        final String methodName = description.getMethodName();
        String className = description.getClassName();
        className = className.substring(className.lastIndexOf('.') + 1);
        System.out.println(">>>> " + className + " " + methodName + " FAILED due to " + e);
    }

    @Override
    protected void finished(final Description description) {
        // System.out.println("-----------------------------------------------------------------------------------------");
    }

}
