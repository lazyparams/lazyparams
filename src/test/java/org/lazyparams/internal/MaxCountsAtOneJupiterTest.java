/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.internal;

import org.junit.Rule;
import org.junit.Test;
import org.lazyparams.VerifyJupiterRule;

/**
 * @author Henrik Kaipe
 */
public class MaxCountsAtOneJupiterTest {

    @Rule
    public final VerifyJupiterRule expect = new VerifyJupiterRule(MaxCountsAtOne.class);

    @Test
    public void maxTotal_pass() {
        expect.pass("");
    }
    @Test
    public void maxTotal_fail() {
        expect.fail("").withMessage(".*42.*");
    }
    @Test
    public void maxBoth_fail() {
        /*Expect same as ...*/ maxTotal_fail();
    }
    @Test
    public void maxFail_only_should_not_prevent_parameterization() {
        expect.fail(" dummy_param.*").withMessage(".*42.*")
                .fail("").withMessage(".*[Ff]ail.*count.*max.* 1");
    }

    @Test
    public void configureMaxTotalOneOn2ndExecutionReportsAsParameterized_pass() {
        expect.pass(" dummy_param.*")
                .pass(" dummy_param.* set_max-total_one")
                .fail("").withMessage(".*[Tt]otal.*max at 1.*");
    }

    @Test
    public void configureMaxTotalOneOn2ndExecutionReportsAsParameterized_fail1st() {
        expect.fail(" dummy_param.*").withMessage(".*42.*")
                .pass(" dummy_param.* set_max-total_one")
                .fail("").withMessage(".*[Tt]otal.*max at 1.*");
    }

    @Test
    public void configureMaxTotalOneOn2ndExecutionReportsAsParameterized_fail2nd() {
        expect.pass(" dummy_param.*")
                .fail(" dummy_param.* set_max-total_one answer=.*")
                .fail("").withMessage(".*[Tt]otal.*max at 1.*");
    }

    @Test
    public void configureMaxTotalOneOn2ndExecutionReportsAsParameterized_failBoth() {
        expect.fail(" dummy_param.*").withMessage(".*42.*")
                .fail(" dummy_param.* set_max-total_one answer=.*")
                .fail("").withMessage(".*[Tt]otal.*max at 1.*");
    }
}
