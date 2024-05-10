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

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.lazyparams.core.Lazer;

/**
 * @author Henrik Kaipe
 */
@RunWith(Parameterized.class)
public class LeafParameterizedJupiterTest {

    @Rule
    public VerifyJupiterRule expectRule =
            new VerifyJupiterRule(LeafParameterizedJupiter.class);

    public LeafParameterizedJupiterTest(
            InstallScenario install, StaticScopeParam before,
            Object asynchronousExecution,
            Object displayBeforeEach,
            Object resultTweak, Object maxCountsTweak,
            StaticScopeParam after, MaxCountsTweak maxStatics) {
        try {
            /* Clean up any dangling tweak from previous test-execution: */
            LeafParameterizedJupiter.mutableExtensions.clear();
        } catch (Exception ignore) {}

        LeafParameterizedJupiter.mutableExtensions.add(install);
        expectRule.addTweaker(install);
        if (Boolean.TRUE.equals(asynchronousExecution)) {
            LeafParameterizedJupiter.mutableExtensions.add(AsyncExecution.INTERCEPTOR);
        }
        if (Boolean.TRUE.equals(displayBeforeEach)) {
            LeafParameterizedJupiter.mutableExtensions.add((BeforeEachCallback) ctx -> {
                Object displayId = new Object();
                LazyParamsCoreUtil.displayOnSuccess(displayId, "");
                LazyParamsCoreUtil.displayOnFailure(displayId, "");
            });
        }
        if (resultTweak instanceof ResultTweak) {
            LeafParameterizedJupiter.mutableExtensions.add(
                    (ResultTweak)resultTweak);
            expectRule.addTweaker(
                    ((ResultTweak)resultTweak)::modifyExpectations);
        }
        if (maxCountsTweak instanceof MaxCountsTweak) {
            LeafParameterizedJupiter.mutableExtensions.add(
                    (MaxCountsTweak)maxCountsTweak);
            expectRule.addTweaker(
                    ((MaxCountsTweak)maxCountsTweak)::modifyExpectations);
        }

        /*
         * Statics last seems like the most efficient way to get
         * the result-tweaking right!
         */
        LeafParameterizedJupiter.mutableExtensions.add(
                maxStatics.resetStaticConfigBeforeAll());
        LeafParameterizedJupiter.mutableExtensions.add(
                maxStatics.asAfterAllCallback());
        LeafParameterizedJupiter.mutableExtensions.add(before.asBeforeAll());
        expectRule.addTweaker(before.beforeModifications());
        LeafParameterizedJupiter.mutableExtensions.add(after.asAfterAll());
        expectRule.addTweaker(after.afterModifications());
        expectRule.addTweaker(maxStatics::modifyAfterAllExpectations);
    }

    @Parameterized.Parameters(name=
            "{0} beforeAll={1}) ---async={2} displayB4Each={3} resultTweak={4} maxcounts={5}--- (after={6} maxtatics={7}")
    public static List<?> tweaks() throws Lazer.ExpectedParameterRepetition {
        InstallScenario[] install = InstallScenario.values();
        StaticScopeParam[] beforeAll = StaticScopeParam.values();
        Object[] asynch = {false,true};
        Object[] displayB4Each = {false,true};
        Object[] resultTweak = ResultTweak.values();
        Object[] maxCountTweak = MaxCountsTweak.values();
        Object[] afterAll = StaticScopeParam.afterAllOptions();
        Object[] maxStatics = MaxCountsTweak.values();
        List<Object[]> valueRecords = new ArrayList<>(
                resultTweak.length * beforeAll.length + 10);

        /* Let's eat our own medicin when combining the above parameter values: */
        Lazer lazer = new Lazer();
        do {
            lazer.startNew();
            StaticScopeParam beforeAllPick = beforeAll[
                    lazer.pick(beforeAll, true, beforeAll.length)];
            InstallScenario installPick = beforeAllPick.moreThanOneOption()
                    ? InstallScenario.ALREADY_INSTALLED
                    : install[lazer.pick(install, true, install.length)];
            boolean beforeFailsAll = StaticScopeParam.FAILURE == beforeAllPick
                    || beforeAllPick.name().startsWith("FAIL_ALL");

            valueRecords.add(new Object[] {
                installPick,
                beforeAllPick,
                beforeFailsAll ? "()"
                : asynch[lazer.pick(asynch, true, asynch.length)],
                beforeFailsAll ? "()"
                : displayB4Each[lazer.pick(displayB4Each, true, displayB4Each.length)],
                beforeFailsAll ? "()"
                : resultTweak [lazer.pick(resultTweak, true, resultTweak.length)],
                beforeFailsAll ? "()"
                : maxCountTweak[lazer.pick(maxCountTweak, true, maxCountTweak.length)],
                afterAll[lazer.pick(afterAll, true, afterAll.length)],
                maxStatics[lazer.pick(maxStatics, true, maxStatics.length)]
            });
        } while (lazer.pendingCombinations());
//        System.out.println("Number of records: " + valueRecords.size());
        if (500 < valueRecords.size()) {
            throw new Error("Too many records ..."
                    + "\n- This would probably require too much execution time!");
        }
        return valueRecords;
    }

