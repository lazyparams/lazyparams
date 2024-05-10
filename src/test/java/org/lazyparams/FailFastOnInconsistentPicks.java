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

import java.lang.annotation.RetentionPolicy;
import org.junit.jupiter.api.Test;
import org.lazyparams.showcase.Qronicly;

/**
 * @author Henrik Kaipe
 */
public class FailFastOnInconsistentPicks {

    boolean nonRepeatableIsPending = true;

    @Test void pickNonRepeatable() {
        if (nonRepeatableIsPending) {
            nonRepeatableIsPending = false;
            LazyParams.pickValue("non_repeatable", 1, 2);
        }
    }

    @Test void nonRecoverable1st() {
        pickNonRepeatable();
        LazyParams.pickValue("repeatable", 1,2);
    }

    @Test void diffent1stParameterOnSecondRun() {
        if (nonRepeatableIsPending) {
            pickNonRepeatable();
        } else {
            LazyParams.pickValue("2nd run", 33,44);
        }
    }

    @Test void nonRepeatable1stAndThenTwoRepeatableParams() {
        pickNonRepeatable();
        LazyParams.pickValue("repeatable1", 11, 22);
        LazyParams.pickValue("repeatable2", 101, 202);
    }

    @Test void twoUnrepeatableParams_and_TwoGoodOnes() {
        if (nonRepeatableIsPending) {
            LazyParams.pickValue("non_repeatable2", -1, -2);
            pickNonRepeatable();
        }
        LazyParams.pickValue("good1", 4,5);
        LazyParams.pickValue("good2", 73, 42);
    }

    @Test void lateConflictAfterFail1st() {
        int first = LazyParams.pickValue("good-1st", 1, 2);
        pickNonRepeatable();
        int second = LazyParams.pickValue("good-again", 2,3);
        if (1 == first && 2 == second) {
            throw new AssertionError("Fail on 1st execution");
        }
    }

    @Test void failTwiceAndLaterIntroductionOfNonRepeatable() {
        boolean forceFailure = false;
        switch (Qronicly.<RetentionPolicy>pickValue().ordinal()) {
            case 1:
                LazyParams.pickValue("on3", -8,-9);
                forceFailure = true;
                break;
            case 2:
                pickNonRepeatable();
                break;
        }
        LazyParams.pickValue("finally", 99,999);
        if (forceFailure) {
            throw new AssertionError("Forced failure");
        }
    }

    @Test void nonRepeatableLast() {
        LazyParams.pickValue("1st", 42,73);
        pickNonRepeatable();
    }
}
