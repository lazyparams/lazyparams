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

import org.lazyparams.LazyParams;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Same as {@link LegacyHarnessReference} but with a different super-class
 * {@link LegacyHarness}, which mimics {@link junit.framework.Test
   the JUnit-3 API harness super-class TestCase} by delegating to test-methods
 * that are identified in the same manner.
 * But technically {@link LegacyHarness} presents a JUpiter-style test-method
 * {@link LegacyHarness#test(LegacyHarness.TestMethod)} that achieves delegation
 * to the JUnit-3-style test-methods by having them as values of a lazy 
 * parameter. This will work just like a normal JUnit-3 test-class unless
 * there are additional parameters introduced. Then the parameterized nature of
 * the test-methods will show as they will be pairwise combined with the other
 * parameters. I.e. each test-method will execute at least once with each value
 * of all other parameters.
 * <br/>
 * This is very different from the real JUnit-3-style equivalent
 * {@link LegacyHarnessReference}, in which each test-method combines the
 * parameter values independently of one another, resulting in both test-methods
 * executed for the same parameter-value combinations. Here the test-methods
 * are parameters themselves and are pairwise combined with the actual parameters.
 * This means each test-method will iterate through its own set of parameter
 * combinations and though all parameter value pairs will be executed, most
 * value pairs will only execute for one of the test-methods (but each value
 * will execute at least once with each test-method).
 * <br/>
 * This concept could be useful for situations where most parameters are rather
 * primitive, e.g. their values are ints or strings etc, but there is also one
 * parameter that provides separate execution paths (i.e. the test-methods) that
 * do differ but are still similar by testing the same functionality for the
 * same ends with mostly the same parameters.
 *
 * @author Henrik Kaipe
 */
public class LegacyHarnessExample extends LegacyHarness {

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
