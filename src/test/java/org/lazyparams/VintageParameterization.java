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

import java.util.List;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.model.Statement;
import org.lazyparams.showcase.Timing;

import static org.junit.Assert.assertTrue;
import static org.lazyparams.LazyParams.pickValue;
import static org.lazyparams.showcase.FalseOrTrue.pickBoolean;

/**
 * @author Henrik Kaipe
 */
@RunWith(Parameterized.class)
public class VintageParameterization {

    @Rule
    public final TestRule printExecutionTime = new TestRule() {
        @Override
        public Statement apply(final Statement base, final Description d) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    Object duration = Timing.displayFromNow();
                    try {
                        base.evaluate();
                    } finally {
//                        System.out.println(d.getDisplayName()
//                                + " execution time: " + duration);
                    }
                }
            };
        }
    };

    @Parameterized.Parameter
    public int fromParameterized;

    @Parameterized.Parameters(name = "Parameterized={0}")
    public static List<?> values() {
        LazyParams.currentScopeConfiguration().setMaxFailureCount(6);
        return java.util.Arrays.asList(new Object[][] {
            {42},
            {24}
        });
    }

    @Test
    public void test() {
        if (pickBoolean("pickWithParameterized?")) {
            pickValue("value",90,fromParameterized,91);
        } else {
            pickValue("value",90,91);
        }
        assertTrue(pickBoolean("middle") | pickBoolean("final"));
    }
}
