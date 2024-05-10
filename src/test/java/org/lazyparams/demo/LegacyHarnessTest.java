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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.lazyparams.VerifyJupiterRule;
import org.lazyparams.VerifyVintageRule;

/**
 * @author Henrik Kaipe
 */
public class LegacyHarnessTest {

    @Rule
    public final TestRule delegationRule = (stmt,desc) -> {
        TestRule delegationTarget = desc.getMethodName().equals("test")
                ? (expectJupiter = new VerifyJupiterRule(LegacyHarnessExample.class) {
                    @Override
                    protected void customizeBeforeLaunch() {
                        List<VerifyJupiterRule.ResultVerifier> expects = getExpectations();
                        List<VerifyJupiterRule.ResultVerifier> original = new ArrayList<>(expects);
                        expects.clear();
                        for (final VerifyJupiterRule.ResultVerifier each : original) {
                            expects.add(new VerifyJupiterRule.ResultVerifier(each.displayNameRgx,
                                    each.legacyNameRgx.replaceFirst("\\_", "(.*TestMethod.*)?_"),
                                    each.getMessageRgx()));
                        }
                    }
                })
                : (expectLegacy = new VerifyVintageRule(LegacyHarnessReference.class));
        return delegationTarget.apply(stmt,desc);
    };

    VerifyJupiterRule expectJupiter;
    VerifyVintageRule expectLegacy;

    /**
     * Verifies test-results of {@link LegacyHarnessExample}, on which all
     * test-methods are indirect as they are actually invoked by common
     * test-method {@link LegacyHarness#test(LegacyHarness.TestMethod)} of the
     * abstract harness super-class.
     */
    @Test public void test() {
        expectJupiter.methodParameterTypes(LegacyHarness.TestMethod.class)
                .pass("test_1 before=1 inside=1 after=1")
                .fail("test_2 before=2 inside=2 after=1")
                        .withMessage(".*2.*")
                .pass("test_1 before=3 inside=3 after=2")
                .pass("test_2 before=1 inside=3 after=3")
                .pass("test_1 before=2 inside=1 after=3")
                .fail("test_2 before=3 inside=2 after=2")
                        .withMessage(".*2.*")
                .pass("test_1 before=3 inside=1 after=2")
                .pass("test_2 before=3 inside=3 after=1")
                .fail("test_2 before=2 inside=2 after=2")
                        .withMessage(".*2.*")
                .fail("test_2 before=1 inside=2 after=3")
                        .withMessage(".*2.*")
                .pass("test_1 before=1 inside=2 after=2")
                .pass("test_2 before=2 inside=3 after=1")
                .pass("test_2 before=3 inside=1 after=3")
                .fail("test", "test(.*TestMethod.*)?")
                        .withMessage(".*4.*fail.*total 13.*");
    }

    /** Verifies test-results of {@link LegacyHarnessReference#test_1() } */
    @Test public void test_1() {
        expectLegacy(false);
    }
    /** Verifies test-results of {@link LegacyHarnessReference#test_2() } */
    @Test public void test_2() {
        expectLegacy(true);
    }

    private void expectLegacy(boolean withFails) {
        Function<String,VerifyVintageRule.NextResult> expectFail = withFails
                ? rgx -> expectLegacy.fail(rgx).withMessage(".*2.*")
                : rgx -> expectLegacy.pass(rgx);

        expectLegacy.pass(" before=1 inside=1 after=1");
        expectFail.apply(" before=2 inside=2 after=2")
                .pass(" before=3 inside=3 after=3");
        expectFail.apply(" before=1 inside=2 after=3")
                .pass(" before=3 inside=1 after=2")
                .pass(" before=2 inside=3 after=1")
                .pass(" before=1 inside=3 after=2")
                .pass(" before=2 inside=1 after=3");
        expectFail.apply(" before=3 inside=2 after=1");

        if (withFails) {
            expectLegacy.fail("").withMessage(".*3.*fail.*total 9.*");
        } else {
            expectLegacy.pass("");
        }
    }
}
