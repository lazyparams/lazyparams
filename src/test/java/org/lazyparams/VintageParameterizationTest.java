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
public class VintageParameterizationTest {

    @Rule
    public VerifyVintageRule expect = new VerifyVintageRule(VintageParameterization.class);

    @Test
    public void test() {
        expect.fail("test\\[Parameterized=42\\] 0\\.\\d{3}s value=90")
                .pass("test\\[Parameterized=42\\] 0\\.0\\d\\ds pickWithParameterized\\? value=90 final")
                .pass("test\\[Parameterized=42\\] 0\\.0\\d\\ds pickWithParameterized\\? value=42 middle")
                .pass("test\\[Parameterized=42\\] 0\\.0\\d\\ds value=91 middle final")
                .fail("test\\[Parameterized=42\\] 0\\.0\\d\\ds pickWithParameterized\\? value=91")
                .pass("test\\[Parameterized=42\\] 0\\.0\\d\\ds pickWithParameterized\\? value=90 middle")
                .pass("test\\[Parameterized=42\\] 0\\.0\\d\\ds value=90 middle final")
                .pass("test\\[Parameterized=42\\] 0\\.0\\d\\ds pickWithParameterized\\? value=42 final")
                .fail("test\\[Parameterized=42\\] 0\\.\\d{3}s value=91 *")
                .pass("test\\[Parameterized=42\\] 0\\.0\\d\\ds pickWithParameterized\\? +value=91 middle final")
                .fail("test\\[Parameterized=42\\]").withMessage(".*3 .*fail.*total 10.*");
        expect.fail("test\\[Parameterized=24\\] 0\\.\\d{3}s +value=90")
                .pass("test\\[Parameterized=24\\] 0\\.0\\d\\ds pickWithParameterized\\? value=90 final")
                .pass("test\\[Parameterized=24\\] 0\\.0\\d\\ds pickWithParameterized\\? value=24 middle")
                .pass("test\\[Parameterized=24\\] 0\\.0\\d\\ds value=91 middle final")
                .fail("test\\[Parameterized=24\\] 0\\.0\\d\\ds pickWithParameterized\\? value=91")
                .pass("test\\[Parameterized=24\\] 0\\.0\\d\\ds pickWithParameterized\\? value=90 middle")
                .pass("test\\[Parameterized=24\\] 0\\.0\\d\\ds value=90 middle final")
                .pass("test\\[Parameterized=24\\] 0\\.0\\d\\ds pickWithParameterized\\? value=24 final")
                .fail("test\\[Parameterized=24\\] 0\\.\\d{3}s value=91 *")
                .pass("test\\[Parameterized=24\\] 0\\.0\\d\\ds pickWithParameterized\\? +value=91 middle final")
                .fail("test\\[Parameterized=24\\]").withMessage(".*3 .*fail.*total 10.*");
    }
}
