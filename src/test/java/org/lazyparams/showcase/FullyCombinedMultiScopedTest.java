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
public class FullyCombinedMultiScopedTest {

    @Rule
    public VerifyJupiterRule expect = new VerifyJupiterRule(FullyCombinedMultiScoped.class);

    @Test
    public void test() {
        expect
                .pass(" a=1 b=1 c=1 / a=1 c=1")
                .pass(" a=1 b=1 c=1 / a=2 c=2")
                .pass(" a=1 b=1 c=1 / a=1 c=2")
                .pass(" a=1 b=1 c=1 / a=2 c=1")
                .pass(" a=1 b=1 c=1")
                .pass("FullyCombinedMultiScoped a=1 b=1 c=1",
                        "org.lazyparams.showcase.FullyCombinedMultiScoped a=1 b=1 c=1")
                .pass(" a=2 b=2 c=2 / a=1 c=1")
                .pass(" a=2 b=2 c=2 / a=2 c=2")
                .pass(" a=2 b=2 c=2 / a=1 c=2")
                .pass(" a=2 b=2 c=2 / a=2 c=1")
                .pass(" a=2 b=2 c=2")
                .pass("FullyCombinedMultiScoped a=2 b=2 c=2",
                        "org.lazyparams.showcase.FullyCombinedMultiScoped a=2 b=2 c=2")
                .pass(" a=1 b=2 c=2 / a=1 c=1")
                .pass(" a=1 b=2 c=2 / a=2 c=2")
                .pass(" a=1 b=2 c=2 / a=1 c=2")
                .pass(" a=1 b=2 c=2 / a=2 c=1")
                .pass(" a=1 b=2 c=2")
                .pass("FullyCombinedMultiScoped a=1 b=2 c=2",
                        "org.lazyparams.showcase.FullyCombinedMultiScoped a=1 b=2 c=2")
                .pass(" a=1 b=1 c=2 / a=1 c=1")
                .pass(" a=1 b=1 c=2 / a=2 c=2")
                .pass(" a=1 b=1 c=2 / a=1 c=2")
                .pass(" a=1 b=1 c=2 / a=2 c=1")
                .pass(" a=1 b=1 c=2")
                .pass("FullyCombinedMultiScoped a=1 b=1 c=2",
                        "org.lazyparams.showcase.FullyCombinedMultiScoped a=1 b=1 c=2")
                .pass(" a=2 b=1 c=1 / a=1 c=1")
                .pass(" a=2 b=1 c=1 / a=2 c=2")
                .pass(" a=2 b=1 c=1 / a=1 c=2")
                .pass(" a=2 b=1 c=1 / a=2 c=1")
                .pass(" a=2 b=1 c=1")
                .pass("FullyCombinedMultiScoped a=2 b=1 c=1",
                        "org.lazyparams.showcase.FullyCombinedMultiScoped a=2 b=1 c=1")
                .pass(" a=2 b=2 c=1 / a=1 c=1")
                .pass(" a=2 b=2 c=1 / a=2 c=2")
                .pass(" a=2 b=2 c=1 / a=1 c=2")
                .pass(" a=2 b=2 c=1 / a=2 c=1")
                .pass(" a=2 b=2 c=1")
                .pass("FullyCombinedMultiScoped a=2 b=2 c=1",
                        "org.lazyparams.showcase.FullyCombinedMultiScoped a=2 b=2 c=1")
                .pass(" a=1 b=2 c=1 / a=1 c=1")
                .pass(" a=1 b=2 c=1 / a=2 c=2")
                .pass(" a=1 b=2 c=1 / a=1 c=2")
                .pass(" a=1 b=2 c=1 / a=2 c=1")
                .pass(" a=1 b=2 c=1")
                .pass("FullyCombinedMultiScoped a=1 b=2 c=1",
                        "org.lazyparams.showcase.FullyCombinedMultiScoped a=1 b=2 c=1")
                .pass(" a=2 b=1 c=2 / a=1 c=1")
                .pass(" a=2 b=1 c=2 / a=2 c=2")
                .pass(" a=2 b=1 c=2 / a=1 c=2")
                .pass(" a=2 b=1 c=2 / a=2 c=1")
                .pass(" a=2 b=1 c=2")
                .pass("FullyCombinedMultiScoped a=2 b=1 c=2",
                        "org.lazyparams.showcase.FullyCombinedMultiScoped a=2 b=1 c=2")
                ;
    }
}
