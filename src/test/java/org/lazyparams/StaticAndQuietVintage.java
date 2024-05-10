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

import org.junit.BeforeClass;
import org.junit.Test;
import org.lazyparams.showcase.FalseOrTrue;
import org.lazyparams.showcase.ScopedLazyParameter;

import static org.junit.Assert.assertEquals;

/**
 * @author Henrik Kaipe
 */
public class StaticAndQuietVintage {

    static int staticPick;
    static int count;

    @BeforeClass
    public static void staticPick() {
        staticPick = LazyParams.pickValue("staticPick", 1,2,3);
        count = 0;
    }

    @Test
    public void onlyPrimaryValuePickedInStaticScope() {
        assertEquals("Static pick", 1, staticPick);
    }

    @Test
    public void withQuietPick() {
        onlyPrimaryValuePickedInStaticScope();
        LazyParamsCoreUtil.displayOnFailure(new Object(),
                " Fail on #" + ++count);

        int intPick = LazyParams.pickValue("pick", 1,2,3);
        boolean quietBool = ScopedLazyParameter.from(false, true).quietly()
                .asParameter("quietPick").pickValue();
        boolean extraBool = FalseOrTrue.pickBoolean("extra true", "extra false");

        assertEquals("Quiet bool",
                3 == intPick ? !extraBool :  extraBool,
                quietBool);
    }
}
