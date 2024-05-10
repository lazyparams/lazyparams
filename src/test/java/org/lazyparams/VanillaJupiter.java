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

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

/**
 * Regular Jupiter test that is used by {@link VerifyJupiterRuleTest} as a
 * reference case to ensure there is no regression on {@link VerifyJupiterRule}.
 *
 * @author Henrik Kaipe
 */
public class VanillaJupiter {

    @Test
    public void normal() {
    }

    @ParameterizedTest
    @ValueSource(ints = {28, 42, 43})
    public void parameterization(int nbr) {
        if (42 == nbr) {
            throw new AssertionError("Not perfect");
        }
    }

    @RepeatedTest(4)
    public void repeat() {
    }


    @Test
    public void failedNormal() {
        throw new AssertionError("Normal failure");
    }

    @TestFactory
    public Stream<DynamicNode> factoryStuff() {
        return Stream.of(DynamicTest.dynamicTest("factory success", () -> {}),
                DynamicTest.dynamicTest("factory failure",
                        () -> { throw new AssertionError("Factory failure"); }),
                DynamicTest.dynamicTest("more factory success", () -> {}));
    }
}