    @Test
    public void normal() {
        expectRule
                .pass("normal(\\(\\))?(staticplace)? 1st=34 2nd=sdf")
                .pass("normal(\\(\\))?(staticplace)? 1st=42 2nd=dfwe")
                .fail("normal(\\(\\))?(staticplace)? 1st=34 2nd=dfwe")
                        .withMessage("Fail here")
                .pass("normal(\\(\\))?(staticplace)? 1st=42 2nd=sdf")
                .fail("normal(\\(\\))?(staticplace)?")
                        .withMessage("1 test failed.*total 4.*");
    }

    @Test
    public void parameterization() {
        expectRule.methodParameterTypes(int.class)
                .pass("\\[1\\] nbr=28(staticplace)? 1st=34 2nd=sdf xtra=a One",
                        "parameterization.int..1.(staticplace)? 1st=34 2nd=sdf xtra=a One")
                .pass("\\[1\\] nbr=28(staticplace)? 1st=42 2nd=dfwe xtra=a One",
                        "parameterization.int..1.(staticplace)? 1st=42 2nd=dfwe xtra=a One")
                .fail("\\[1\\] nbr=28(staticplace)? 1st=34 2nd=dfwe",
                        "parameterization.int..1.(staticplace)? 1st=34 2nd=dfwe")
                        .withMessage("Fail here")
                .pass("\\[1\\] nbr=28(staticplace)? 1st=42 2nd=sdf xtra=a Two",
                        "parameterization.int..1.(staticplace)? 1st=42 2nd=sdf xtra=a Two")
                .pass("\\[1\\] nbr=28(staticplace)? 1st=34 2nd=sdf xtra=a Two",
                        "parameterization.int..1.(staticplace)? 1st=34 2nd=sdf xtra=a Two")
                .pass("\\[1\\] nbr=28(staticplace)? 1st=42 2nd=dfwe xtra=a Two",
                        "parameterization.int..1.(staticplace)? 1st=42 2nd=dfwe xtra=a Two")
                .fail("\\[1\\] nbr=28(staticplace)?",
                        "parameterization\\(int\\)\\[1\\](staticplace)?")
                        .withMessage("1 test failed.*total 6.*")
                .pass("\\[2\\] nbr=42(staticplace)? 1st=34 2nd=sdf xtra=a One",
                        "parameterization.int..2.(staticplace)? 1st=34 2nd=sdf xtra=a One")
                .pass("\\[2\\] nbr=42(staticplace)? 1st=42 2nd=dfwe xtra=a One",
                        "parameterization.int..2.(staticplace)? 1st=42 2nd=dfwe xtra=a One")
                .fail("\\[2\\] nbr=42(staticplace)? 1st=34 2nd=dfwe",
                        "parameterization.int..2.(staticplace)? 1st=34 2nd=dfwe")
                        .withMessage("Fail here")
                .pass("\\[2\\] nbr=42(staticplace)? 1st=42 2nd=sdf xtra=a Two",
                        "parameterization.int..2.(staticplace)? 1st=42 2nd=sdf xtra=a Two")
                .pass("\\[2\\] nbr=42(staticplace)? 1st=34 2nd=sdf xtra=a Two",
                        "parameterization.int..2.(staticplace)? 1st=34 2nd=sdf xtra=a Two")
                .pass("\\[2\\] nbr=42(staticplace)? 1st=42 2nd=dfwe xtra=a Two",
                        "parameterization.int..2.(staticplace)? 1st=42 2nd=dfwe xtra=a Two")
                .fail("\\[2\\] nbr=42(staticplace)?",
                        "parameterization\\(int\\)\\[2\\](staticplace)?")
                        .withMessage("1 test failed.*total 6.*")
                .pass("\\[3\\] nbr=43(staticplace)? 1st=34 2nd=sdf xtra=a One",
                        "parameterization.int..3.(staticplace)? 1st=34 2nd=sdf xtra=a One")
                .pass("\\[3\\] nbr=43(staticplace)? 1st=42 2nd=dfwe xtra=a One",
                        "parameterization.int..3.(staticplace)? 1st=42 2nd=dfwe xtra=a One")
                .fail("\\[3\\] nbr=43(staticplace)? 1st=34 2nd=dfwe",
                        "parameterization.int..3.(staticplace)? 1st=34 2nd=dfwe")
                        .withMessage("Fail here")
                .pass("\\[3\\] nbr=43(staticplace)? 1st=42 2nd=sdf xtra=a Two",
                        "parameterization.int..3.(staticplace)? 1st=42 2nd=sdf xtra=a Two")
                .pass("\\[3\\] nbr=43(staticplace)? 1st=34 2nd=sdf xtra=a Two",
                        "parameterization.int..3.(staticplace)? 1st=34 2nd=sdf xtra=a Two")
                .pass("\\[3\\] nbr=43(staticplace)? 1st=42 2nd=dfwe xtra=a Two",
                        "parameterization.int..3.(staticplace)? 1st=42 2nd=dfwe xtra=a Two")
                .fail("\\[3\\] nbr=43(staticplace)?",
                        "parameterization\\(int\\)\\[3\\](staticplace)?")
                        .withMessage("1 test failed.*total 6.*") 
                .pass(/*
                       * With some logic this should be a failure for
                       * the actual test-method as its result should cover results
                       * for all parameter value combination.
                       * However, that is not the case and it is because of how
                       * jupiter-params works, because this level in the hierarchy
                       * does not fall into any scope of LazyParams, so the
                       * core framework behaviour will prevail. This does not
                       * seem to bother IDEs much, however, because they seem clever
                       * enough to report failure on the test-method anyway!
                       */
                       "parameterization\\(int\\)(staticplace)?");
    }

