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
public class RegularLegacyTest {

    @Rule
    public final VerifyVintageRule expectRule =
            new VerifyVintageRule(RegularLegacy.class);

    @Test
    public void testNormal() {
        expectRule
                .pass("testNormal 1st=34 2nd=sdf")
                .pass("testNormal 1st=42 2nd=dfwe")
                .fail("testNormal 1st=34 2nd=dfwe")
                        .withMessage("Fail here")
                .pass("testNormal 1st=42 2nd=sdf")
                .fail("testNormal")
                        .withMessage("1 test failed.*total 4.*");
    }

    @Test
    public void testNoParams() {
        expectRule.pass("testNoParams");
    }

    @Test
    public void testFailWithoutParams() {
        expectRule.fail("testFailWithoutParams")
                .withMessage("FAiLURE");
    }

    @Test
    public void testManyParameters() {
        expectRule
                .pass(" 1st=34 2nd=sdf nbr=1 boolean=false")
                .pass(" 1st=42 2nd=dfwe nbr=2 boolean=false")
                .pass(" 1st=34 2nd=dfwe")
                .fail(" 1st=42 2nd=sdf nbr=3 boolean=true")
                        .withMessage(".*mix.*2.*but.*3.*")
                .pass(" 1st=34 2nd=sdf nbr=2 boolean=true")
                .fail(" 1st=42 2nd=dfwe nbr=1 boolean=true")
                        .withMessage(".*mix.*2.*but.*1.*")
                .pass(" 1st=34 2nd=sdf nbr=3 boolean=false")
                .pass(" 1st=42 2nd=dfwe nbr=3 boolean=false")
                .fail("")
                        .withMessage("2 tests failed.*total 8.*");
    }
}
