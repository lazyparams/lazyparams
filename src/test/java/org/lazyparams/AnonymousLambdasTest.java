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
import java.util.function.Consumer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Henrik Kaipe
 */
public class AnonymousLambdasTest {

    static Consumer<String> recorder;

    @BeforeAll @BeforeClass
    public static void initRecorder() {
        List<String> recorded = new ArrayList<>();
        recorder = recorded::add;
        LazyParams.currentScopeConfiguration().setScopedCustomItem(recorder, recorded);
    }

    static void verifyRecordings(String recordingsName) {
        /* Also testing scope consistency: */
        List<String> recorded = LazyParams.currentScopeConfiguration()
                .getScopedCustomItem(recorder);
        assertThat(recorded).as(recordingsName)
                .containsExactly("foo", "oof", "BAR", "RAB");
    }

    @AfterAll static void verifyPlanetary() {
        verifyRecordings("planetary recordings");
    }
    @AfterClass public static void verifyVintage() {
        verifyRecordings("vintage recordings");
    }

    void test() {
        LazyParams.<Consumer<String>>pickValue(lda -> "",
                this::record, this::recordReversed,
                s -> record("BAR"), s -> recordReversed("BAR"))
                .accept("foo");
    }

    void record(String text) {
        LazyParams.pickValue("recorded", text);
        recorder.accept(text);
    }

    void recordReversed(String text) {
        char[] chars = text.toCharArray();
        for (int i = 0, j = chars.length - 1; i < j; ++i, --j) {
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        record(new String(chars));
    }

    @Test void planetary() { test(); }
    @org.junit.Test public void vintage() { test(); }
}
