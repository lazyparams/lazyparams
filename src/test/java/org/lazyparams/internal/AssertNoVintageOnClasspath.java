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

import org.junit.jupiter.api.Test;
import org.lazyparams.LazyParams;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Purpose with this test is to make sure there is no unexpected vintage or
 * JUnit-4 dependency that must be present if framework is used with just JUnit-5.
 *
 * @author Henrik Kaipe
 */
public class AssertNoVintageOnClasspath {

    /**
     * Test is expected to fail unless being run on a test-execution that has
     * excluded the JUnit vintage class from classpath.
     */
    @Test void test() {
        String vintageClassName = LazyParams.pickValue("vintage-class",
                "org.junit.Test", "org.junit.Rule", "org.junit.runners.JUnit4",
                "org.junit.vintage.engine.VintageTestEngine");
        assertThrows(ClassNotFoundException.class, () -> Class.forName(vintageClassName));
    }
}
