/*
 * Copyright 2024-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.showcase;

import org.junit.Rule;
import org.junit.Test;
import org.lazyparams.VerifyJupiterRule;

/**
 * @author Henrik Kaipe
 */
public class ToListLessCombinedTest {

    @Rule
    public VerifyJupiterRule expect = new VerifyJupiterRule(ToListLessCombined.class) {

        String evaluateDigitPrefix(String nameRgx) {
            char firstCh = nameRgx.charAt(0);

            if (firstCh < '0' || '9' < firstCh) {
                return nameRgx;
            } else {
                StringBuilder sb = new StringBuilder(" src-size=").append(firstCh);
                if (2 <= nameRgx.length()) {
                    sb.append(" / 0\\.\\d{3}s").append(nameRgx.substring(1));
                }
                return sb.toString();
            }
        }

        @Override
        public VerifyJupiterRule.SpecifyFailMessageOrNextResult fail(String nameRgx) {
            return super.fail(evaluateDigitPrefix(nameRgx));
        }
        @Override
        public VerifyJupiterRule.NextResult pass(String nameRgx) {
            return super.pass(evaluateDigitPrefix(nameRgx));
        }
    };

    @Test
    public void oneOrTwo() {
        expect
                .pass("1 1or2=\\[null\\]")
                .pass("ToListLessCombined src-size=1",
                        "org.lazyparams.showcase.ToListLessCombined src-size=1")
                .pass("2 1or2=\\[null\\]")
                .pass("2 1or2=\\[null, b\\]")
                .pass("2 1or2=\\[b\\]")
                .pass("2")
                .pass("ToListLessCombined src-size=2",
                        "org.lazyparams.showcase.ToListLessCombined src-size=2")
                .pass("3 1or2=\\[null\\]")
                .pass("3 1or2=\\[b\\]")
                .pass("3 1or2=\\[null, C\\]")
                .pass("3")
                .pass("ToListLessCombined src-size=3",
                        "org.lazyparams.showcase.ToListLessCombined src-size=3")
                .pass("4 1or2=\\[null\\]")
                .pass("4 1or2=\\[b, 4\\]")
                .pass("4 1or2=\\[null, C\\]")
                .pass("4 1or2=\\[4\\]")
                .pass("4 1or2=\\[C\\]")
                .pass("4")
                .pass("ToListLessCombined src-size=4",
                        "org.lazyparams.showcase.ToListLessCombined src-size=4")
                .pass("5 1or2=\\[null\\]")
                .pass("5 1or2=\\[b, 5\\]")
                .pass("5 1or2=\\[C\\]")
                .pass("5 1or2=\\[null, 4\\]")
                .pass("5 1or2=\\[5\\]")
                .pass("5")
                .pass("ToListLessCombined src-size=5",
                        "org.lazyparams.showcase.ToListLessCombined src-size=5")
                .pass("6 1or2=\\[null\\]")
                .pass("6 1or2=\\[b\\]")
                .pass("6 1or2=\\[C, 6\\]")
                .pass("6 1or2=\\[null, 4\\]")
                .pass("6 1or2=\\[b, 5\\]")
                .pass("6 1or2=\\[6\\]")
                .pass("6 1or2=\\[4\\]")
                .pass("6")
                .pass("ToListLessCombined src-size=6",
                        "org.lazyparams.showcase.ToListLessCombined src-size=6")
                .pass("7 1or2=\\[null\\]")
                .pass("7 1or2=\\[b\\]")
                .pass("7 1or2=\\[C, null\\]")
                .pass("7 1or2=\\[4\\]")
                .pass("7 1or2=\\[null, 5\\]")
                .pass("7 1or2=\\[b, 6\\]")
                .fail("7 1or2=\\[null\\]").withMessage(".*duplicate.*")
                .fail("7")
                        .withMessage(".*1.*fail.*total 7.*")
                .pass("ToListLessCombined src-size=7",
                        "org.lazyparams.showcase.ToListLessCombined src-size=7")
                .pass("8 1or2=\\[null\\]")
                .pass("8 1or2=\\[b\\]")
                .pass("8 1or2=\\[C, null\\]")
                .pass("8 1or2=\\[4, HH\\]")
                .pass("8 1or2=\\[null, 5\\]")
                .pass("8 1or2=\\[b, 6\\]")
                .fail("8 1or2=\\[null\\]").withMessage(".*duplicate.*")
                .pass("8 1or2=\\[HH\\]")
                .pass("8 1or2=\\[5\\]")
                .fail("8")
                        .withMessage(".*1.*fail.*total 9.*")
                .pass("ToListLessCombined src-size=8",
                        "org.lazyparams.showcase.ToListLessCombined src-size=8")
                .pass("9 1or2=\\[null\\]")
                .pass("9 1or2=\\[b\\]")
                .pass("9 1or2=\\[C, HH\\]")
                .pass("9 1or2=\\[4, !!\\]")
                .pass("9 1or2=\\[5\\]")
                .pass("9 1or2=\\[null, 6\\]")
                .pass("9 1or2=\\[b, null\\]")
                .pass("9 1or2=\\[HH\\]")
                .pass("9 1or2=\\[!!\\]")
                .pass("9")
                .pass("ToListLessCombined src-size=9",
                        "org.lazyparams.showcase.ToListLessCombined src-size=9");
    }

