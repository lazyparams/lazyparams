/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.internal;

import org.junit.Test;
import org.lazyparams.LazyParams;

/**
 * Purpose with this test is to make sure there is no unexpected JUnit-5
 * dependency that must be present if framework is used with just JUnit-4.
 *
 * @author Henrik Kaipe
 */
public class AssertNoJUnit5onClasspath {

    /**
     * Test is expected to fail unless being run on a test-execution that has
     * excluded the JUnit-5 classes from classpath.
     */
    @Test(expected = ClassNotFoundException.class)
    public void test() throws ClassNotFoundException {
        Class.forName(LazyParams.pickValue("junit5-class",
                "org.junit.jupiter.api.Test",
                "org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor",
                "org.junit.vintage.engine.VintageTestEngine",
                "org.junit.platform.launcher.Launcher",
                "org.junit.platform.engine.TestDescriptor"));
    }
}