    @Test
    public void repeat() {
        expectRule
                .pass("repetition 1 of 2(staticplace)? 1st=43 2nd=prime",
                        "repeat\\(\\)\\[1\\](staticplace)? 1st=43 2nd=prime")
                .pass("repetition 1 of 2(staticplace)? 1st=242 2nd=other",
                        "repeat\\(\\)\\[1\\](staticplace)? 1st=242 2nd=other")
                .fail("repetition 1 of 2(staticplace)? 1st=43 2nd=other",
                        "repeat\\(\\)\\[1\\](staticplace)? 1st=43 2nd=other")
                        .withMessage("Fail repeat")
                .pass("repetition 1 of 2(staticplace)? 1st=242 2nd=prime",
                        "repeat\\(\\)\\[1\\](staticplace)? 1st=242 2nd=prime")
                .fail("repetition 1 of 2(staticplace)?",
                        "repeat\\(\\)\\[1\\](staticplace)?")
                        .withMessage("1 tests? failed.*total 4\\D*")
                .pass("repetition 2 of 2(staticplace)? 1st=43 2nd=prime",
                        "repeat\\(\\)\\[2\\](staticplace)? 1st=43 2nd=prime")
                .pass("repetition 2 of 2(staticplace)? 1st=242 2nd=other",
                        "repeat\\(\\)\\[2\\](staticplace)? 1st=242 2nd=other")
                .fail("repetition 2 of 2(staticplace)? 1st=43 2nd=other",
                        "repeat\\(\\)\\[2\\](staticplace)? 1st=43 2nd=other")
                        .withMessage("Fail repeat")
                .pass("repetition 2 of 2(staticplace)? 1st=242 2nd=prime",
                        "repeat\\(\\)\\[2\\](staticplace)? 1st=242 2nd=prime")
                .fail("repetition 2 of 2(staticplace)?",
                        "repeat\\(\\)\\[2\\](staticplace)?")
                        .withMessage("1 tests? failed.*total 4\\D*")
                .pass(/*
                       * With some logic this should be a failure for
                       * the actual test-method as its result should cover results
                       * for all parameter value combination.
                       * However, that is not the case and it is because of how
                       * jupiter-engine works, because this level in the hierarchy
                       * does not fall into any scope of LazyParams, so the
                       * core framework behaviour will prevail. This does not
                       * seem to bother IDEs much, however, because they seem clever
                       * enough to report failure on the test-method anyway!
                       */
                       "repeat(\\(\\))?(staticplace)?");
    }

    @Test
    public void failWithoutParams() {
        expectRule.fail("failWithoutParams(\\(\\))?(staticplace)?")
                .withMessage("FAiLURE");
    }

    @Test
    public void manyParameters() {
        expectRule
                .pass("manyParameters(\\(\\))?(staticplace)? 1st=34 2nd=sdf nbr=1 boolean=false")
                .pass("manyParameters(\\(\\))?(staticplace)? 1st=42 2nd=dfwe nbr=2 boolean=false")
                .pass("manyParameters(\\(\\))?(staticplace)? 1st=34 2nd=dfwe")
                .fail("manyParameters(\\(\\))?(staticplace)? 1st=42 2nd=sdf nbr=3 boolean=true")
                        .withMessage(".*mix.*2.*but.*3.*")
                .pass("manyParameters(\\(\\))?(staticplace)? 1st=34 2nd=sdf nbr=2 boolean=true")
                .fail("manyParameters(\\(\\))?(staticplace)? 1st=42 2nd=dfwe nbr=1 boolean=true")
                        .withMessage(".*mix.*2.*but.*1.*")
                .pass("manyParameters(\\(\\))?(staticplace)? 1st=34 2nd=sdf nbr=3 boolean=false")
                .pass("manyParameters(\\(\\))?(staticplace)? 1st=42 2nd=dfwe nbr=3 boolean=false")
                .fail("manyParameters(\\(\\))?(staticplace)?")
                        .withMessage("2 tests failed.*total 8.*");;
    }
}