    @Test
    public void oneOrPermutationOfTwo() {
        expect
                .pass("1 1or2=\\[0\\]")
                .pass("ToListLessCombined src-size=1",
                        "org.lazyparams.showcase.ToListLessCombined src-size=1")
                .pass("2 1or2=\\[0\\]")
                .pass("2 1or2=\\[0, 1\\]")
                .pass("2 1or2=\\[1\\]")
                .pass("2 1or2=\\[1, 0\\]")
                .pass("2")
                .pass("ToListLessCombined src-size=2",
                        "org.lazyparams.showcase.ToListLessCombined src-size=2")
                .pass("3 1or2=\\[0\\]")
                .pass("3 1or2=\\[1\\]")
                .pass("3 1or2=\\[0, 2\\]")
                .pass("3 1or2=\\[2, 0\\]")
                .pass("3")
                .pass("ToListLessCombined src-size=3",
                        "org.lazyparams.showcase.ToListLessCombined src-size=3")
                .pass("4 1or2=\\[0\\]")
                .pass("4 1or2=\\[1, 3\\]")
                .pass("4 1or2=\\[2, 1\\]")
                .pass("4 1or2=\\[2, 3\\]")
                .pass("4 1or2=\\[2\\]")
                .pass("4")
                .pass("ToListLessCombined src-size=4",
                        "org.lazyparams.showcase.ToListLessCombined src-size=4")
                .pass("5 1or2=\\[0\\]")
                .pass("5 1or2=\\[1, 4\\]")
                .pass("5 1or2=\\[2\\]")
                .pass("5 1or2=\\[3, 1\\]")
                .pass("5 1or2=\\[3, 4\\]")
                .pass("5")
                .pass("ToListLessCombined src-size=5",
                        "org.lazyparams.showcase.ToListLessCombined src-size=5")
                .pass("6 1or2=\\[0\\]")
                .pass("6 1or2=\\[1\\]")
                .pass("6 1or2=\\[2, 5\\]")
                .pass("6 1or2=\\[3, 2\\]")
                .pass("6 1or2=\\[3, 4\\]")
                .pass("6 1or2=\\[5, 4\\]")
                .pass("6 1or2=\\[3\\]")
                .pass("6")
                .pass("ToListLessCombined src-size=6",
                        "org.lazyparams.showcase.ToListLessCombined src-size=6")
                .pass("7 1or2=\\[0\\]")
                .pass("7 1or2=\\[1\\]")
                .pass("7 1or2=\\[2, 6\\]")
                .pass("7 1or2=\\[3\\]")
                .pass("7 1or2=\\[4, 2\\]")
                .pass("7 1or2=\\[4, 5\\]")
                .pass("7 1or2=\\[6, 5\\]")
                .pass("7")
                .pass("ToListLessCombined src-size=7",
                        "org.lazyparams.showcase.ToListLessCombined src-size=7")
                .pass("8 1or2=\\[0\\]")
                .pass("8 1or2=\\[1\\]")
                .pass("8 1or2=\\[2, 6\\]")
                .pass("8 1or2=\\[7, 3\\]")
                .pass("8 1or2=\\[0, 4\\]")
                .pass("8 1or2=\\[5, 1\\]")
                .pass("8 1or2=\\[6\\]")
                .pass("8 1or2=\\[7\\]")
                .pass("8 1or2=\\[4\\]")
                .pass("8")
                .pass("ToListLessCombined src-size=8",
                        "org.lazyparams.showcase.ToListLessCombined src-size=8")
                .pass("9 1or2=\\[0\\]")
                .pass("9 1or2=\\[1\\]")
                .pass("9 1or2=\\[2, 7\\]")
                .pass("9 1or2=\\[8, 3\\]")
                .pass("9 1or2=\\[4\\]")
                .pass("9 1or2=\\[0, 5\\]")
                .pass("9 1or2=\\[6, 1\\]")
                .pass("9 1or2=\\[7\\]")
                .pass("9 1or2=\\[8\\]")
                .pass("9")
                .pass("ToListLessCombined src-size=9",
                        "org.lazyparams.showcase.ToListLessCombined src-size=9");
    }

