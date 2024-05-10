/*
 * Copyright 2024 the original author or authors.
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
public class InAnyOrderCasesFlatTest {

    @Rule
    public final VerifyJupiterRule expect = new VerifyJupiterRule(InAnyOrderCasesFlat.class) {

        String evaluateDigitPrefix(String nameRgx) {
            char firstCh = nameRgx.charAt(0);

            if (firstCh < '0' || '9' < firstCh) {
                return nameRgx;
            } else {
                return " " + firstCh + " tasks 0\\.\\d\\d\\ds" + nameRgx.substring(1);
            }
        }

        @Override
        public SpecifyFailMessageOrNextResult fail(String nameRgx) {
            return super.fail(evaluateDigitPrefix(nameRgx));
        }
        @Override
        public NextResult pass(String nameRgx) {
            return super.pass(evaluateDigitPrefix(nameRgx));
        }
    };

    @Test public void test() {
        expect
                .pass("0 verbose")
                .pass("1 qronicly abORc=A 12or3=1")
                .pass("2 quietly 12or3=2 abORc=A")
                .pass("3 quietly abORc=B 12or3=3 on3pass")
                .fail("4 qronicly Task1\\[ 12or3=2 abORc=c\\] TaskTwo\\[\\] IOException\\[ on3fail\\]")
                        .withMessage("an xeption")
                .pass("4 quietly 12or3=2 abORc=B on3pass")
                .fail("3 qronicly IOException\\[ on3fail\\]")
                        .withMessage("an xeption")
                .pass("1 quietly abORc=B 12or3=3")
                .pass("2 qronicly 12or3=3 abORc=B")
                .fail("3 quietly on3fail")
                        .withMessage("an xeption")
                .fail("3 quietly abORc=B 12or3=3 on3fail")
                        .withMessage("an xeption")
                .pass("3 quietly abORc=A 12or3=3 on3pass")
                .fail("4 quietly 12or3=3 abORc=A on3fail")
                        .withMessage("an xeption")
                .pass("3 quietly abORc=B 12or3=1 on3pass")
                .pass("3 quietly on3pass 12or3=1 abORc=c")
                .pass("1 qronicly abORc=c 12or3=1")
                .pass("2 qronicly 12or3=1 abORc=c")
                .pass("3 qronicly on3pass 12or3=1 abORc=c")
                .pass("3 verbose task3\\[ on3pass\\] Task1\\[ abORc=c 12or3=1\\] TaskTwo\\[\\]")
                .pass("1 verbose Task1\\[ 12or3=1 abORc=c\\]")
                .pass("4 verbose TaskTwo\\[\\] task3\\[ on3pass\\] Task1double\\[ 12or3=1 abORc=c\\] Task1\\[\\]")
                .pass("0 qronicly")
                .pass("3 verbose task3\\[ on3pass\\] Task1\\[ abORc=c 12or3=3\\] TaskTwo\\[\\]")
                .pass("3 verbose task3\\[ on3pass\\] Task1\\[ abORc=c 12or3=2\\] TaskTwo\\[\\]")
                .pass("1 verbose Task1\\[ 12or3=1 abORc=B\\]")
                .pass("1 verbose Task1\\[ abORc=A 12or3=1\\]")
                .pass("1 verbose Task1\\[ 12or3=1 abORc=A\\]")
                .pass("1 verbose Task1\\[ abORc=B 12or3=1\\]")
                .pass("1 verbose Task1\\[ abORc=B 12or3=3\\]")
                .pass("1 verbose Task1\\[ 12or3=3 abORc=c\\]")
                .pass("1 verbose Task1\\[ abORc=B 12or3=2\\]")
                .pass("0 quietly")
                .pass("2 verbose Task1\\[ 12or3=1 abORc=c\\] TaskTwo\\[\\]")
                .pass("3 verbose task3\\[ on3pass\\] Task1\\[ abORc=A 12or3=1\\] TaskTwo\\[\\]")
                .pass("3 verbose task3\\[ on3pass\\] Task1\\[ 12or3=1 abORc=A\\] TaskTwo\\[\\]")
                .pass("3 verbose task3\\[ on3pass\\] Task1\\[ abORc=A 12or3=2\\] TaskTwo\\[\\]")
                .pass("4 verbose TaskTwo\\[\\] task3\\[ on3pass\\] Task1double\\[ 12or3=1 abORc=B\\] Task1\\[\\]")
                .fail("4 verbose TaskTwo\\[\\] IOException\\[ on3fail\\]")
                        .withMessage("an xeption")
                .pass("4 qronicly 12or3=1 abORc=c on3pass")
                .fail("4 qronicly Task1\\[ 12or3=1 abORc=B\\] TaskTwo\\[\\] IOException\\[ on3fail\\]")
                        .withMessage("an xeption")
                .fail("test\\(\\)").withMessage("7 .*fail.*total 40.*");
    }
}
