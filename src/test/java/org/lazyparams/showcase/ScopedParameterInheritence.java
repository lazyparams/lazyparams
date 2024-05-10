/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.showcase;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Henrik Kaipe
 */
public class ScopedParameterInheritence {

    private static ScopedLazyParameter<String> fromStaticScope =
            commonParameterDefinition();
    private static ScopedLazyParameter<String> inTestMetodScope =
            commonParameterDefinition();

    private static ScopedLazyParameter<Integer> intToBe1stIntroduceByTestMethod =
            ScopedLazyParameter.from(11,22).asParameter("int");

    static ScopedLazyParameter<String> commonParameterDefinition() {
        return ScopedLazyParameter.from("1st", "2nd")
                .asParameter("common");
    }

    static String latestStaticPick;
    static String latestPickFromTestMethod;
    static Integer testMethodIntPick;

    @BeforeAll
    static void pickStaticAndCompareWithIdenticalPick() {
        latestStaticPick = fromStaticScope.pickValue();
        assertSame(latestStaticPick, commonParameterDefinition().pickValue(),
                "Repeat pick from separate but identical parameter definition");
        assertSame(latestStaticPick, fromStaticScope.pickValue(),
                "Repeat pick from same parameter definition");
    }

    @Test void test() {
        latestPickFromTestMethod = null; testMethodIntPick = null;
        InAnyOrder.runVerbosly(
            () -> {
                assertSame(latestStaticPick, fromStaticScope.pickValue(),
                        "Introduced static paramter-def must stick to same value during test-method");
                return "FROM_STATIC_DEF";
            },
            () -> {
                assertSame(latestStaticPick, latestPickFromTestMethod = inTestMetodScope.pickValue(),
                        "Separate but identical parameter-def should introduce its own value");
                return "COMPR_METHOD_DEF";
            },
            () -> {
                assertSame(latestStaticPick, commonParameterDefinition().pickValue(),
                        "New separate but identical parameter intoduced");
                return "COMPR_MIDAIR_DEF";
            },
            () -> {
                testMethodIntPick = intToBe1stIntroduceByTestMethod.pickValue();
                return "PICKED_INT";
            }
        );
        assertSame(latestStaticPick, inTestMetodScope.pickValue(),
                "Repeated picks from here should not introduce any excitement");
        assertSame(latestStaticPick, commonParameterDefinition().pickValue(),
                "Repeated picks from here should not introduce any excitement");
    }

    @AfterAll
    static void repicksAfterFallbackToStaticScope() {
        assertSame(latestStaticPick, inTestMetodScope.pickValue(),
                "Definition used by test-method must release value when test-method scope closed");
        if (null != testMethodIntPick && latestPickFromTestMethod == latestStaticPick) {
            assertSame(testMethodIntPick, intToBe1stIntroduceByTestMethod.pickValue(),
                    "Only 50% successrate when picking int again!");
        }
    }
}