    @Test
    public void individuallyCombined() {
        expect
                .pass(" src-size=1 / 0\\.\\d{3}s individuals=\\[\\]")
                .pass("1 individuals=\\[11\\]")
                .pass("1")
                .pass("ToListLessCombined src-size=1",
                        "org.lazyparams.showcase.ToListLessCombined src-size=1")
                .pass("2 individuals=\\[\\]")
                .pass("2 individuals=\\[11, 12\\]")
                .pass("2")
                .pass("ToListLessCombined src-size=2",
                        "org.lazyparams.showcase.ToListLessCombined src-size=2")
                .pass("3 individuals=\\[\\]")
                .pass("3 individuals=\\[11, 12, 13\\]")
                .pass("3")
                .pass("ToListLessCombined src-size=3",
                        "org.lazyparams.showcase.ToListLessCombined src-size=3")
                .pass("4 individuals=\\[\\]")
                .pass("4 individuals=\\[11, 12, 13, 14\\]")
                .pass("4")
                .pass("ToListLessCombined src-size=4",
                        "org.lazyparams.showcase.ToListLessCombined src-size=4")
                .pass("5 individuals=\\[\\]")
                .pass("5 individuals=\\[11, 12, 13, 14, 15\\]")
                .pass("5 individuals=\\[12, 14\\]")
                .pass("5 individuals=\\[11, 13, 15\\]")
                .pass("5")
                .pass("ToListLessCombined src-size=5",
                        "org.lazyparams.showcase.ToListLessCombined src-size=5")
                .pass("6 individuals=\\[\\]")
                .pass("6 individuals=\\[11, 12, 13, 14, 15, 16\\]")
                .pass("6 individuals=\\[12, 14, 16\\]")
                .pass("6 individuals=\\[11, 13, 15\\]")
                .pass("6")
                .pass("ToListLessCombined src-size=6",
                        "org.lazyparams.showcase.ToListLessCombined src-size=6")
                .pass("7 individuals=\\[\\]")
                .pass("7 individuals=\\[11, 12, 13, 14, 15, 16, 17\\]")
                .pass("7 individuals=\\[12, 14, 16\\]")
                .pass("7 individuals=\\[11, 13, 15, 17\\]")
                .pass("7")
                .pass("ToListLessCombined src-size=7",
                        "org.lazyparams.showcase.ToListLessCombined src-size=7")
                .pass("8 individuals=\\[\\]")
                .pass("8 individuals=\\[11, 12, 13, 14, 15, 16, 17, 18\\]")
                .pass("8 individuals=\\[12, 14, 16, 18\\]")
                .pass("8 individuals=\\[11, 13, 15, 17\\]")
                .pass("8")
                .pass("ToListLessCombined src-size=8",
                        "org.lazyparams.showcase.ToListLessCombined src-size=8")
                .pass("9 individuals=\\[\\]")
                .pass("9 individuals=\\[11, 12, 14, 15, 16, 17, 18, 19\\]")
                .pass("9 individuals=\\[12, 13, 15, 17, 19\\]")
                .pass("9 individuals=\\[11, 13, 14, 16, 18\\]")
                .pass("9")
                .pass("ToListLessCombined src-size=9",
                        "org.lazyparams.showcase.ToListLessCombined src-size=9");
    }

