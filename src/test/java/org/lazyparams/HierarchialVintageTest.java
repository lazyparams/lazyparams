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

import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;

/**
 * @author Henrik Kaipe
 */
public class HierarchialVintageTest {

    @Rule
    public final VerifyVintageRule expect = new VerifyVintageRule(HierarchialVintage.class)
            .expectSkipMessageOnParametersInStaticScope();

    @Before
    public void resetStaticCountsOn_HierarchialVintage() {
        HierarchialVintage.afterClassCount = 0;
        HierarchialVintage.beforeClassCount = 0;
    }

    @Test
    public void twoParams() {
        expect
                .fail(" 1st=34 2nd=_i_ HAS FAILED!")
                .pass(" 1st=42 2nd=_%_")
                .pass(" 1st=34 2nd=_%_")
                .pass(" 1st=42 2nd=_i_")
                .fail("").withMessage(".*1.*fail.*total 4.*");
    }

    @Test
    public void noParamsHere() {
        expect.pass("");
    }

    @Test
    public void butMoreHere() {
        expect
                .fail(" 1st=34 2nd=_i_ HAS FAILED!")
                .pass(" 1st=42 2nd=_%_ extra=1 *")
                .pass(" 1st=42 2nd=_%_ extra=2 verify extra")
                .pass(" 1st=42 2nd=_%_ extra=3 *")
                .pass(" 1st=42 2nd=_i_ extra=2 *")
                .fail(" 1st=34 2nd=_%_ extra=1 verify extra HAS FAILED!")
                        .withMessage(".*2.*but was.*1.*")
                .fail(" 1st=34 2nd=_%_ extra=3 verify extra HAS FAILED!")
                        .withMessage(".*2.*but was.*3.*")
                .pass(" 1st=34 2nd=_%_ extra=2 *")
                .fail("").withMessage(".*max at 8");
    }

    @Test
    public void plainFailure() {
        expect.fail("").withMessage(".*fail.*");
    }
}
