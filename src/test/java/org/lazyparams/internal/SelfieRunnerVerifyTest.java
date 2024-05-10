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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.lazyparams.VerifyVintageRule;

/**
 * @author Henrik Kaipe
 */
public class SelfieRunnerVerifyTest {

    final VerifyVintageRule expect = new VerifyVintageRule(SelfieRunnerTest.class);
    @Rule
    public final TestRule tweak_testname_to_expect = (stmt,desc) -> {
        return expect.apply(stmt, Description.createTestDescription(
                desc.getTestClass(),
                desc.getMethodName().replace('_', ' ')));
    };

    @Before
    public void commonParameterExpectation() {
        expect.pass(" dummy_param=foo").pass(" dummy_param=bar");
    }
    @After
    public void allRepetitionsSuccessful() {
        expect.pass("");
    }

    @Test public void first_test() {}
    @Test public void next_test_2_run() {}
    @Test public void three_times_a_charm() {}
}