    @Test
    public void permutations() {
        expect
                .pass("1 permutation=\\[z\\]")
                .pass("ToListLessCombined src-size=1",
                        "org.lazyparams.showcase.ToListLessCombined src-size=1")
                .pass("2 permutation=\\[z, Y\\]")
                .pass("2 permutation=\\[Y, z\\]")
                .pass("2")
                .pass("ToListLessCombined src-size=2",
                        "org.lazyparams.showcase.ToListLessCombined src-size=2")
                .pass("3 permutation=\\[z, Y, x\\]")
                .pass("3 permutation=\\[x, z, Y\\]")
                .pass("3 permutation=\\[Y, x, z\\]")
                .pass("3")
                .pass("ToListLessCombined src-size=3",
                        "org.lazyparams.showcase.ToListLessCombined src-size=3")
                .pass("4 permutation=\\[z, Y, x, 9\\]")
                .pass("4 permutation=\\[9, z, Y, x\\]")
                .pass("4 permutation=\\[x, 9, z, Y\\]")
                .pass("4 permutation=\\[Y, 9, x, z\\]")
                .pass("4")
                .pass("ToListLessCombined src-size=4",
                        "org.lazyparams.showcase.ToListLessCombined src-size=4")
                .pass("5 permutation=\\[z, Y, x, 9, 8\\]")
                .pass("5 permutation=\\[9, z, Y, 8, x\\]")
                .pass("5 permutation=\\[x, 9, 8, z, Y\\]")
                .pass("5 permutation=\\[Y, 8, 9, x, z\\]")
                .pass("5 permutation=\\[8, z, Y, x, 9\\]")
                .pass("5")
                .pass("ToListLessCombined src-size=5",
                        "org.lazyparams.showcase.ToListLessCombined src-size=5")
                .pass("6 permutation=\\[z, Y, x, 9, 8, 7\\]")
                .pass("6 permutation=\\[9, z, Y, 8, 7, x\\]")
                .pass("6 permutation=\\[x, 9, 8, 7, z, Y\\]")
                .pass("6 permutation=\\[Y, 8, 7, 9, x, z\\]")
                .pass("6 permutation=\\[8, 7, z, Y, x, 9\\]")
                .pass("6 permutation=\\[7, 9, z, Y, 8, x\\]")
                .pass("6")
                .pass("ToListLessCombined src-size=6",
                        "org.lazyparams.showcase.ToListLessCombined src-size=6")
                .pass("7 permutation=\\[z, Y, x, 9, 8, 7, W\\]")
                .pass("7 permutation=\\[9, z, Y, 8, 7, W, x\\]")
                .pass("7 permutation=\\[x, 9, 8, 7, W, z, Y\\]")
                .pass("7 permutation=\\[Y, 8, 7, W, 9, x, z\\]")
                .pass("7 permutation=\\[8, 7, W, z, Y, x, 9\\]")
                .pass("7 permutation=\\[7, W, 9, z, Y, 8, x\\]")
                .pass("7 permutation=\\[W, x, 9, 8, 7, z, Y\\]")
                .pass("7")
                .pass("ToListLessCombined src-size=7",
                        "org.lazyparams.showcase.ToListLessCombined src-size=7")
                .pass("8 permutation=\\[z, Y, x, 9, 8, 7, W, v\\]")
                .pass("8 permutation=\\[9, z, Y, 8, 7, W, v, x\\]")
                .pass("8 permutation=\\[x, 9, 8, 7, W, v, z, Y\\]")
                .pass("8 permutation=\\[Y, 8, 7, W, v, 9, x, z\\]")
                .pass("8 permutation=\\[8, 7, W, v, z, Y, x, 9\\]")
                .pass("8 permutation=\\[7, W, v, 9, z, Y, 8, x\\]")
                .pass("8 permutation=\\[W, v, x, 9, 8, 7, z, Y\\]")
                .pass("8 permutation=\\[v, Y, 8, 7, W, 9, x, z\\]")
                .pass("8")
                .pass("ToListLessCombined src-size=8",
                        "org.lazyparams.showcase.ToListLessCombined src-size=8")
                .pass("9 permutation=\\[z, Y, x, 9, 8, 7, W, v, !\\]")
                .pass("9 permutation=\\[9, z, Y, 8, 7, W, v, !, x\\]")
                .pass("9 permutation=\\[x, 9, 8, 7, W, v, !, z, Y\\]")
                .pass("9 permutation=\\[Y, 8, 7, W, v, !, 9, x, z\\]")
                .pass("9 permutation=\\[8, 7, W, v, !, z, Y, x, 9\\]")
                .pass("9 permutation=\\[7, W, v, !, 9, z, Y, 8, x\\]")
                .pass("9 permutation=\\[W, v, !, x, 9, 8, 7, z, Y\\]")
                .pass("9 permutation=\\[v, !, Y, 8, 7, W, 9, x, z\\]")
                .pass("9 permutation=\\[!, z, 7, Y, v, x, 9, 8, W\\]")
                .pass("9")
                .pass("ToListLessCombined src-size=9",
                        "org.lazyparams.showcase.ToListLessCombined src-size=9");
    }
}
