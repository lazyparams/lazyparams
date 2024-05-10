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

import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lazyparams.showcase.FalseOrTrue;
import org.lazyparams.LazyParams;

import static org.junit.Assert.assertEquals;

/**
 * @author Henrik Kaipe
 */
public class MaxCountsAtOne {

    @BeforeEach @Before
    public void introduceParameter() {
        LazyParams.pickValue("dummy_param",
                "1st value", "2nd value", "other values never happen");
    }

    private void fail() {
        assertEquals("Fail answer", -1, LazyParams
                .pickValue("answer", 42,"wrong number"));
    }

    boolean setMaxTotalOneOn2ndExecution() {
        if (FalseOrTrue.pickBoolean("set_max-total_one")) {
            LazyParams.currentScopeConfiguration().setMaxTotalCount(1);
            return true;
        } else {
            return false;
        }
    }

    @Test @org.junit.Test
    public void maxTotal_pass() {
        LazyParams.currentScopeConfiguration().setMaxTotalCount(1);
    }

    @Test @org.junit.Test
    public void maxTotal_fail() {
        LazyParams.currentScopeConfiguration().setMaxTotalCount(1);
        fail();
    }

    @Test @org.junit.Test
    public void maxBoth_fail() {
        LazyParams.currentScopeConfiguration().setMaxFailureCount(1);
        LazyParams.currentScopeConfiguration().setMaxTotalCount(1);
        fail();
    }

    @Test @org.junit.Test
    public void maxFail_only_should_not_prevent_parameterization() {
        LazyParams.currentScopeConfiguration().setMaxFailureCount(1);
        fail();
    }

    @Test @org.junit.Test
    public void configureMaxTotalOneOn2ndExecutionReportsAsParameterized_pass() {
        setMaxTotalOneOn2ndExecution();
    }

    @Test @org.junit.Test
    public void configureMaxTotalOneOn2ndExecutionReportsAsParameterized_fail1st() {
        if (false == setMaxTotalOneOn2ndExecution()) {
            fail();
        }
    }

    @Test @org.junit.Test
    public void configureMaxTotalOneOn2ndExecutionReportsAsParameterized_fail2nd() {
        if (setMaxTotalOneOn2ndExecution()) {
            fail();
        }
    }

    @Test @org.junit.Test
    public void configureMaxTotalOneOn2ndExecutionReportsAsParameterized_failBoth() {
        setMaxTotalOneOn2ndExecution();
        fail();
    }
}
