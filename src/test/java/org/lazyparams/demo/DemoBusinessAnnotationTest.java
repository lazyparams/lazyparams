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
public class DemoBusinessAnnotationTest {

    @Rule
    public VerifyJupiterRule expect = new VerifyJupiterRule(DemoBusinessAnnotation.class);

    @Test
    public void onlyOneUserOptionDoesNotInitiateParametrizationOnItsOwn() {
        expect.pass("");
    }

    @Test
    public void butParameterWillShowIfThereIsOtherParameterWithMultipleValues() {
        String configures_security_appendix =
                " as_ADMIN configures security  & finally ADMIN is logged out";
        String generate_reports_appendix =
                " as_ADMIN generates reports  & finally ADMIN is logged out";
        expect.pass(configures_security_appendix)
                .pass(generate_reports_appendix)
                .pass(generate_reports_appendix)
                .pass(configures_security_appendix)
                /*Duplications above happen because qronic parameter does not show on success*/
                .pass("");
    }

    @Test
    public void onIntranet() {
        expect.pass(" as_ADMIN visits business plans  & finally ADMIN is logged out")
                .fail(" as_EMPLOYEE HIDDEN=2 visits business plans  & finally EMPLOYEE is logged out")
                        .withMessage(".*will reveal HIDDEN.*")
                .fail(" as_IMPOSTER  & finally IMPOSTER is logged out")
                        .withMessage(".*IMPOSTER.*")
                .pass(" as_ADMIN visits latest report  & finally ADMIN is logged out")
                .pass(" as_EMPLOYEE visits latest report  & finally EMPLOYEE is logged out")
                .fail("").withMessage("2.*test.*fail.*total 5.*");
    }

    @Test
    public void onPublicWeb() {
        expect.pass(" as_ADMIN visits first-page +visits logout-page  & finally ADMIN is logged out")
                .pass(" as_EMPLOYEE visits news-page visits logout-page  & finally EMPLOYEE is logged out")
                .pass(" as_CUSTOMER visits first-page then gives feedback: THUMBS=UP visits logout-page  & finally CUSTOMER is logged out")
                .fail(" as_IMPOSTER  & finally IMPOSTER is logged out")
                        .withMessage(".*IMPOSTER.*")
                .pass(" as_GUEST visits news-page then gives feedback: THUMBS=DOWN visits logout-page  & finally GUEST is logged out")
                .pass(" as_CUSTOMER visits news-page visits logout-page  & finally CUSTOMER is logged out")
                .pass(" as_GUEST visits first-page visits logout-page  & finally GUEST is logged out")
                .pass(" as_ADMIN visits news-page then gives feedback: THUMBS=UP visits logout-page  & finally ADMIN is logged out")
                .pass(" as_EMPLOYEE visits first-page then gives feedback: THUMBS=DOWN visits logout-page  & finally EMPLOYEE is logged out")
                .pass(" as_ADMIN visits first-page then gives feedback: THUMBS=DOWN visits logout-page  & finally ADMIN is logged out")
                .pass(" as_EMPLOYEE visits first-page then gives feedback: THUMBS=UP visits logout-page  & finally EMPLOYEE is logged out")
                .pass(" as_CUSTOMER visits news-page then gives feedback: THUMBS=DOWN visits logout-page  & finally CUSTOMER is logged out")
                .pass(" as_GUEST visits news-page then gives feedback: THUMBS=UP visits logout-page  & finally GUEST is logged out")
                .fail("").withMessage("1 test.*fail.*total 13.*");
    }
}
