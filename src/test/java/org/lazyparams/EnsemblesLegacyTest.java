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
public class EnsemblesLegacyTest {

    @Rule
    public final VerifyVintageRule expect = new VerifyVintageRule(EnsemblesLegacy.class);

    @Test
    public void test2fail() {
        expect.fail("test2fail [01]\\.\\d{3}s throw=false of=class java.sql.SQLException")
                .withMessage("Intentional.*")
                .pass("test2fail 0.0\\d\\ds throw=true of=class java.io.IOException")
                .fail("test2fail").withMessage("1 .*fail.*total 2.*");
    }

    @Test
    public void testMaxMin() {
        expect.fail("testMaxMin [01]\\.\\d{3}s max-options=\\[12000000032, 12313213, 12000000032.78\\] max-pick=12000000032.78")

                .pass("testMaxMin 0.0\\d\\ds max-options=\\[1234567890321, 12300892, 1234567890320.99\\]"
                + " max-pick=1234567890321 min-options=\\[-999999, -12389012380, 12000000032.78\\] min-pick=-12389012380")

                .pass("testMaxMin 0.0\\d\\ds max-options=\\[987000122999, 987000123000, -123000321092.3432\\]"
                + " max-pick=987000123000 min-options=\\[-123000321092, -123000321092, -123000321092.3432\\] min-pick=-123000321092.3432")

                .pass("testMaxMin 0.0\\d\\ds max-options=\\[1234567890321, 12300892, 1234567890320.99\\]"
                + " max-pick=1234567890321 min-options=\\[-3423523.2314, -3423523, -1239032\\] min-pick=-3423523.2314")

                .pass("testMaxMin 0.0\\d\\ds max-options=\\[987000122999, 987000123000, -123000321092.3432\\]"
                + " max-pick=987000123000 min-options=\\[-999999, -12389012380, 12000000032.78\\] min-pick=-12389012380")

                .pass("testMaxMin 0.0\\d\\ds max-options=\\[1234567890321, 12300892, 1234567890320.99\\]"
                + " max-pick=1234567890321 min-options=\\[-123000321092, -123000321092, -123000321092.3432\\] min-pick=-123000321092.3432")

                .pass("testMaxMin 0.0\\d\\ds max-options=\\[987000122999, 987000123000, -123000321092.3432\\]"
                + " max-pick=987000123000 min-options=\\[-3423523.2314, -3423523, -1239032\\] min-pick=-3423523.2314")
                .fail("testMaxMin").withMessage("1 .*fail.*total 7.*");
    }

    @Test
    public void testDuo() {
        expect.pass("testDuo [01]\\.\\d{3}s finalStr=FOO Final string value=FOO")
                .pass("testDuo 0\\.0\\d\\ds finalStr=bar Final string value=bar")
                .pass("testDuo 0\\.0\\d\\ds finalStr=Hmm Final string value=Hmm")
                .pass("testDuo");
    }

    @Test
    public void testArrayParam() {
        expect.fail("testArrayParam [01]\\.\\d{3}s x-int=1 number=23 order=1st array=\\[42, 2nd\\]")
                        .withMessage(".*expect.+3rd.+was.+1st.*")
                .pass("testArrayParam 0\\.0\\d\\ds x-int=2 number=42 order=3rd array=\\[23, 4th\\]")
                .pass("testArrayParam 0\\.0\\d\\ds x-int=3 number=23 order=1st array=\\[42, 2nd\\]")
                .pass("testArrayParam 0\\.0\\d\\ds x-int=1 number=42 order=3rd array=\\[23, 4th\\]")
                .pass("testArrayParam 0\\.0\\d\\ds x-int=2 number=23 order=1st array=\\[42, 2nd\\]")
                .pass("testArrayParam 0\\.0\\d\\ds x-int=3 number=42 order=3rd array=\\[23, 4th\\]")
                .fail("testArrayParam").withMessage("1 .*fail.*total 6.*");
    }

    @Test
    public void testComplicatedFunctionInstance() {
        expect.pass("testComplicatedFunctionInstance [01]\\.\\d{3}s n1=1 n2=2 n3=3 n4=4 expect=4 with being inspected=\\[4, 4\\]")
                .pass("testComplicatedFunctionInstance 0\\.0\\d\\ds n1=5 n2=6 n3=7 n4=8 expect=8 with being inspected=\\[8, 8\\]")
                .pass("testComplicatedFunctionInstance");
    }
}
