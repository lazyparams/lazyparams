/*
 * Copyright 2024-2025 the original author or authors.
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

import static java.util.regex.Pattern.quote;

/**
 * @author Henrik Kaipe
 */
public class VerifyJupiterRuleTest {

    @Rule
    public VerifyJupiterRule expectRule =
            new VerifyJupiterRule(VanillaJupiter.class);

    @Test
    public void normal() {
        expectRule.pass("normal\\(\\)");
    }
    @Test
    public void parameterization() {
        expectRule
                .methodParameterTypes(int.class)
                .pass("\\[1\\] nbr ?= ?28", quote("parameterization(int)[1]"))
                .fail("\\[2\\] nbr ?= ?42", quote("parameterization(int)[2]"))
                        .withMessage("Not perfect")
                .pass("\\[3\\] nbr ?= ?43", quote("parameterization(int)[3]"))
                .pass("parameterization\\(int\\)");
    }
    @Test
    public void repeat() {
        expectRule
                .pass("repetition 1 of 4", quote("repeat()[1]"))
                .pass("repetition 2 of 4", quote("repeat()[2]"))
                .pass("repetition 3 of 4", quote("repeat()[3]"))
                .pass("repetition 4 of 4", quote("repeat()[4]"))
                .pass("repeat\\(\\)");
        
    }
    @Test
    public void failedNormal() {
        expectRule.fail("failedNormal\\(\\)")
                .withMessage("Normal failure");
        
    }
    @Test
    public void factoryStuff() {
        expectRule
                .pass("factory success", quote("factoryStuff()[1]"))
                .fail("factory failure", quote("factoryStuff()[2]"))
                        .withMessage("Factory failure")
                .pass("more factory success", quote("factoryStuff()[3]"))
                .pass("factoryStuff\\(\\)");
    }
}
