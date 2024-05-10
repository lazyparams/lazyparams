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
import org.lazyparams.VerifyVintageRule;

/**
 * @author Henrik Kaipe
 */
public class WithExpectRuleTest {

    @Rule
    public final VerifyVintageRule expect = new VerifyVintageRule(WithExpectRule.class);

    @Test
    public void test() {
        expect
                .fail(" arrayOrExpect=\\[2, 3, 5\\] IOException")
                .pass(" arrayOrExpect=true")
                .fail(" arrayOrExpect=false IOException")
                .fail(" arrayOrExpect=\\[2, 3, 5\\] NoSuchElementException")
                .pass(" arrayOrExpect=true")
                .fail(" arrayOrExpect=false NoSuchElementException")
                .fail("").withMessage(".*4.*fail.*total 6.*")
                ;
    }
}
