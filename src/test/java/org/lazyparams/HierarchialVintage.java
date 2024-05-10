/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lazyparams.showcase.FalseOrTrue;

import static org.junit.Assert.assertEquals;

/**
 * @author Henrik Kaipe
 */
public class HierarchialVintage {
    static volatile int beforeClassCount = 0;
    static volatile int afterClassCount = 0;

    static void display(CharSequence text) {
        Object key = new Object();
        LazyParamsCoreUtil.displayOnFailure(key, text);
        LazyParamsCoreUtil.displayOnSuccess(key, text);
    }

    @BeforeClass
    public static void introduceParameterInStaticScope() {
        display(" OOJAA");
        LazyParams.pickValue("OLASD", 54321, 12354);
        assertEquals("Count before class", 1, ++beforeClassCount);
    }
    @AfterClass
    public static void afterAll() {
        assertEquals("After class count", 1, ++afterClassCount);
        assertEquals("Before class count", 1, beforeClassCount);
    }

    @Test
    public void twoParams() {
        LazyParams.currentScopeConfiguration().setMaxTotalCount(8);
        int i = LazyParams.pickValue("1st", 34, 42);
        String s = LazyParams.pickValue("2nd", "_i_", "_%_");
        if (34 == i && "_i_".equals(s)) {
            throw new IllegalStateException("Fail here");
        }
    }

    @Test
    public void noParamsHere() {
        display(" to pass!");
    }

    @Test
    public void butMoreHere() {
        try {
            twoParams();
        } catch (RuntimeException e) {
            throw e;
        }
        int extra = LazyParams.pickValue("extra", 1, 2, 3);
        if (FalseOrTrue.pickBoolean("verify extra")) {
            assertEquals("extra", 2, extra);
        }
    }

    @Test
    public void plainFailure() {
        display(" to fail ");
        throw new AssertionError("Test failed");
    }

    @After
    public void dislayOnFailure() {
        LazyParamsCoreUtil.displayOnFailure(new Object(), " HAS FAILED!");
    }
}
