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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;

/**
 * @author Henrik Kaipe
 */
public class FailFastOnInconsistentVintage extends FailFastOnInconsistentPicks {

    private static String latestTestMethodName = "--";

    @Rule
    public final TestRule resetOnNewTestMethodName = (stmt,desc) -> {
        return new Statement() {
            @Override public void evaluate() throws Throwable {
                nonRepeatableIsPending =
                        false == latestTestMethodName.equals(desc.getMethodName());
                try {
                    stmt.evaluate();
                } finally {
                    latestTestMethodName = nonRepeatableIsPending
                            ? "--" : desc.getMethodName();
                }
            }
        };
    };

    @Test @Override
    public void pickNonRepeatable() {
        if (nonRepeatableIsPending) {
            nonRepeatableIsPending = false;
            LazyParams.pickValue("non_repeatable", 1, 2);
        }
    }

    @Test @Override
    public void nonRecoverable1st() {
        super.nonRecoverable1st();
    }

    @Test @Override
    public void diffent1stParameterOnSecondRun() {
        super.diffent1stParameterOnSecondRun();
    }

    @Test @Override
    public void nonRepeatable1stAndThenTwoRepeatableParams() {
        super.nonRepeatable1stAndThenTwoRepeatableParams();
    }

    @Test @Override
    public void twoUnrepeatableParams_and_TwoGoodOnes() {
        super.twoUnrepeatableParams_and_TwoGoodOnes();
    }

    @Test @Override
    public void lateConflictAfterFail1st() {
        super.lateConflictAfterFail1st();
    }

    @Test @Override
    public void failTwiceAndLaterIntroductionOfNonRepeatable() {
        super.failTwiceAndLaterIntroductionOfNonRepeatable();
    }

    @Test @Override
    public void nonRepeatableLast() {
        super.nonRepeatableLast();
    }
}
