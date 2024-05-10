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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lazyparams.LazyParams;
import org.lazyparams.showcase.FalseOrTrue;

/**
 * It can be somewhat hard to grasp what it means to have parameters that are
 * dependent or independent of one another, yet still a concern because it
 * affects whether pairwise combination reduction will happen or not.
 *
 * @author Henrik Kaipe
 */
public class DependentOrNot {

    @Before
    public void beforeIsIndependentOfOtherParameters() {
        LazyParams.pickValue("before", 1,2,3);
    }
    @After
    public void repetitionCounterSuffix() {
        Uncombined.forceRepeatUntilDesiredTotalCount(
                /*until at least*/ 1 /*but only used for its suffix here!*/);
    }

    @Test
    public void independent1stAnd2nd_reduces_18_possible_combinations_to_9() {
        LazyParams.pickValue("1st", 1,2);
        LazyParams.pickValue("2nd", 1,2,3);
    }

    @Test
    public void all_18_combinations_When2ndParameterValueDependsOn1st() {
        int value1 = LazyParams.pickValue("1st", 1,2,3);
        LazyParams.pickValue("2nd", 0, value1);
    }

    @Test
    public void all_18_combinations_When2ndParameterNameDependsOn1st() {
        int value1 = LazyParams.pickValue("1st", 1,2);
        LazyParams.pickValue("2nd_after_" + value1, 1,2,3);
    }

    @Test
    public void all_12_combinations_When2ndParameterIntroductionDependsOn1st() {
        int value = FalseOrTrue.pickBoolean("1st: default_value") ? 7
                : LazyParams.pickValue("2nd: value", 1,2,3);
    }
}
