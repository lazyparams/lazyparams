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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.runners.model.Statement;
import org.lazyparams.LazyParams;
import org.lazyparams.LazyParamsCoreUtil;
import org.lazyparams.showcase.ToPick;

/**
 * Demonstrates how a dummy parameter having Combiner#notCombined()
 * can be used to try out additional combinations beyond what is
 * necessary for satisfying all the pairwise combinations.
 * The dummy parameter is introduced during
 * {@link AfterEachCallback#afterEach(ExtensionContext)} (or after base
 * evaluation in the JUnit-4 rule alternative) so that
 * {@link org.lazyparams.core.Lazer} doesn't need to keep track of its
 * impact on the introduction of other parameters.
 *
 * @author Henrik Kaipe
 */
public class ContinueForUncombinedCount {
    static final int
            VINTAGE_TARGET_COUNT = 25,
            PLANETARY_TARGET_COUNT = 15;

    @Rule
    public final ForceRepetitionRule repeat25onVintage = new ForceRepetitionRule(VINTAGE_TARGET_COUNT);

    @org.junit.Test
    @RepeatTestAtLeastUntilTargetCount(PLANETARY_TARGET_COUNT)
    public void twoMatchResultsAndAnExtraInt() {
        String m1 = LazyParams.pickValue("m1", "1", "X", "2");
        String m2 = LazyParams.pickValue("m2", "1", "X", "2");
        int extra = LazyParams.pickValue("x", 1, 2);
    }

    @Before public void vintagePrefix() {
        LazyParamsCoreUtil.displayOnSuccess(new Object(), " vintage");
    }
    @BeforeEach void planetaryPrefix() {
        LazyParamsCoreUtil.displayOnSuccess(new Object(), " planetary");
    }

    @ExtendWith(ForceRepetitionRule.class)
    @org.junit.jupiter.api.Test
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RepeatTestAtLeastUntilTargetCount {
        int value();
    }

    public static class ForceRepetitionRule implements LambdaRule, AfterEachCallback {

        private final int targetCount;

        ForceRepetitionRule() { this(0); }
        ForceRepetitionRule(int targetCount) {
            this.targetCount = targetCount;
        }

        @Override
        public void evaluate(Statement base) throws Throwable {
            try {
                base.evaluate();
            } finally {
                repeatUntil(targetCount);
            }
        }

        @Override
        public void afterEach(ExtensionContext context) {
            final int targetCountFromAnnotationValue = context.getElement().get()
                    .getAnnotation(RepeatTestAtLeastUntilTargetCount.class)
                    .value();
            repeatUntil(targetCountFromAnnotationValue);
        }

        private static void repeatUntil(int targetCount) {
            IntStream.rangeClosed(1, targetCount)
                    .mapToObj(Integer::valueOf)
                    .collect(ToPick.from())
                    .notCombined()/*Important! or it will seek out combinations with other parameters!*/
                    .asParameter(i -> "#" + i)
                    .pickValue();
        }

        /**
         * A {@link #repeatUntil(int)} alternative that does not try to append
         * current count to test-name. This alternative quiet approach is
         * probably preferable in a realistic situation. But this test uses
         * the more verbose {@link #repeatUntil(int)}, in order to better
         * demonstrate how this works.
         */
        @SuppressWarnings("unused")
        private static void repeatQuietlyUntil(int targetCount) {
            LazyParamsCoreUtil.makePick(ForceRepetitionRule.class, false, targetCount);
        }
    }
}
