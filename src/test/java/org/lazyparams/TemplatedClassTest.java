/*
 * Copyright 2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams;

import java.lang.annotation.RetentionPolicy;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Henrik Kaipe
 */
public class TemplatedClassTest {

    @Rule
    public VerifyJupiterRule expect = new VerifyJupiterRule(TemplatedClass.class);

    @Before
    public void reset() {
        TemplatedClass.latestClassinv = 0;
    }

    void expectPolicy(RetentionPolicy policy,
            String appendix, String expectedFailure) {
        String countPrefix = "\\[" + (policy.ordinal() + 1) + "\\] ";
        String modernAppendix =
                countPrefix + "policy ?= ?" + policy + " " + appendix;
        String legacyAppendix = TemplatedClass.class.getName()
                + countPrefix + appendix;
        if (null == expectedFailure) {
            expect.pass(modernAppendix, legacyAppendix);
        } else {
            expect.fail(modernAppendix, legacyAppendix)
                    .withMessage(expectedFailure);
        }
    }

    void normal_prohibitClassinvOn(RetentionPolicy policy) {
        expect  .pass(" prohibit_classinv_params /( /)* normal=41")
                .pass(" prohibit_classinv_params /( /)* normal=42")
                .pass(" prohibit_classinv_params /( /)*");
        expectPolicy(policy, "prohibit_classinv_params / clsInv=4 *",
                ".*expect.*0.*was.*4.*");
        expect  .pass(" prohibit_classinv_params /( /)* normal=41")
                .pass(" prohibit_classinv_params /( /)* normal=42")
                .pass(" prohibit_classinv_params /( /)*");
        expectPolicy(policy, "prohibit_classinv_params / clsInv=5",
                ".*expect.*0.*was.*5.*");
        expectPolicy(policy, "prohibit_classinv_params",
                ".*2.*fail.*total 2.*");
    }
    void normal_allowClassinvOn(RetentionPolicy policy) {
        for (String statics : new String[] {
            "allow_classinv_params / clsInv=4 inConstructor=1",
            "allow_classinv_params / clsInv=5 inConstructor=2",
            "allow_classinv_params / clsInv=4 inConstructor=2",
            "allow_classinv_params / clsInv=5 inConstructor=1"
        }) {
            expect  .pass(" " + statics + " / normal=41")
                    .pass(" " + statics + " / normal=42")
                    .pass(" " + statics + "( /)?");
            expectPolicy(policy, statics, null);
        }
        expectPolicy(policy, "allow_classinv_params", null);
    }

    @Test
    public void normalTest() {
        test(this::normal_prohibitClassinvOn,
                this::normal_allowClassinvOn);
    }

    void test(Consumer<RetentionPolicy> prohibiter, Consumer<RetentionPolicy> allower) {
        Stream.of(RetentionPolicy.values()).forEach(prohibiter);
        expect.pass(TemplatedClass.class.getSimpleName()
                + " prohibit_classinv_params",
                TemplatedClass.class.getName()
                + " prohibit_classinv_params");
        Stream.of(RetentionPolicy.values()).forEach(allower);
        expect.pass(TemplatedClass.class.getSimpleName()
                + " allow_classinv_params",
                TemplatedClass.class.getName()
                + " allow_classinv_params");
    }

    @Test
    public void parameterized() {
        expect.methodParameterTypes(String.class);
        test(this::parameterized_prohibitClassinvOn,
                this::parameterized_allowClassinvOn);
    }

    void parameterized_prohibitClassinvOn(RetentionPolicy policy) {
        for (int clsInv : new int[] {4,5}) {
            expect  .pass("\\[1\\] value ?= ?\"?tjo\"? prohibit_classinv_params( /)*",
                            "parameterized\\(String\\)\\[1\\] prohibit_classinv_params( /)*")
                    .pass("\\[2\\] value ?= ?\"?tjim\"? prohibit_classinv_params( /)*",
                        "parameterized\\(String\\)\\[2\\] prohibit_classinv_params( /)*")
                    .pass("parameterized\\(String\\) prohibit_classinv_params( /)*");
            expectPolicy(policy, "prohibit_classinv_params / clsInv=" + clsInv + " *",
                    ".*expect.*0.*was.*" + clsInv + ".*");
        }
        expectPolicy(policy, "prohibit_classinv_params", ".*2.*fail.*total 2.*");
    }
    void parameterized_allowClassinvOn(RetentionPolicy policy) {
        for (String statics : new String[] {
            "allow_classinv_params / clsInv=4 / inConstructor=1( /)?",
            "allow_classinv_params / clsInv=4 / inConstructor=2( /)?",
            "allow_classinv_params / clsInv=5 / inConstructor=1( /)?",
            "allow_classinv_params / clsInv=5 / inConstructor=2( /)?"
        }) {
            expect  .pass("\\[1\\] value ?= ?\"?tjo\"? " + statics,
                            "parameterized\\(String\\)\\[1\\] " + statics)
                    .pass("\\[2\\] value ?= ?\"?tjim\"? " + statics,
                            "parameterized\\(String\\)\\[2\\] " + statics)
                    .pass("parameterized\\(String\\) " + statics);
            if (statics.contains("2")) {
                statics = statics.replaceFirst(" / inConstr.*", "( /)*");
                expect.pass("parameterized\\(String\\) " + statics);
                expectPolicy(policy, statics, null);
            }
        }
        expectPolicy(policy, "allow_classinv_params", null);
    }
}
