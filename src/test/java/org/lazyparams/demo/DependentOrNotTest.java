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

import org.junit.Rule;
import org.junit.Test;
import org.lazyparams.VerifyVintageRule;

/**
 * It can be somewhat hard to grasp what it means to have parameters that are
 * dependent or independent of one another, yet still a concern because it
 * affects whether pairwise combination reduction will happen or not.
 *
 * @author Henrik Kaipe
 */
public class DependentOrNotTest {

    @Rule
    public final VerifyVintageRule expect = new VerifyVintageRule(DependentOrNot.class) {
        int count = 0;

        @Override
        public VerifyVintageRule.NextResult pass(String nameRgx) {
            return super.pass("".equals(nameRgx) ? ""
                    : nameRgx + " \\#" + ++count);
        }
    };

    @Test
    public void independent1stAnd2nd_reduces_18_possible_combinations_to_9() {
        expect.pass(" before=1 1st=1 2nd=1")
                .pass(" before=2 1st=2 2nd=2")
                .pass(" before=3 1st=1 2nd=3")
                .pass(" before=2 1st=2 2nd=1")
                .pass(" before=1 1st=2 2nd=3")
                .pass(" before=3 1st=1 2nd=2")
                .pass(" before=2 1st=1 2nd=3")
                .pass(" before=3 1st=2 2nd=1")
                .pass(" before=1 1st=1 2nd=2")
                .pass("");
    }

    @Test
    public void all_18_combinations_When2ndParameterValueDependsOn1st() {
        expect.pass(" before=1 1st=1 2nd=0")
                .pass(" before=2 1st=2 2nd=0")
                .pass(" before=3 1st=3 2nd=0")
                .pass(" before=1 1st=2 2nd=2")
                .pass(" before=3 1st=1 2nd=1")
                .pass(" before=3 1st=3 2nd=3")
                .pass(" before=2 1st=3 2nd=0")
                .pass(" before=1 1st=3 2nd=3")
                .pass(" before=2 1st=1 2nd=0")
                .pass(" before=1 1st=1 2nd=1")
                .pass(" before=2 1st=2 2nd=2")
                .pass(" before=3 1st=2 2nd=0")
                .pass(" before=1 1st=2 2nd=0")
                .pass(" before=2 1st=1 2nd=1")
                .pass(" before=3 1st=1 2nd=0")
                .pass(" before=1 1st=3 2nd=0")
                .pass(" before=2 1st=3 2nd=3")
                .pass(" before=3 1st=2 2nd=2")
                .pass("");
    }

    @Test
    public void all_18_combinations_When2ndParameterNameDependsOn1st() {
        expect.pass(" before=1 1st=1 2nd_after_1=1")
                .pass(" before=2 1st=2 2nd_after_2=1")
                .pass(" before=3 1st=1 2nd_after_1=2")
                .pass(" before=2 1st=2 2nd_after_2=2")
                .pass(" before=1 1st=2 2nd_after_2=3")
                .pass(" before=1 1st=1 2nd_after_1=3")
                .pass(" before=3 1st=2 2nd_after_2=2")
                .pass(" before=3 1st=2 2nd_after_2=1")
                .pass(" before=1 1st=2 2nd_after_2=1")
                .pass(" before=3 1st=1 2nd_after_1=1")
                .pass(" before=2 1st=1 2nd_after_1=2")
                .pass(" before=2 1st=1 2nd_after_1=3")
                .pass(" before=1 1st=1 2nd_after_1=2")
                .pass(" before=2 1st=2 2nd_after_2=3")
                .pass(" before=3 1st=1 2nd_after_1=3")
                .pass(" before=1 1st=2 2nd_after_2=2")
                .pass(" before=2 1st=1 2nd_after_1=1")
                .pass(" before=3 1st=2 2nd_after_2=3")
                .pass("");
    }

    @Test
    public void all_12_combinations_When2ndParameterIntroductionDependsOn1st() {
        expect.pass(" before=1 2nd: value=1")
                .pass(" before=2 1st: default_value")
                .pass(" before=3 2nd: value=2")
                .pass(" before=1 2nd: value=3")
                .pass(" before=3 1st: default_value")
                .pass(" before=1 1st: default_value")
                .pass(" before=3 2nd: value=1")
                .pass(" before=2 2nd: value=2")
                .pass(" before=2 2nd: value=3")
                .pass(" before=1 2nd: value=2")
                .pass(" before=2 2nd: value=1")
                .pass(" before=3 2nd: value=3")
                .pass("");
    }
}
