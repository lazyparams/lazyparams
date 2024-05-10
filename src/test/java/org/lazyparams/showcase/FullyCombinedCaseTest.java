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
public class FullyCombinedCaseTest {

    @Rule
    public VerifyJupiterRule expect = new VerifyJupiterRule(FullyCombinedCase.class);

    @Test
    public void pairwise1234fromBasicFactoryWithExplicitId() {
        expect.pass(" c=1").pass(" c=2").pass(" c=3").pass(" c=4").pass("");
    }

    @Test
    public void pairwise1234() {
        expect.pass(" a=1 b=1 c=1")
                .pass(" a=2 b=2 c=2")
                .pass(" a=3 b=3 c=3")
                .pass(" a=4 b=4 c=4")
                .pass(" a=1 b=2 c=3")
                .pass(" a=3 b=4 c=2")
                .pass(" a=2 b=1 c=4")
                .pass(" a=4 b=3 c=1")
                .pass(" a=1 b=3 c=4")
                .pass(" a=2 b=4 c=3")
                .pass(" a=3 b=1 c=2")
                .pass(" a=4 b=2 c=1")
                .pass(" a=3 b=2 c=4")
                .pass(" a=4 b=1 c=3")
                .pass(" a=1 b=4 c=2")
                .pass(" a=2 b=3 c=1")
                .pass(" a=3 b=3 c=1")/*almost same as the one right before :-( */
                .pass(" a=4 b=4 c=2")
                .pass(" a=1 b=3 c=2")
                .pass(" a=2 b=4 c=1")
                .pass("");
    }

    @Test
    public void repeat_a_x5() {
        expect.pass(" a=1 a=1 a=1 a=1 a=1")
                .pass(" a=2 a=2 a=1 a=2 a=2")
                .pass(" a=1 a=2 a=2 a=1 a=2")
                .pass(" a=2 a=1 a=2 a=2 a=1")
                .pass(" a=1 a=1 a=1 a=2 a=2")
                .pass(" a=2 a=2 a=2 a=1 a=1")
                .pass("");
    }

