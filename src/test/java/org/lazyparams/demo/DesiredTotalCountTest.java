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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.lazyparams.VerifyJupiterRule;

/**
 * @author Henrik Kaipe
 */
public class DesiredTotalCountTest {

    @Rule
    public VerifyJupiterRule expect = new VerifyJupiterRule(DesiredTotalCount.class);

    @Test
    public void notCombined() {
        String[] afterOrQuiet = {
            "1111", "XXXX", "2222", //#1 - 3
            "1X21", "X112", "22XX", //#4 - 6
            "121X", "X121", "2XX2", //#7 - 9
            "11X2", "X211", "2X2X", //#10 - 12
            "1X12", "X2X1", "212X", //#13 - 15
            "1222", "XX1X", "21X1", //#16 - 18
            "11XX", "XX22"        //#19 - 20
        };
        String[] beforeNoisy_10 = {
            "1111", "XXXX", "2222", //#1 - 3
            "X21X", "1X21", "21X2", //#4 - 6
            "2X12", "12X1", "X12X", //#7 - 9
            "11XX"                //#10
        };
        String[] beforeNoisy_5 = {
            "1111", "XXXX", "2222", //#1 - 3
            "X21X", "1122"        //#4 - 5
        };

        verify1X2(new String[][] {
            afterOrQuiet, afterOrQuiet, afterOrQuiet, beforeNoisy_10,
            beforeNoisy_5, afterOrQuiet, afterOrQuiet
        }, WithExpectedStaticValues::desiredCount);
    }

    @Test
    public void pairwiseCombined() {
        String[] afterOrQuiet = {
            "1111", "XXX1", "2221", //#1 - 3
            "1X2X", "21X2", "X21X", //#4 - 6
            "12X2", "X122", "2X12", //#7 - 9
            "21XX", "1X11", "X22X", //#10 - 12
            "1121", "XXXX", "2212", //#13 - 15
            "12X1", "X11X", "2X22", //#16 - 18
            "111X", "XXX1"        //#19 - 20
        };
        String[] beforeNoisy_10 = {
            "1111", "XXX1", "2221", //#1 - 3
            "X21X", "1X22", "21XX", //#4 - 6
            "2X12", "12X2", "X122", //#7 - 9
            "1X2X"                 //#10
        };
        String[] beforeNoisy_5 = {
            "1111", "XXX1", "2221", //#1 - 3
            "X21X", "1X22", "21XX", //#4 - 6
            "2X12", "X122", "12X2", //#7 - 9
            "1X2X"                 //#10
        };

        verify1X2(new String[][] {
            afterOrQuiet, afterOrQuiet, afterOrQuiet, beforeNoisy_10,
            beforeNoisy_5, afterOrQuiet, afterOrQuiet
        }, statics -> Math.max(10, statics.desiredCount()));
    }

    void verify1X2(String[][] expectations,
            ToIntFunction<WithExpectedStaticValues> desiredCount) {
        for (WithExpectedStaticValues statics : WithExpectedStaticValues.values()) {
            statics.on(expect, null).accept(Stream
                    .of(expectations[statics.ordinal()])
                    .limit(desiredCount.applyAsInt(statics))
                    .map(s -> String.format("M1=%s M2=%s M3=%s M4=%s",
                            s.charAt(0), s.charAt(1), s.charAt(2), s.charAt(3))));
        }
    }

