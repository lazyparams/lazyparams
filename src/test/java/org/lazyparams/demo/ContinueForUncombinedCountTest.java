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
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.lazyparams.VerifyJupiterRule;
import org.lazyparams.VerifyVintageRule;

/**
 * @author Henrik Kaipe
 */
@RunWith(Parameterized.class)
public class ContinueForUncombinedCountTest {

    private Consumer<String> expectPass;
    private int limit;

    @Parameterized.Parameter @Rule
    public VintageOrPlanetary setup_limit_and_expectPass;

    @Parameterized.Parameters(name="{0}")
    public static List<?> vintageOrPlanetary() {
        return Stream.of(VintageOrPlanetary.values())
                .map(enmRuleConstant -> new Object[] {enmRuleConstant})
                .collect(Collectors.toList());
    }

    @Test
    public void twoMatchResultsAndAnExtraInt() {
        Stream.of(
                " m1=1 m2=1 x=1",
                " m1=X m2=X x=1",
                " m1=2 m2=2 x=2",
                " m1=1 m2=X x=2",
                " m1=2 m2=1 x=1",
                " m1=X m2=2 x=2",
                " m1=2 m2=1 x=2",
                " m1=X m2=2 x=1",
                " m1=1 m2=2 x=1",
                " m1=X m2=1 x=2",
                " m1=2 m2=X x=1",
                " m1=1 m2=X x=2",
                " m1=1 m2=1 x=1",
                " m1=X m2=X x=2",
                " m1=2 m2=2 x=1",

                " m1=1 m2=2 x=2",
                " m1=X m2=1 x=1",
                " m1=2 m2=X x=2",
                " m1=1 m2=1 x=2",
                " m1=X m2=X x=1",
                " m1=2 m2=2 x=1",
                " m1=1 m2=X x=1",
                " m1=X m2=1 x=2",
                " m1=2 m2=1 x=2",
                " m1=1 m2=2 x=2")
                .limit(limit)
                .map(new UnaryOperator<String>() {
                    int count = 0;
                    @Override public String apply(String s) {
                        return " " + setup_limit_and_expectPass.name() + s + " #" + ++count;
                    }
                }).forEach(expectPass);
        expectPass.accept("");
    }

    enum VintageOrPlanetary implements MethodRule {
        vintage, planetary;

        TestRule createVerifyRule(final ContinueForUncombinedCountTest testInstance) {
            switch (this) {
                case vintage:
                    testInstance.limit = ContinueForUncombinedCount.VINTAGE_TARGET_COUNT;
                    return new VerifyVintageRule(ContinueForUncombinedCount.class) {{
                        testInstance.expectPass = this::pass;
                    }};
                case planetary:
                    testInstance.limit = ContinueForUncombinedCount.PLANETARY_TARGET_COUNT;
                    return new VerifyJupiterRule(ContinueForUncombinedCount.class) {{
                        testInstance.expectPass = this::pass;
                    }};
                default:
                    throw new AssertionError("Unknown constant " + this.name());
            }
        }

        @Override
        public Statement apply(Statement base, FrameworkMethod method, Object target) {
            final Description description = Description.createTestDescription(
                    method.getDeclaringClass(), method.getName());
            return createVerifyRule((ContinueForUncombinedCountTest) target)
                    .apply(base, description);
        }
    }
}