    @Test
    public void cartesian12() {
        expect.fail(" a=1 b=1 c=1 d=1 e=1 f=1").withMessage("Need to fail a little.*")
                .pass(" a=2 b=2 c=2 d=2 e=2 f=2")
                .pass(" a=1 b=2 c=2 d=2 e=2 f=2")
                .pass(" a=1 b=1 c=2 d=2 e=2 f=2")
                .pass(" a=1 b=1 c=1 d=2 e=2 f=2")
                .pass(" a=1 b=1 c=1 d=1 e=2 f=2")
                .pass(" a=1 b=1 c=1 d=1 e=1 f=2")
                .pass(" a=1 b=1 c=1 d=2 e=1 f=1")
                .pass(" a=1 b=1 c=1 d=2 e=2 f=1")
                .pass(" a=1 b=1 c=2 d=1 e=1 f=1")
                .pass(" a=1 b=1 c=2 d=2 e=1 f=1")
                .pass(" a=1 b=1 c=2 d=2 e=2 f=1")
                .pass(" a=1 b=1 c=2 d=1 e=2 f=2")
                .pass(" a=1 b=1 c=2 d=1 e=1 f=2")
                .pass(" a=1 b=1 c=1 d=1 e=2 f=1")
                .pass(" a=1 b=2 c=1 d=1 e=1 f=1")
                .pass(" a=1 b=2 c=2 d=1 e=1 f=1")
                .pass(" a=1 b=2 c=2 d=2 e=1 f=1")
                .pass(" a=1 b=2 c=2 d=2 e=2 f=1")
                .pass(" a=1 b=2 c=2 d=1 e=2 f=2")
                .pass(" a=1 b=2 c=2 d=1 e=1 f=2")
                .pass(" a=1 b=2 c=1 d=2 e=2 f=2")
                .pass(" a=1 b=2 c=1 d=1 e=2 f=2")
                .pass(" a=1 b=2 c=1 d=1 e=1 f=2")
                .pass(" a=1 b=2 c=1 d=2 e=1 f=1")
                .pass(" a=1 b=2 c=1 d=2 e=2 f=1")
                .pass(" a=1 b=2 c=2 d=2 e=1 f=2")
                .pass(" a=1 b=1 c=2 d=2 e=1 f=2")
                .pass(" a=1 b=2 c=1 d=1 e=2 f=1")
                .pass(" a=2 b=1 c=1 d=1 e=1 f=1")
                .pass(" a=2 b=2 c=1 d=1 e=1 f=1")
                .pass(" a=2 b=2 c=2 d=1 e=1 f=1")
                .pass(" a=2 b=2 c=2 d=2 e=1 f=1")
                .pass(" a=2 b=2 c=2 d=2 e=2 f=1")
                .pass(" a=2 b=2 c=2 d=1 e=2 f=2")
                .pass(" a=2 b=2 c=2 d=1 e=1 f=2")
                .pass(" a=2 b=2 c=1 d=2 e=2 f=2")
                .pass(" a=2 b=2 c=1 d=1 e=2 f=2")
                .pass(" a=2 b=2 c=1 d=1 e=1 f=2")
                .pass(" a=2 b=2 c=1 d=2 e=1 f=1")
                .pass(" a=2 b=2 c=1 d=2 e=2 f=1")
                .pass(" a=2 b=2 c=2 d=2 e=1 f=2")
                .pass(" a=2 b=1 c=2 d=2 e=2 f=2")
                .pass(" a=2 b=1 c=1 d=2 e=2 f=2")
                .pass(" a=2 b=1 c=1 d=1 e=2 f=2")
                .pass(" a=2 b=1 c=1 d=1 e=1 f=2")
                .pass(" a=2 b=1 c=1 d=2 e=1 f=1")
                .pass(" a=2 b=1 c=1 d=2 e=2 f=1")
                .pass(" a=2 b=1 c=2 d=1 e=1 f=1")
                .pass(" a=2 b=1 c=2 d=2 e=1 f=1")
                .pass(" a=2 b=1 c=2 d=2 e=2 f=1")
                .pass(" a=2 b=1 c=2 d=1 e=2 f=2")
                .pass(" a=2 b=1 c=2 d=1 e=1 f=2")
                .pass(" a=2 b=1 c=1 d=1 e=2 f=1")
                .pass(" a=2 b=2 c=1 d=1 e=2 f=1")
                .pass(" a=2 b=1 c=2 d=2 e=1 f=2")
                .pass(" a=1 b=1 c=1 d=2 e=1 f=2")
                .pass(" a=2 b=2 c=2 d=1 e=2 f=1")
                .pass(" a=1 b=2 c=2 d=1 e=2 f=1")
                .pass(" a=2 b=1 c=1 d=2 e=1 f=2")
                .pass(" a=1 b=1 c=2 d=1 e=2 f=1")
                .pass(" a=2 b=2 c=1 d=2 e=1 f=2")
                .pass(" a=1 b=2 c=1 d=2 e=1 f=2")
                .pass(" a=2 b=1 c=2 d=1 e=2 f=1")
                .fail("").withMessage(".*1 test.*fail.*total 64.*");
    }

    @Test
    public void pairwise12() {
        expect.pass(" a=1 b=1 c=1 d=1 e=1 f=1 g=1 h=1")
                .pass(" a=2 b=2 c=1 d=2 e=2 f=2 g=1 h=2")
                .pass(" a=1 b=2 c=2 d=1 e=2 f=2 g=2 h=1")
                .pass(" a=2 b=1 c=2 d=2 e=1 f=1 g=2 h=2")
                .pass(" a=1 b=1 c=1 d=2 e=2 f=2 g=2 h=1")
                .pass(" a=2 b=2 c=2 d=1 e=1 f=1 g=1 h=2")
                .pass(" a=1 b=2 c=1 d=2 e=1 f=2 g=2 h=2")
                .pass(" a=2 b=1 c=2 d=1 e=2 f=1 g=1 h=1")
                .pass("");
    }

