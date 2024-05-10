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
public class StaticAndQuietVintageTest {

    @Rule
    public VerifyVintageRule expect = new VerifyVintageRule(StaticAndQuietVintage.class)
            .expectSkipMessageOnParametersInStaticScope();

    @Test
    public void onlyPrimaryValuePickedInStaticScope() {
        expect.pass("");
    }

    @Test
    public void withQuietPick() {
        expect
                .pass(" pick=1 extra false")
                .fail(" Fail on #2 pick=2 extra false")
                        .withMessage("Quiet bool.*")
                .pass(" pick=3 extra true")
                .pass(" pick=2 extra true")
                .pass(" pick=1 extra true")
                .pass(" pick=3 extra false")
                .pass(" pick=2 extra false")
                .fail("").withMessage(".*1.*fail.*total 7.*");
    }
}
