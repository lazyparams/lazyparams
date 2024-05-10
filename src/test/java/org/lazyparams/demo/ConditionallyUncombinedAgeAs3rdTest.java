/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.demo;

import org.junit.Rule;
import org.junit.Test;
import org.lazyparams.VerifyJupiterRule;

/**
 * @author Henrik Kaipe
 */
public class ConditionallyUncombinedAgeAs3rdTest {

    @Rule
    public VerifyJupiterRule expect =
            new VerifyJupiterRule(ConditionallyUncombinedAgeAs3rd.class);

    @Test
    public void test() {
        expect
                .pass(" jv=1 age-range=ELDERLY age=91")
                .pass(" jv=2 age-range=GROWNUP age=21")
                .pass(" jv=3 age-range=ELDERLY age=91 plf=7 impl=5")
                .pass(" jv=4 age-range=GROWNUP age=22 plf=8 impl=6")
                .pass(" jv=5 age-range=YOUNG age=11 plf=9 impl=7")
                .pass(" jv=6 age-range=ELDERLY age=92 plf=10 impl=8")
                .pass(" jv=3 age-range=CHILD age=1 plf=9 impl=9")
                .pass(" jv=6 age-range=GROWNUP age=23")
                .pass(" jv=6 age-range=YOUNG age=12")
                .pass(" jv=6 age-range=CHILD age=2")
                .pass(" jv=6 age-range=ELDERLY age=93 plf=8 impl=10")
                .pass(" jv=3 age-range=ELDERLY age=94 plf=10 impl=7")
                .pass(" jv=6 age-range=ELDERLY age=95 plf=7 impl=6")
                .pass(" jv=3 age-range=ELDERLY age=96 plf=8 impl=8")
                .pass(" jv=5 age-range=ELDERLY age=97 plf=9 impl=10")
                .pass(" jv=5 age-range=GROWNUP age=24 plf=7 impl=9")
                .pass(" jv=5 age-range=CHILD age=3 plf=10 impl=5")
                .pass(" jv=5 age-range=YOUNG age=13 plf=8 impl=9")
                .pass(" jv=4 age-range=ELDERLY age=98 plf=9 impl=6")
                .pass(" jv=4 age-range=YOUNG age=14 plf=10 impl=10")
                .pass(" jv=4 age-range=CHILD age=4 plf=7 impl=7")
                .pass(" jv=4 age-range=GROWNUP age=25 plf=9 impl=5")
                .pass(" jv=6 age-range=ELDERLY age=99 plf=8 impl=7")
                .pass(" jv=4 age-range=ELDERLY age=92 plf=7 impl=9")
                .pass(" jv=5 age-range=CHILD age=5 plf=7 impl=8")
                .pass(" jv=3 age-range=YOUNG age=15 plf=10 impl=6")
                .pass(" jv=5 age-range=GROWNUP age=26 plf=10 impl=9")
                .pass(" jv=5 age-range=ELDERLY age=93 plf=9 impl=8")
                .pass(" jv=6 age-range=ELDERLY age=91 plf=8 impl=5")
                .pass(" jv=3 age-range=GROWNUP age=27 plf=7 impl=10")
                .pass(" jv=4 age-range=YOUNG age=16 plf=7 impl=8")
                .pass(" jv=5 age-range=CHILD age=6 plf=8 impl=6")
                .pass(" jv=4 age-range=ELDERLY age=95 plf=9 impl=9")
                .pass(" jv=5 age-range=GROWNUP age=28 plf=8 impl=7")
                .pass(" jv=5 age-range=ELDERLY age=99 plf=9 impl=9")
                .pass(" jv=6 age-range=ELDERLY age=94 plf=9 impl=9")
                .pass(" jv=3 age-range=YOUNG age=17 plf=9 impl=5")
                .pass(" jv=3 age-range=CHILD age=7 plf=9 impl=10")
                .pass(" jv=3 age-range=GROWNUP age=29 plf=9 impl=8")
                .pass(" jv=3 age-range=ELDERLY age=97 plf=10 impl=5")
                .pass(" jv=4 age-range=ELDERLY age=96 plf=10 impl=10")
                .pass(" jv=5 age-range=ELDERLY age=98 plf=7 impl=7")
                .pass(" jv=3 age-range=ELDERLY age=91 plf=10 impl=6")
                .pass(" jv=4 age-range=ELDERLY age=97 plf=8 impl=8")
                .pass(" jv=5 age-range=ELDERLY age=96 plf=7 impl=5")
                .pass(" jv=3 age-range=ELDERLY age=98 plf=10 impl=10")
                .pass(" jv=4 age-range=ELDERLY age=93 plf=7 impl=7")
                .pass(" jv=3 age-range=ELDERLY age=92 plf=8 impl=6")
                .pass(" jv=5 age-range=ELDERLY age=94 plf=8 impl=5")
                .pass(" jv=6 age-range=ELDERLY age=95 plf=10 impl=10")
                .pass(" jv=6 age-range=ELDERLY age=99 plf=7 impl=8")
                .pass(" jv=6 age-range=ELDERLY age=91 plf=9 impl=9")
                .pass(" jv=6 age-range=ELDERLY age=96 plf=9 impl=7")
                .pass(" jv=4 age-range=ELDERLY age=99 plf=10 impl=6")
                .pass(" jv=3 age-range=ELDERLY age=95 plf=8 impl=5")
                .pass(" jv=5 age-range=ELDERLY age=92 plf=9 impl=10")
                .pass(" jv=1 age-range=YOUNG age=18")
                .pass(" jv=2 age-range=CHILD age=8")
                .pass(" jv=5 age-range=ELDERLY age=93 plf=10 impl=9")
                .pass(" jv=3 age-range=ELDERLY age=94 plf=7 impl=8")
                .pass(" jv=3 age-range=ELDERLY age=97 plf=7 impl=7")
                .pass(" jv=6 age-range=ELDERLY age=98 plf=8 impl=5")
                .pass(" jv=3 age-range=ELDERLY age=91 plf=8 impl=10")
                .pass(" jv=4 age-range=ELDERLY age=91 plf=9 impl=8")
                .pass(" jv=5 age-range=ELDERLY age=95 plf=10 impl=7")
                .pass(" jv=3 age-range=ELDERLY age=99 plf=8 impl=5")
                .pass(" jv=1 age-range=CHILD age=9")
                .pass(" jv=2 age-range=ELDERLY age=92")
                .pass(" jv=6 age-range=ELDERLY age=97 plf=7 impl=6")
                .pass(" jv=5 age-range=ELDERLY age=92 plf=7 impl=5")
                .pass(" jv=5 age-range=ELDERLY age=94 plf=8 impl=6")
                .pass(" jv=4 age-range=ELDERLY age=93 plf=8 impl=6")
                .pass(" jv=4 age-range=ELDERLY age=96 plf=10 impl=9")
                .pass(" jv=4 age-range=ELDERLY age=98 plf=8 impl=9")
                .pass(" jv=4 age-range=ELDERLY age=94 plf=9 impl=10")
                .pass(" jv=3 age-range=ELDERLY age=93 plf=7 impl=5")
                .pass(" jv=1 age-range=GROWNUP age=30")
                .pass(" jv=5 age-range=ELDERLY age=91 plf=10 impl=7")
                .pass(" jv=2 age-range=YOUNG age=19")
                .pass(" jv=1 age-range=ELDERLY age=93")
                .pass(" jv=2 age-range=ELDERLY age=94")
                .pass(" jv=6 age-range=ELDERLY age=92 plf=10 impl=7")
                .pass(" jv=1 age-range=ELDERLY age=95")
                .pass(" jv=6 age-range=ELDERLY age=96 plf=7 impl=6")
                .pass(" jv=2 age-range=ELDERLY age=96")
                .pass(" jv=4 age-range=ELDERLY age=95 plf=7 impl=8")
                .pass(" jv=1 age-range=ELDERLY age=97")
                .pass(" jv=6 age-range=ELDERLY age=97 plf=9 impl=9")
                .pass(" jv=2 age-range=ELDERLY age=98")
                .pass(" jv=4 age-range=ELDERLY age=99 plf=10 impl=10")
                .pass(" jv=1 age-range=YOUNG age=20")
                .pass(" jv=2 age-range=CHILD age=10")
                .pass(" jv=1 age-range=ELDERLY age=99")
                .pass(" jv=6 age-range=ELDERLY age=98 plf=10 impl=8")
                .pass("");
    }
}