    @Test
    public void mixed12() {
        expect.pass(" a=1 b=1 c=1 d=1 e=1 f=1")
                .fail(" a=2 b=2 c=1 d=2 e=2 f=2")
                        .withMessage("Fail a little")
                .pass(" a=1 b=2 c=2 d=2 e=1 f=2")
                .pass(" a=2 b=1 c=2 d=1 e=2 f=1")
                .pass(" a=1 b=1 c=1 d=2 e=2 f=2")
                .pass(" a=2 b=2 c=2 d=2 e=1 f=1")
                .pass(" a=2 b=2 c=1 d=1 e=2 f=2")
                .pass(" a=1 b=2 c=2 d=1 e=1 f=2")
                .pass(" a=2 b=1 c=2 d=2 e=2 f=1")
                .pass(" a=1 b=1 c=1 d=2 e=1 f=1")
                .fail("").withMessage(".*1 test.*fail.*total 10.*");
    }

    @Test
    public void failOn21() {
        expect.pass(" a=1 b=1 c=1 d=1 e=1 f=1")
                .pass(" a=2 b=2 c=1 d=2 e=2 f=2")
                .pass(" a=1 b=2 c=2 d=1 e=2 f=2")
                .fail(" a=2 b=1").withMessage("Early failure")
                .pass(" a=2 b=2 c=2 d=2 e=1 f=1")
                .pass(" a=1 b=1 c=2 d=2 e=2 f=2")
                .pass(" a=2 b=2 c=1 d=1 e=1 f=2")
                .pass(" a=1 b=2 c=1 d=2 e=2 f=1")
                .fail("").withMessage(".*1 test.*fail.*total 8.*");
    }

    @Test
    public void separateProducts() {
        expect.pass(" a=1 b=1 c=1 d=1 e=1 f=1")
                .fail(" QRONIC FAILURE a=2 b=2 c=1 d=2 e=2 f=2")
                        .withMessage("Unlucky 7")
                .pass(" a=1 b=2 c=2 d=2 e=2 f=1")
                .pass(" a=2 b=1 c=2 d=1 e=1 f=2")
                .pass(" a=1 b=1 c=2 d=1 e=2 f=2")
                .pass(" a=2 b=2 c=1 d=1 e=1 f=2")
                .pass(" a=2 b=1 c=1 d=2 e=1 f=1")
                .pass(" a=1 b=1 c=1 d=2 e=2 f=1")
                .pass(" a=2 b=1 c=2 d=2 e=1 f=1")
                .pass(" a=1 b=2 c=1 d=1 e=2 f=2")
                .pass(" a=1 b=2 c=1 d=2 e=1 f=2")
                .pass(" a=2 b=2 c=2 d=2 e=2 f=1")
                .pass(" a=1 b=2 c=2 d=1 e=1 f=1")
                .fail(" QRONIC FAILURE a=2 b=2 c=2 d=2 e=1 f=2")
                        .withMessage("Unlucky 7")
                .fail(" QRONIC FAILURE a=1 b=1 c=2 d=2 e=2 f=2")
                        .withMessage("Unlucky 7")
                .pass(" a=2 b=1 c=1 d=1 e=2 f=1")
                .pass(" a=2 b=2 c=1 d=1 e=2 f=2")
                .fail(" QRONIC FAILURE a=2 b=2 c=2 d=1 e=2 f=1")
                        .withMessage("Unlucky 7")
                .pass(" a=2 b=1 c=1 d=1 e=1 f=2")
                .pass(" a=2 b=1 c=1 d=1 e=1 f=1")
                .pass(" a=2 b=2 c=1 d=1 e=1 f=1")
                .pass(" a=1 b=1 c=1 d=1 e=1 f=2")
                .pass(" a=2 b=1 c=1 d=1 e=2 f=2")
                .fail("").withMessage(".*4 test.*fail.*total 23.*");
    }

