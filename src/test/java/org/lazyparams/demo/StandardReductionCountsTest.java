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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodDescriptor;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.MethodOrdererContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.lazyparams.LazyParams;
import org.lazyparams.showcase.ScopedLazyParameter;
import org.lazyparams.showcase.ToPick;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Disabled;

/**
 * Verify reasonable repetition counts for some standard pairwise scenarios.
 * Here "standard" means the exact same parameters are reintroduced in the same
 * order on each repetition, i.e. no concern for the volatile scenarios that
 * LazyParams' algorithms are designed to cope with.
 *
 * @author Henrik Kaipe
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(StandardReductionCountsTest.class)
public class StandardReductionCountsTest implements MethodOrderer {

    @Test
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType./*Test-*/METHOD)
    @interface AssertRepetitionCountBetween { int min(); int max(); }

    private final Map<Method,AtomicInteger> methodRepeatCounts = new HashMap<>();
    private ScopedLazyParameter<Method> completedTests;

    @AfterAll void assertAllRepeatCountsAreVerified() {
        if (null != completedTests) {
            assertThat(methodRepeatCounts)
                    .as("Map with pending repeat count verifications")
                    .isEmpty();
        }
    }

    @BeforeEach void keepCount(TestInfo details) {
        details.getTestMethod()
                .filter(m -> m.isAnnotationPresent(AssertRepetitionCountBetween.class))
                .ifPresent(m -> {
            methodRepeatCounts
                    .computeIfAbsent(m, __-> new AtomicInteger())
                    .incrementAndGet();
            LazyParams.currentScopeConfiguration().setMaxTotalCount(
                    /*... to support repetition count up to reasonable upper bound: */
                    m.getAnnotation(AssertRepetitionCountBetween.class).max() * 3 / 2 - 1);
        });
    }

    @Test
    void verifyRepeatCount() {
        LazyParams.currentScopeConfiguration().setMaxFailureCount(10);
        if (null == completedTests) {
            completedTests = methodRepeatCounts.keySet().stream()
                    .sorted(Comparator.comparing(m -> methodRepeatCounts.get(m).intValue()))
                    .collect(ToPick.from())
                    .asParameter(m -> {
                        AssertRepetitionCountBetween bounds =
                                m.getAnnotation(AssertRepetitionCountBetween.class);
                        return methodRepeatCounts.get(m) + " on " + m.getName()
                                + " to be between " + bounds.min()
                                + " and " + bounds.max();
                    });
        }
        Method m = completedTests.pickValue();
        AssertRepetitionCountBetween bounds =
                m.getAnnotation(AssertRepetitionCountBetween.class);
        assertThat(methodRepeatCounts.remove(m).intValue())
                .as("Repetition Count")
                .isBetween(bounds.min(), bounds.max());
    }

    @Override
    public void orderMethods(MethodOrdererContext context) {
        context.getMethodDescriptors().sort(Comparator
                .comparing((MethodDescriptor md)
                        -> md.getMethod().isAnnotationPresent(AssertRepetitionCountBetween.class))
                .reversed());
    }

    void makePicks(int nbrOfPicks, Object... values) {
        while (0 <= --nbrOfPicks) {
            ScopedLazyParameter.from(values)
                    .withExplicitParameterId(values.length + "_" + nbrOfPicks)
                    .asParameter(String::valueOf)
                    .pickValue();
        }
    }

    void threes(int nbrOfPicks) { makePicks(nbrOfPicks, 0,1,2); }
    void twos(int nbrOfPicks) { makePicks(nbrOfPicks, 0,1); }
    void fours(int nbrOfPicks) { makePicks(nbrOfPicks, 0,1,2,3); }
    void fives(int nbrOfPicks) { makePicks(nbrOfPicks, 0,1,2,3,4); }

    @AfterEach void appendCount(TestInfo details) {
        if (details.getTestMethod()
                .filter(m -> m.isAnnotationPresent(AssertRepetitionCountBetween.class))
                .isPresent()) {
            Uncombined.forceRepeatUntilDesiredTotalCount(1);
        }
    }

    @AssertRepetitionCountBetween( min=9, max=9) void threeOf3() { threes(3); }

    /* To be published at {@link https://www.pairwise.org/efficiency.html} */
    @AssertRepetitionCountBetween( min=9, max=10) void fourOf3() { threes(4); }

    /* To be published at {@link https://www.pairwise.org/efficiency.html} */
    @AssertRepetitionCountBetween( min=12, max=20) void thirteenOf3() { threes(13); }

    @AssertRepetitionCountBetween( min=4, max=4) void threeOf2() { twos(3); }

    /* To be published at {@link https://www.pairwise.org/efficiency.html} */
    @AssertRepetitionCountBetween( min=9, max=16) void oneHundredOf2() { twos(100); }

    /* To be published at {@link https://www.pairwise.org/efficiency.html} */
    @AssertRepetitionCountBetween( min=100, max=288) void twentyOf10() {
        makePicks(20, 0,1,2,3,4,5,6,7,8,9);
    }

    @AssertRepetitionCountBetween( min=200, max=600) @Disabled("because of long execute time")
    void fiftyOf12() {
        makePicks(50, 0,1,2,3,4,5,6,7,8,9,'a','b');
    }

    @AssertRepetitionCountBetween( min=16, max=16) void fourOf4() { fours(4); }
    @AssertRepetitionCountBetween( min=16, max=23) void fiveOf4() { fours(5); }
    @AssertRepetitionCountBetween( min=25, max=35) void fourOf5() { fives(4); }
    @AssertRepetitionCountBetween( min=25, max=41) void fiveOf5() { fives(5); }
    @AssertRepetitionCountBetween( min=25, max=46) void sixOf5() { fives(6); }
    @AssertRepetitionCountBetween( min=25, max=50) void sevenOf5() { fives(7); }

    /* To be published at {@link https://www.pairwise.org/efficiency.html} */
    @AssertRepetitionCountBetween( min=25, max=45)
    void fifteen4seventeen3twentynine2() {
        fours(15); threes(17); twos(29);
    }
    /** Slides down to 51 combinations when parameters are introduced in
     * the opposite order.
     * The lazy algorithm is generally better when parameters with many values
     * are introduced early. */
    @AssertRepetitionCountBetween( min=25, max=51)
    void fifteen4seventeen3twentynine2_reverse() {
        twos(29); threes(17); fours(15);
    }

    /* To be published at {@link https://www.pairwise.org/efficiency.html} */
    @AssertRepetitionCountBetween( min=21, max=33)
    void one4thirtynine3thirtyfive2() {
        LazyParams.currentScopeConfiguration().setMaxTotalCount(33);
        fours(1); threes(39); twos(35);
    }
    /** Slides down to 40 combinations when parameters are introduced in
     * the opposite order.
     * The lazy algorithm is generally better when parameters with many values
     * are introduced early. */
    @AssertRepetitionCountBetween( min=21, max=45)
    void one4thirtynine3thirtyfive2_reverse() {
        twos(35); threes(39); fours(1);
    }
}
