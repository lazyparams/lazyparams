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

/**
 * @author Henrik Kaipe
 */
public class DisplayStaticTest {

    private static final String timeRgx = " 0\\.\\d{3}s";

    @Rule
    public VerifyJupiterRule expect = new VerifyJupiterRule(DisplayStatic.class) {

        @Override
        public VerifyJupiterRule.SpecifyFailMessageOrNextResult fail(String nameRgx) {
            return super.fail(timeRgx + " /" + nameRgx);
        }
        @Override
        public VerifyJupiterRule.NextResult pass(String nameRgx) {
            return super.pass(timeRgx + " /" + nameRgx);
        }
        @Override
        protected void customizeBeforeLaunch() {
            pass(DisplayStatic.class.getSimpleName() + timeRgx,
                    DisplayStatic.class.getName() + timeRgx);
        }
    };

    @Test
    public void regularWithoutParameter() {
        expect.pass("");
    }

    @Test
    public void displaySomething() {
        expect.pass("displayed");
    }

    @Test
    public void singleValueParameter() {
        expect.pass(" The 1");
    }

    @Test
    public void valuesX3() {
        expect
                .pass(" *")
                .fail(" Draw").withMessage(".*X.*")
                .pass(" *")
                .fail("valuesX3(\\(\\))?" + timeRgx, "valuesX3(\\(\\))?" + timeRgx)
                        .withMessage(".*1.*fail.*total 3.*");
    }
}
