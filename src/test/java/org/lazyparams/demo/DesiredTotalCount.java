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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lazyparams.LazyParams;
import org.lazyparams.config.Configuration;
import org.lazyparams.showcase.CartesianProductHub;
import org.lazyparams.showcase.FalseOrTrue;
import org.lazyparams.showcase.FullyCombined;
import org.lazyparams.showcase.ScopedLazyParameter;

/**
 * Scenarios to demonstrate possible future feature
 * {@link Uncombined#forceRepeatUntilDesiredTotalCount(int)}.
 * From the outcome of this test it can be concluded that its behavior is more
 * predictable if applied after the repeated test. (E.g. during
 * {@link org.junit.After @After} or
 * {@link org.junit.jupiter.api.AfterEach @AfterEach}.) - Especially when
 * when pairwise combine conditions force additional repetitions beyond what
 * is specified with {@link Uncombined#forceRepeatUntilDesiredTotalCount(int)}.
 *
 * @author Henrik Kaipe
 */
public class DesiredTotalCount {

    final int desiredTotalCount = LazyParams.pickValue("desired_total-count", 5,10,20);
    final boolean setupBeforeEach = FalseOrTrue.pickBoolean(
            "is setup BEFORE each", "is setup AFTER each");
    final boolean withNoise = FalseOrTrue.pickBoolean("with noise");

    void setupDesiredCount() {
        if (withNoise) {
            Uncombined.forceRepeatUntilDesiredTotalCount(3);            
        }
        Uncombined.forceRepeatUntilDesiredTotalCount(desiredTotalCount);
    }

    @BeforeEach void setupBefore() {
        Configuration config = LazyParams.currentScopeConfiguration();
        config.setMaxTotalCount(16);
        if (setupBeforeEach) {
            setupDesiredCount();
        }
    }

    @AfterEach void setupAfter() {
        if (false == setupBeforeEach) {
            setupDesiredCount();
        }
    }

    @Test void notCombined() {
        Uncombined.pick("M1", '1','X','2');
        Uncombined.pick("M2", '1','X','2');
        Uncombined.pick("M3", '1','X','2');
        Uncombined.pick("M4", '1','X','2');
    }

    @Test void pairwiseCombined() {
        LazyParams.pickValue("M1", '1','X','2');
        LazyParams.pickValue("M2", '1','X','2');
        LazyParams.pickValue("M3", '1','X','2');
        LazyParams.pickValue("M4", '1','X','2');        
    }

    @Test void fullyCombined() {
        FullyCombined.pickFullyCombined("1st_on_global", "-", "+");
        FullyCombined.pickFullyCombined("2nd_on_global", "-", "+");
        combineOnLocalHub("1st_on_local", '-', '+');
        combineOnLocalHub("2nd_on_local", '-', '+');
    }

    <T> T combineOnLocalHub(String paramName, T... values) {
        return ScopedLazyParameter.from(values)
                .fullyCombinedOn(new CartesianProductHub() {})
                .asParameter(paramName)
                .pickValue();
    }
}
