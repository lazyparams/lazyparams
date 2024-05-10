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

import java.util.List;
import java.util.function.Consumer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.lazyparams.VerifyJupiterRule;

/**
 * @author Henrik Kaipe
 */
public class SprouterTest {

    @Rule
    public final VerifyJupiterRule expect =
            new VerifyJupiterRule(Sprouter.Sprinner.class) {
        /**
         * Need a twist to make expectations work for @Nested test class:
         */
        @Override protected void customizeBeforeLaunch() {
            pass(Sprouter.Sprinner.class.getSimpleName(),
                    Sprouter.Sprinner.class.getName().replace("$", "\\$"));
            pass(Sprouter.class.getSimpleName(), Sprouter.class.getName());

            List<VerifyJupiterRule.ResultVerifier> expects = getExpectations();

            final ResultVerifier outerClassTerminalVerification =
                    expects.remove(expects.size() - 1);
            expects.add(new ResultVerifier(
                    /*
                     * Verifies outer class test completion - but disguised as
                     * terminal verification of inner test-class completion,
                     * for which an undesirable default fallback verification
                     * would otherwise be appended further downstream!
                     */
                    Sprouter.Sprinner.class.getSimpleName(),
                    Sprouter.Sprinner.class.getName(), null) {
                @Override
                public Consumer<TestExecutionResult> apply(
                        String actualDisplayName, String actualLegacyName) {
                    return outerClassTerminalVerification
                            .apply(actualDisplayName, actualLegacyName);
                }
            });
        }
    };

    @Test
    public void threeTimes() {
        expect.fail(" expect=32").withMessage(".*23.*")
                .pass(" expect=23")
                .fail(" expect=101").withMessage(".*101.*")
                .fail("").withMessage(".*2 .*fail.*total 3.*");
    }
}