    @Test
    public void enums() {
        expect.pass(" SOURCE SCIENTIFIC SOURCE final_layout=SCIENTIFIC")
                .pass(" CLASS DECIMAL_FLOAT CLASS final_layout=DECIMAL_FLOAT")
                .pass(" RUNTIME SCIENTIFIC RUNTIME final_layout=SCIENTIFIC")
                .pass(" SOURCE DECIMAL_FLOAT CLASS final_layout=DECIMAL_FLOAT")
                .pass(" SOURCE SCIENTIFIC CLASS final_layout=DECIMAL_FLOAT")
                .pass(" SOURCE SCIENTIFIC RUNTIME final_layout=SCIENTIFIC")
                .pass(" SOURCE SCIENTIFIC SOURCE final_layout=DECIMAL_FLOAT")
                .pass(" SOURCE DECIMAL_FLOAT RUNTIME final_layout=SCIENTIFIC")
                .pass(" SOURCE DECIMAL_FLOAT SOURCE final_layout=DECIMAL_FLOAT")
                .pass(" SOURCE DECIMAL_FLOAT CLASS final_layout=SCIENTIFIC")
                .pass(" SOURCE SCIENTIFIC CLASS final_layout=SCIENTIFIC")
                .pass(" CLASS SCIENTIFIC RUNTIME final_layout=SCIENTIFIC")
                .pass(" CLASS DECIMAL_FLOAT RUNTIME final_layout=SCIENTIFIC")
                .pass(" CLASS DECIMAL_FLOAT SOURCE final_layout=DECIMAL_FLOAT")
                .pass(" CLASS DECIMAL_FLOAT CLASS final_layout=SCIENTIFIC")
                .pass(" CLASS SCIENTIFIC SOURCE final_layout=DECIMAL_FLOAT")
                .pass(" CLASS SCIENTIFIC CLASS final_layout=SCIENTIFIC")
                .pass(" CLASS SCIENTIFIC RUNTIME final_layout=DECIMAL_FLOAT")
                .pass(" CLASS DECIMAL_FLOAT RUNTIME final_layout=DECIMAL_FLOAT")
                .pass(" RUNTIME DECIMAL_FLOAT SOURCE final_layout=DECIMAL_FLOAT")
                .pass(" RUNTIME SCIENTIFIC SOURCE final_layout=DECIMAL_FLOAT")
                .pass(" RUNTIME SCIENTIFIC CLASS final_layout=SCIENTIFIC")
                .pass(" RUNTIME SCIENTIFIC RUNTIME final_layout=DECIMAL_FLOAT")
                .pass(" RUNTIME DECIMAL_FLOAT CLASS final_layout=SCIENTIFIC")
                .pass(" RUNTIME DECIMAL_FLOAT RUNTIME final_layout=DECIMAL_FLOAT")
                .pass(" RUNTIME DECIMAL_FLOAT SOURCE final_layout=SCIENTIFIC")
                .pass(" RUNTIME SCIENTIFIC SOURCE final_layout=SCIENTIFIC")
                .pass(" SOURCE DECIMAL_FLOAT RUNTIME final_layout=DECIMAL_FLOAT")
                .pass(" CLASS SCIENTIFIC SOURCE final_layout=SCIENTIFIC")
                .pass(" RUNTIME DECIMAL_FLOAT CLASS final_layout=DECIMAL_FLOAT")
                .pass(" SOURCE SCIENTIFIC RUNTIME final_layout=DECIMAL_FLOAT")
                .pass(" CLASS DECIMAL_FLOAT SOURCE final_layout=SCIENTIFIC")
                .pass(" RUNTIME SCIENTIFIC CLASS final_layout=DECIMAL_FLOAT")
                .pass(" SOURCE DECIMAL_FLOAT SOURCE final_layout=SCIENTIFIC")
                .pass(" CLASS SCIENTIFIC CLASS final_layout=DECIMAL_FLOAT")
                .pass(" RUNTIME DECIMAL_FLOAT RUNTIME final_layout=SCIENTIFIC")
                .pass("");
    }
}
