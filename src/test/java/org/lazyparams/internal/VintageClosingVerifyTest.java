/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.internal;

import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.Rule;

import org.junit.Test;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.runners.Suite;
import org.lazyparams.VerifyVintageRule;

import static org.lazyparams.internal.VintageClosingTest.*;

/**
 * @author Henrik Kaipe
 */
public class VintageClosingVerifyTest {

    VerifyVintageRule[] verifyRules = Stream
            .of(VintageClosingTest.class.getAnnotation(Suite.SuiteClasses.class).value())
            .map(VerifyVintageRule::new)
            .toArray(VerifyVintageRule[]::new);

    @Rule
    public TestRule compoundRule = (base,desc) -> new RunRules(
            base, Arrays.asList(verifyRules), desc);

    @Test
    public void pickAndOpen() {
        for (VerifyVintageRule eachRule : verifyRules) {
            Stream.of(eachRule.getTestClass().getAnnotation(Resources.class).value())
                    .map(resource -> " opens=" + resource)
                    .forEach(eachRule::pass);
            eachRule.pass("");
        }
    }
}
