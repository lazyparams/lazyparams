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
public class ScopedParameterInheritenceTest {

    @Rule
    public final VerifyJupiterRule expect = new VerifyJupiterRule(ScopedParameterInheritence.class);

    @Test public void test() {
        String MSG_SEPARATE_BUT_IDENTICAL = "Separate but identical.*";
        String MSG_IDENTICAL_FROM_MIDAIR = "New separate but identical.*";
        String MSG_50_SUCCESSRATE = "Only 50% success.*";
        expect
        .pass(" common=1st / FROM_STATIC_DEF.. COMPR_METHOD_DEF. common=1st. COMPR_MIDAIR_DEF.. PICKED_INT. int=11.")
        .fail(" common=1st / PICKED_INT. int=22. FROM_STATIC_DEF.. +common=2nd")
                .withMessage(MSG_SEPARATE_BUT_IDENTICAL)
        .pass(" common=1st / COMPR_MIDAIR_DEF. common=1st. PICKED_INT. int=22. FROM_STATIC_DEF.. COMPR_METHOD_DEF..")
        .pass(" common=1st / PICKED_INT. int=22. COMPR_METHOD_DEF. common=1st. COMPR_MIDAIR_DEF.. FROM_STATIC_DEF..")
        .fail(" common=1st")
                .withMessage("1.*fail.*total 4.*")
        .fail("ScopedParameterInheritence common=1st int=11"/*but lastet int from test-method was 22!*/,
                "org.lazyparams.showcase.ScopedParameterInheritence common=1st int=11")
                .withMessage(MSG_50_SUCCESSRATE)

        .fail(" common=2nd / FROM_STATIC_DEF.. +common=1st")
                 /*common=1st end-of-line!*/
                .withMessage(MSG_SEPARATE_BUT_IDENTICAL)
        .pass(" common=2nd / PICKED_INT. int=11. FROM_STATIC_DEF.. COMPR_METHOD_DEF. common=2nd. COMPR_MIDAIR_DEF..")
        .fail(" common=2nd / PICKED_INT. int=22. +common=1st")
                 /*common=1st end-of-line again - and now int=22 was contaminated and also lost priority!*/
                .withMessage(MSG_IDENTICAL_FROM_MIDAIR)
        .pass(" common=2nd / PICKED_INT. int=22. COMPR_METHOD_DEF. common=2nd. COMPR_MIDAIR_DEF.. FROM_STATIC_DEF..")
        .fail(" common=2nd / +common=1st")
                 /*common=1st end-of-line 3rd time! from here on it is hard for common=1st to have any priority! */
                .withMessage(MSG_IDENTICAL_FROM_MIDAIR)
                /*
                 * From here on we have a pending combo int=11 - common=1st
                 * but algorithms now avoid common=1st at any cost and have a great bias for common=2nd,
                 * which indeed does unlock a bunch of exection paths that are not so long but they are successful!
                 */
        .pass(" common=2nd / COMPR_METHOD_DEF. common=2nd. FROM_STATIC_DEF.. PICKED_INT. int=11. COMPR_MIDAIR_DEF..")
        .pass(" common=2nd / FROM_STATIC_DEF.. COMPR_METHOD_DEF. common=2nd. COMPR_MIDAIR_DEF.. PICKED_INT. int=11.")
        .pass(" common=2nd / FROM_STATIC_DEF.. COMPR_MIDAIR_DEF. common=2nd. PICKED_INT. int=11. COMPR_METHOD_DEF..")
        .pass(" common=2nd / PICKED_INT. int=11. COMPR_MIDAIR_DEF. common=2nd. FROM_STATIC_DEF.. COMPR_METHOD_DEF..")
        .pass(" common=2nd / COMPR_MIDAIR_DEF. common=2nd. PICKED_INT. int=11. FROM_STATIC_DEF.. COMPR_METHOD_DEF..")
        .pass(" common=2nd / COMPR_METHOD_DEF. common=2nd. PICKED_INT. int=11. COMPR_MIDAIR_DEF.. FROM_STATIC_DEF..")
        .pass(" common=2nd / COMPR_MIDAIR_DEF. common=2nd. COMPR_METHOD_DEF.. PICKED_INT. int=11. FROM_STATIC_DEF..")
        .pass(" common=2nd / PICKED_INT. int=11. FROM_STATIC_DEF.. COMPR_MIDAIR_DEF. common=2nd. COMPR_METHOD_DEF..")
        .pass(" common=2nd / COMPR_MIDAIR_DEF. common=2nd. COMPR_METHOD_DEF.. FROM_STATIC_DEF.. PICKED_INT. int=11.")
        .pass(" common=2nd / FROM_STATIC_DEF.. COMPR_METHOD_DEF. common=2nd. PICKED_INT. int=11. COMPR_MIDAIR_DEF..")
        .pass(" common=2nd / COMPR_MIDAIR_DEF. common=2nd. PICKED_INT. int=11. COMPR_METHOD_DEF.. FROM_STATIC_DEF..")
        .fail(" common=2nd / PICKED_INT. int=11. FROM_STATIC_DEF.. +common=1st") /* Eventually ... */
                .withMessage(MSG_SEPARATE_BUT_IDENTICAL)
                /* ... and now int=22 can enjoy some much needed priority: */
        .pass(" common=2nd / FROM_STATIC_DEF.. COMPR_METHOD_DEF. common=2nd. COMPR_MIDAIR_DEF.. PICKED_INT. int=22.")
        .fail(" common=2nd").withMessage(".*4.*fail.*total 18.*")
        .pass("ScopedParameterInheritence common=2nd int=22"/*same int as latest test-method pick*/,
                "org.lazyparams.showcase.ScopedParameterInheritence common=2nd int=22")

        /*
         * Everything repeated! - in order to walk through remaining combinations in static scope:
         * common=1st,int=22  and  common=2nd,int=11
         */
        .pass(" common=1st / FROM_STATIC_DEF.. COMPR_METHOD_DEF. common=1st. COMPR_MIDAIR_DEF.. PICKED_INT. int=11.")
        .fail(" common=1st / PICKED_INT. int=22. FROM_STATIC_DEF.. +common=2nd").withMessage(MSG_SEPARATE_BUT_IDENTICAL)
        .pass(" common=1st / COMPR_MIDAIR_DEF. common=1st. PICKED_INT. int=22. FROM_STATIC_DEF.. COMPR_METHOD_DEF..")
        .pass(" common=1st / PICKED_INT. int=22. COMPR_METHOD_DEF. common=1st. COMPR_MIDAIR_DEF.. FROM_STATIC_DEF..")
        .fail(" common=1st").withMessage("1.*fail.*total 4.*")
        .pass("ScopedParameterInheritence common=1st int=22"/*same int as latest test-method pick*/,
                "org.lazyparams.showcase.ScopedParameterInheritence common=1st int=22")
        .fail(" common=2nd / FROM_STATIC_DEF.. +common=1st").withMessage(MSG_SEPARATE_BUT_IDENTICAL)
        .pass(" common=2nd / PICKED_INT. int=11. FROM_STATIC_DEF.. COMPR_METHOD_DEF. common=2nd. COMPR_MIDAIR_DEF..")
        .fail(" common=2nd / PICKED_INT. int=22. +common=1st").withMessage(MSG_IDENTICAL_FROM_MIDAIR)
        .pass(" common=2nd / PICKED_INT. int=22. COMPR_METHOD_DEF. common=2nd. COMPR_MIDAIR_DEF.. FROM_STATIC_DEF..")
        .fail(" common=2nd / +common=1st").withMessage(MSG_IDENTICAL_FROM_MIDAIR)
        .pass(" common=2nd / COMPR_METHOD_DEF. common=2nd. FROM_STATIC_DEF.. PICKED_INT. int=11. COMPR_MIDAIR_DEF..")
        .pass(" common=2nd / FROM_STATIC_DEF.. COMPR_METHOD_DEF. common=2nd. COMPR_MIDAIR_DEF.. PICKED_INT. int=11.")
        .pass(" common=2nd / FROM_STATIC_DEF.. COMPR_MIDAIR_DEF. common=2nd. PICKED_INT. int=11. COMPR_METHOD_DEF..")
        .pass(" common=2nd / PICKED_INT. int=11. COMPR_MIDAIR_DEF. common=2nd. FROM_STATIC_DEF.. COMPR_METHOD_DEF..")
        .pass(" common=2nd / COMPR_MIDAIR_DEF. common=2nd. PICKED_INT. int=11. FROM_STATIC_DEF.. COMPR_METHOD_DEF..")
        .pass(" common=2nd / COMPR_METHOD_DEF. common=2nd. PICKED_INT. int=11. COMPR_MIDAIR_DEF.. FROM_STATIC_DEF..")
        .pass(" common=2nd / COMPR_MIDAIR_DEF. common=2nd. COMPR_METHOD_DEF.. PICKED_INT. int=11. FROM_STATIC_DEF..")
        .pass(" common=2nd / PICKED_INT. int=11. FROM_STATIC_DEF.. COMPR_MIDAIR_DEF. common=2nd. COMPR_METHOD_DEF..")
        .pass(" common=2nd / COMPR_MIDAIR_DEF. common=2nd. COMPR_METHOD_DEF.. FROM_STATIC_DEF.. PICKED_INT. int=11.")
        .pass(" common=2nd / FROM_STATIC_DEF.. COMPR_METHOD_DEF. common=2nd. PICKED_INT. int=11. COMPR_MIDAIR_DEF..")
        .pass(" common=2nd / COMPR_MIDAIR_DEF. common=2nd. PICKED_INT. int=11. COMPR_METHOD_DEF.. FROM_STATIC_DEF..")
        .fail(" common=2nd / PICKED_INT. int=11. FROM_STATIC_DEF.. +common=1st").withMessage(MSG_SEPARATE_BUT_IDENTICAL)
        .pass(" common=2nd / FROM_STATIC_DEF.. COMPR_METHOD_DEF. common=2nd. COMPR_MIDAIR_DEF.. PICKED_INT. int=22.")
        .fail(" common=2nd").withMessage(".*4.*fail.*total 18.*")
        .fail("ScopedParameterInheritence common=2nd int=11"/*but latest int from test-method was 22!*/,
                "org.lazyparams.showcase.ScopedParameterInheritence common=2nd int=11")
                .withMessage(MSG_50_SUCCESSRATE)

        /*
         * Everything completed!
         */
        .fail("ScopedParameterInheritence", "org.lazyparams.showcase.ScopedParameterInheritence")
                .withMessage(".*2.*fail.*total 4.*");
    }
}
