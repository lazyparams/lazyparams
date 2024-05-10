/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.demo;

import junit.framework.TestCase;
import org.lazyparams.LazyParams;

import static org.junit.Assert.assertNotEquals;

/**
 * Same as {@link LegacyHarnessExample} except it extends the traditional
 * JUnit-3 harness super-class {@link TestCase), therewith making it a very
 * regular kind of test-class, where each test-method navigates parameter value
 * combinations independently of one another.
 * The two test methods have the same parameters and therefore they produce same
 * test iteratations.
 *
 * @author Henrik Kaipe
 */
public class LegacyHarnessReference extends TestCase {

    @Override protected void setUp() {
        LazyParams.pickValue("before", 1,2,3);
    }
    @Override protected void tearDown() {
        LazyParams.pickValue("after", 1,2,3);
    }

    public void test_1() {
        LazyParams.pickValue("inside", 1,2,3);
    }
    public void test_2() {
        assertNotEquals(2, (int) LazyParams.pickValue("inside", 1,2,3));
    }
}
