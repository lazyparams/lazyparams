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

import java.text.DecimalFormat;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.lazyparams.LazyParams;
import org.lazyparams.LazyParamsCoreUtil;

public class ManyByMany {

    CharSequence timing = new CharSequence() {

        final long startTime = System.currentTimeMillis();

        @Override public int length() { return toString().length(); }
        @Override public char charAt(int index) { return toString().charAt(index); }
        @Override public CharSequence subSequence(int start, int end) {
            return toString().subSequence(start, end);
        }

        @Override
        public String toString() {
            return new DecimalFormat(" 0.000s ").format(
                    (System.currentTimeMillis() - startTime) / 1000.0);
        }
    };

    @BeforeEach void increaseMaxCount() {
        LazyParams.currentScopeConfiguration().setMaxTotalCount(3999);
        LazyParamsCoreUtil.displayOnSuccess("timing", timing);
    }

    /**
     * Benefits greatly from the
     * {@link org.lazyparams.core.Lazer.ValueStats#isTemporarilyParkedOnPrimaryValue()}
     * optimizing.
     */
    @Test void fortyToOne() {
        for (int i = 40; 0 < i; --i) {
            LazyParams.pickValue(String::valueOf, IntStream.range(0,i)
                    .mapToObj(Integer::valueOf)
                    .toArray());
        }
    }

    /**
     * Does only benefit a little from the
     * {@link org.lazyparams.core.Lazer.ValueStats#isTemporarilyParkedOnPrimaryValue()}
     * optimizing.
     */
    @Test void oneToForty() {
        for (int i = 1; i <= 40; ++i) {
            LazyParams.pickValue(String::valueOf, IntStream.range(0,i)
                    .mapToObj(Integer::valueOf)
                    .toArray());
        }
    }

    /**
     * Benefits a little from the
     * {@link org.lazyparams.core.Lazer.ValueStats#isTemporarilyParkedOnPrimaryValue()}
     * optimizing.
     */
    @Order(2)
    @Test
    void regular() {
        for (int i = 0; i < 25; ++i) {
            LazyParams.pickValue((char)('a' + i) + "",
                    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24);
        }
    }

    @Order(1)
    @Test
    void quickerPerhaps() {
        for (int i = 0; i < 25; ++i) {
            FoolParams.pickValue((char)('a' + i) + "",
                    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24);
        }
    }
}
