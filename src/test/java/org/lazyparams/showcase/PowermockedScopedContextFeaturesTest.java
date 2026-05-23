/*
 * Copyright 2026 the original author or authors.
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
import org.lazyparams.VerifyVintageRule;

/**
 * @author Henrik Kaipe
 */
public class PowermockedScopedContextFeaturesTest {

    @Rule
    public VerifyVintageRule expect = new VerifyVintageRule(PowermockedScopedContextFeatures.class);

    @Test
    public void threeParamsFullyCombined() {
        expect.pass(" a=1 b=1 c=1")
                .pass(" a=2 b=2 c=2")
                .pass(" a=1 b=2 c=2")
                .pass(" a=1 b=1 c=2")
                .pass(" a=2 b=1 c=1")
                .pass(" a=2 b=2 c=1")
                .pass(" a=1 b=2 c=1")
                .pass(" a=2 b=1 c=2")
                .pass("");
    }

    @Test
    public void scopeCachedValue() {
        expect.pass(" built=first").pass(" built=second").pass("");
    }

    @Test
    public void runPermutation() {
        expect.pass(" A->B->C")
                .pass(" C->A->B")
                .pass(" B->C->A")
                .pass("");
    }
}
