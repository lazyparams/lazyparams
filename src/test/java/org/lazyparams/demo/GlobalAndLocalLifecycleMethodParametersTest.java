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

import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.lazyparams.VerifyJupiterRule;

/**
 * @author Henrik Kaipe
 */
public class GlobalAndLocalLifecycleMethodParametersTest {

    @Rule
    public final VerifyJupiterRule expect =
            new VerifyJupiterRule(GlobalAndLocalLifecycleMethodParameters.class);

    void expectPass(String testName, String... displayRgxTemplates) {
        Stream.of(" global=gblFoo", " global=gblBar")
                .forEach(staticPrefix -> {
            for (String eachSuffix : displayRgxTemplates) {
                expect.pass(testName + staticPrefix + " / local=" + eachSuffix);
            }
            expect.pass(testName + staticPrefix);
            expect.pass(
                    GlobalAndLocalLifecycleMethodParameters.class.getSimpleName() + staticPrefix,
                    GlobalAndLocalLifecycleMethodParameters.class.getName() + staticPrefix);
        });
    }

    @Test
    public void extraParameters() {
        expect.methodParameterTypes( String.class, int.class);
        expectPass("extraParameters\\(String, int\\)",
                "1 extraStr=foo extraInt=41",
                "2 extraStr=bar extraInt=42",
                "3 extraStr=buz extraInt=43",
                "1 extraStr=bar extraInt=43",
                "3 extraStr=foo extraInt=42",
                "2 extraStr=buz extraInt=41",
                "1 extraStr=buz extraInt=42",
                "2 extraStr=foo extraInt=43",
                "3 extraStr=bar extraInt=41");
    }

    @Test
    public void vanilla() {
        expectPass("", "1","2","3");
    }
}
