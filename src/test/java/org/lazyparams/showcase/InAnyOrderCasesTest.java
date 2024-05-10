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
public class InAnyOrderCasesTest {

    @Rule
    public final VerifyJupiterRule expect = new VerifyJupiterRule(InAnyOrderCases.class) {

        String evaluateDigitPrefix(String nameRgx) {
            char firstCh = nameRgx.charAt(0);

            if (firstCh < '0' || '9' < firstCh) {
                return nameRgx;
            } else {
                StringBuilder sb = new StringBuilder()
                        .append(" ").append(firstCh)
                        .append(" tasks \\d\\.\\d\\d\\ds");
                if (2 <= nameRgx.length()) {
                    sb.append(" /").append(nameRgx.substring(1));
                }
                return sb.toString();
            }
        }

        @Override
        public SpecifyFailMessageOrNextResult fail(String nameRgx) {
            return super.fail(evaluateDigitPrefix(nameRgx));
        }
        @Override
        public NextResult pass(String nameRgx) {
            if (nameRgx.startsWith("InAnyOrder")) {
                return pass(nameRgx, "org\\.lazyparams\\.showcase\\." + nameRgx);
            } else {
                return super.pass(evaluateDigitPrefix(nameRgx));
            }
        }
    };

    @Test public void test() {
        expect
                .pass("0 verbose")
                .pass("0 qronicly")
                .pass("0 quietly")
                .pass("0")
                .pass("InAnyOrderCases 0 tasks \\d\\.\\d\\d\\ds")
                .pass("1 verbose Task1\\[ abORc=A 12or3=1\\]")
                .pass("1 qronicly 12or3=2 abORc=B")
                .pass("1 quietly abORc=c 12or3=3")
                .pass("1 verbose Task1\\[ abORc=c 12or3=2\\]")
                .pass("1 qronicly 12or3=2 abORc=A")
                .pass("1 quietly abORc=A 12or3=2")
                .pass("1 qronicly 12or3=2 abORc=c")
                .pass("1 verbose Task1\\[ abORc=A 12or3=3\\]")
                .pass("1 quietly abORc=c 12or3=1")
                .pass("1 qronicly 12or3=1 abORc=B")
                .pass("1 qronicly 12or3=1 abORc=A")
                .pass("1 qronicly 12or3=1 abORc=c")
                .pass("1 verbose Task1\\[ abORc=B 12or3=3\\]")
                .pass("1 quietly abORc=B 12or3=2")
                .pass("1 qronicly 12or3=3 abORc=B")
                .pass("1 qronicly 12or3=3 abORc=A")
                .pass("1 verbose Task1\\[ abORc=B 12or3=1\\]")
                .pass("1 qronicly 12or3=3 abORc=c")
                .pass("1")
                .pass("InAnyOrderCases 1 tasks \\d\\.\\d\\d\\ds")
                .pass("2 verbose Task1\\[ abORc=A 12or3=1\\] TaskTwo\\[\\]")
                .pass("2 qronicly 12or3=2 abORc=B")
                .pass("2 quietly 12or3=3 abORc=c")
                .pass("2 verbose Task1\\[ abORc=A 12or3=2\\] TaskTwo\\[\\]")
                .pass("2 qronicly 12or3=3 abORc=A")
                .pass("2 quietly 12or3=2 abORc=A")
                .pass("2 verbose Task1\\[ abORc=A 12or3=3\\] TaskTwo\\[\\]")
                .pass("2 qronicly 12or3=2 abORc=c")
                .pass("2 quietly 12or3=3 abORc=B")
                .pass("2 verbose Task1\\[ abORc=B 12or3=1\\] TaskTwo\\[\\]")
                .pass("2 verbose Task1\\[ abORc=B 12or3=2\\] TaskTwo\\[\\]")
                .pass("2 verbose Task1\\[ abORc=B 12or3=3\\] TaskTwo\\[\\]")
                .pass("2 qronicly 12or3=1 abORc=c")
                .pass("2 quietly 12or3=1 abORc=B")
                .pass("2 verbose Task1\\[ abORc=c 12or3=1\\] TaskTwo\\[\\]")
                .pass("2 verbose Task1\\[ abORc=c 12or3=2\\] TaskTwo\\[\\]")
                .pass("2 qronicly 12or3=1 abORc=A")
                .pass("2 verbose Task1\\[ abORc=c 12or3=3\\] TaskTwo\\[\\]")
                .pass("2")
                .pass("InAnyOrderCases 2 tasks 0\\.\\d\\d\\ds")
                .pass("3 verbose Task1\\[ abORc=A 12or3=1\\] TaskTwo\\[\\] task3\\[ on3pass\\]")
                .fail("3 qronicly IOException\\[ on3fail\\]")
                        .withMessage("an xeption")
                .fail("3 quietly on3fail")
                        .withMessage("an xeption")
                .fail("3 verbose Task1\\[ 12or3=2 abORc=B\\] TaskTwo\\[\\] IOException\\[ on3fail\\]")
                        .withMessage("an xeption")
                .pass("3 verbose Task1\\[ abORc=c 12or3=3\\] TaskTwo\\[\\] task3\\[ on3pass\\]")
                .pass("3 qronicly on3pass 12or3=2 abORc=A")
                .pass("3 qronicly on3pass abORc=c 12or3=2")
                .pass("3 qronicly on3pass 12or3=2 abORc=c")
                .pass("3 qronicly on3pass abORc=A 12or3=3")
                .pass("3 qronicly on3pass 12or3=1 abORc=A")
                .pass("3 qronicly on3pass abORc=c 12or3=1")
                .pass("3 qronicly on3pass 12or3=1 abORc=B")
                .pass("3 quietly on3pass abORc=A 12or3=1")
                .pass("3 quietly on3pass abORc=A 12or3=2")
                .pass("3 quietly on3pass abORc=c 12or3=1")
                .pass("3 quietly on3pass abORc=B 12or3=1")
                .pass("3 quietly on3pass abORc=B 12or3=3")
                .fail("3 verbose Task1\\[ 12or3=1 abORc=B\\] TaskTwo\\[\\] IOException\\[ on3fail\\]")
                        .withMessage("an xeption")
                .pass("3 verbose Task1\\[ 12or3=1 abORc=B\\] TaskTwo\\[\\] task3\\[ on3pass\\]")
                .fail("3 verbose Task1\\[ 12or3=3 abORc=c\\] TaskTwo\\[\\] IOException\\[ on3fail\\]")
                        .withMessage("an xeption")
                .pass("3 verbose Task1\\[ 12or3=3 abORc=B\\] TaskTwo\\[\\] task3\\[ on3pass\\]")
                .fail("3 verbose Task1\\[ 12or3=1 abORc=A\\] TaskTwo\\[\\] IOException\\[ on3fail\\]")
                        .withMessage("an xeption")
                .pass("3 verbose Task1\\[ 12or3=2 abORc=B\\] TaskTwo\\[\\] task3\\[ on3pass\\]")
                .pass("3 verbose Task1\\[ abORc=B 12or3=3\\] TaskTwo\\[\\] task3\\[ on3pass\\]")
                .fail("3 verbose Task1\\[ 12or3=3 abORc=B\\] TaskTwo\\[\\] IOException\\[ on3fail\\]")
                        .withMessage("an xeption")
                .pass("3 verbose Task1\\[ abORc=B 12or3=1\\] TaskTwo\\[\\] task3\\[ on3pass\\]")
                .pass("3 verbose Task1\\[ 12or3=3 abORc=A\\] TaskTwo\\[\\] task3\\[ on3pass\\]")
                .fail("3 verbose Task1\\[ abORc=B 12or3=1\\] TaskTwo\\[\\] IOException\\[ on3fail\\]")
                        .withMessage("an xeption")
                .pass("3 verbose Task1\\[ abORc=B 12or3=2\\] TaskTwo\\[\\] task3\\[ on3pass\\]")
                .pass("3 qronicly on3pass 12or3=1 abORc=c")
                .fail("3").withMessage("8 test.*fail.*total 30.*")
                .pass("InAnyOrderCases 3 tasks 0\\.\\d\\d\\ds")
                .pass("4 verbose Task1\\[ abORc=A 12or3=1\\] TaskTwo\\[\\] task3\\[ on3pass\\] Task1double\\[\\]")
                .pass("4 qronicly 12or3=2 abORc=B on3pass")
                .fail("4 quietly on3fail")
                .fail("4 verbose TaskTwo\\[\\] IOException\\[ on3fail\\]")
                .fail("4 qronicly Task1double\\[ 12or3=3 abORc=c\\] Task1\\[\\] TaskTwo\\[\\] IOException\\[ on3fail\\]")
                .fail("4 verbose Task1\\[ abORc=A 12or3=2\\] TaskTwo\\[\\] IOException\\[ on3fail\\]")
                .pass("4 verbose Task1\\[ abORc=c 12or3=3\\] TaskTwo\\[\\] task3\\[ on3pass\\] Task1double\\[\\]")
                .fail("4 qronicly Task1double\\[ 12or3=2 abORc=c\\] Task1\\[\\] TaskTwo\\[\\] IOException\\[ on3fail\\]")
                .pass("4 qronicly 12or3=3 abORc=c on3pass")
                .pass("4 qronicly 12or3=3 abORc=c on3pass")
                .fail("4 qronicly Task1double\\[ 12or3=3 abORc=c\\] Task1\\[\\] TaskTwo\\[\\] IOException\\[ on3fail\\]")
                .fail("4 qronicly Task1double\\[ 12or3=3 abORc=A\\] Task1\\[\\] TaskTwo\\[\\] IOException\\[ on3fail\\]")
                .fail("4 verbose Task1\\[ abORc=c 12or3=1\\] TaskTwo\\[\\] IOException\\[ on3fail\\]")
                .pass("4 qronicly 12or3=2 abORc=A on3pass")
                .pass("4 verbose TaskTwo\\[\\] task3\\[ on3pass\\] Task1double\\[ 12or3=3 abORc=B\\] Task1\\[\\]")
                .pass("4 qronicly 12or3=1 abORc=c on3pass")
                .pass("4 quietly on3pass abORc=A 12or3=1")
                .pass("4 quietly on3pass abORc=c 12or3=1")
                .pass("4 quietly on3pass abORc=B 12or3=1")
                .pass("4 quietly on3pass abORc=B 12or3=1")
                .pass("4 quietly on3pass abORc=B 12or3=2")
                .pass("4 quietly on3pass abORc=B 12or3=3")
                .fail("4 qronicly Task1double\\[ 12or3=1 abORc=B\\] Task1\\[\\] TaskTwo\\[\\] IOException\\[ on3fail\\]")
                .pass("4 verbose TaskTwo\\[\\] task3\\[ on3pass\\] Task1double\\[ abORc=A 12or3=1\\] Task1\\[\\]")
                .pass("4 verbose TaskTwo\\[\\] task3\\[ on3pass\\] Task1double\\[ abORc=c 12or3=1\\] Task1\\[\\]")
                .pass("4 quietly on3pass abORc=A 12or3=3")
                .pass("4 verbose Task1\\[ 12or3=1 abORc=c\\] TaskTwo\\[\\] task3\\[ on3pass\\] Task1double\\[\\]")
                .pass("4 verbose TaskTwo\\[\\] task3\\[ on3pass\\] Task1double\\[ 12or3=1 abORc=c\\] Task1\\[\\]")
                .fail("4 verbose Task1\\[ 12or3=1 abORc=c\\] TaskTwo\\[\\] IOException\\[ on3fail\\]")
                .pass("4 verbose TaskTwo\\[\\] task3\\[ on3pass\\] Task1double\\[ abORc=c 12or3=2\\] Task1\\[\\]")
                .fail("4 qronicly Task1double\\[ 12or3=1 abORc=A\\] Task1\\[\\] TaskTwo\\[\\] IOException\\[ on3fail\\]")
                .fail("4").withMessage("11 .*fail.*total 31.*")
                .pass("InAnyOrderCases 4 tasks 0\\.\\d\\d\\ds");
    }
}