    @Test
    public void fullyCombined() {
        String[] after_or_20 = {
            "----", "++-+", //#1 - 2
            "-+++", "+-+-", //#3 - 4
            "--+-", "++++", //#5 - 6
            "---+", "+++-", //#7 - 8
            "+---", "-+--", //#9 - 10
            "-++-", "+--+", //#11 - 12
            "--++", "++--", //#13 - 14
            "-+-+", "+-++", //#15 - 16
            "----", "++++", //#17 - 18 (Repeats 1-2)
            "-++-", "+--+" //#19 - 20  (Repeats 3-4)
        };
        String[] beforeNoisy_10 = {
            "----", "++-+", //#1 - 2
            "-+++", "+-+-", //#3 - 4
            "++++", "--+-", //#5 - 6
            "---+", "+++-", //#7 - 8
            "+---", "+-++", //#9 - 10
            "-+--", "-+-+", //#11 - 12
            "++--", "--++", //#13 - 14
            "+--+", "-++-", //#15 - 16
        };
        String[] beforeQuiet_10 = {
            "----", "++-+", //#1 - 2
            "-+++", "+-+-", //#3 - 4
            "--+-", "++++", //#5 - 6
            "---+", "+++-", //#7 - 8
            "+---", "-+--", //#9 - 10
            "-++-", "+--+", //#11 - 12
            "++--", "--++", //#13 - 14
            "-+-+", "+-++", //#15 - 16
        };
        String[] beforeNoisy_5 = {
            "----", "++-+", //#1 - 2
            "-+++", "+-+-", //#3 - 4
            "++++", "--+-", //#5 - 6
            "++--", "--++", //#7 - 8
            "-+-+", "+--+", //#9 - 10
            "+---", "-++-", //#11 - 12
            "-++-", "---+", //#13 - 14
            "+++-", "-+--"  //#15 - 16
        };

        String[][] orderedStreamTemplates = { after_or_20,
                beforeQuiet_10, after_or_20, beforeNoisy_10,
                beforeNoisy_5, after_or_20, after_or_20 };
        String appendixTemplate =
                "1st_on_global=\\%s 2nd_on_global=\\%s 1st_on_local=\\%s 2nd_on_local=\\%s";

        for (WithExpectedStaticValues statics : WithExpectedStaticValues.values()) {
            Stream<String> appendixStream = Stream
                    .of(orderedStreamTemplates[statics.ordinal()])
                    .limit(statics.name().contains("20") ? 20 : 16)
                    .map(s -> String.format(appendixTemplate,
                            s.charAt(0), s.charAt(1), s.charAt(2), s.charAt(3)));
            statics.on(expect,
                    WithExpectedStaticValues.desired_total_count_5_is_setup_BEFORE_each_with_noise == statics
                    ? ".*max.*at 16" : null)
                    .accept(appendixStream);
        }
    }

    enum WithExpectedStaticValues {
        desired_total_count_5_is_setup_AFTER_each,
        desired_total_count_10_is_setup_BEFORE_each,
        desired_total_count_20_is_setup_AFTER_each_with_noise,
        desired_total_count_10_is_setup_BEFORE_each_with_noise,
        desired_total_count_5_is_setup_BEFORE_each_with_noise,
        desired_total_count_20_is_setup_BEFORE_each,
        desired_total_count_10_is_setup_AFTER_each;

        final String staticNameEndRgx = " desired_total-count="
                + name().substring(20).replace('_', ' ') + " *";

        int desiredCount() {
            return Integer.parseInt(name().replaceAll("\\D++", ""));
        }

        Consumer<Stream<String>> on(final VerifyJupiterRule expect, String maxMsg) {
            return appends -> {
                AtomicInteger counter = new AtomicInteger(0);
                appends.map(appendix -> name().contains("AFTER")
                        ? appendix + " #" + counter.incrementAndGet()
                        : "#" + counter.incrementAndGet() + " " + appendix)
                        .map(appendix -> staticNameEndRgx + "/ " + appendix)
                        .forEach(expect::pass);
                if (null == maxMsg) {
                    expect.pass(staticNameEndRgx);
                } else {
                    expect.fail(staticNameEndRgx).withMessage(maxMsg);
                }
                Class<?> c = expect.getTestClass();
                expect.pass(
                        c.getSimpleName() + staticNameEndRgx,
                        c.getName() + staticNameEndRgx);
            };
        }
    }
}
