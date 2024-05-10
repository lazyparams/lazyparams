/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
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

/**
 * @author Henrik Kaipe
 */
@RunWith(Parameterized.class)
public class FailFastOnInconsistentPicksTest {

    private static final String DURING_pickNonRepeatable = during("pickNonRepeatable");

    Function<String,Consumer<String>> expectFail;
    Consumer<String> expectPass;

    @Parameterized.Parameter @Rule
    public MethodRule setup_expects;

    @Parameterized.Parameters(name = "{0}")
    public static List<?> vintageOrPlanetary() {
        return Stream.of(VintageOrPlanetary.values())
                .map(enmRuleConst -> new Object[] {enmRuleConst})
                .collect(Collectors.toList());
    }

    static String during(String testMethodNameRgx) {
        return ".*FailFastOnInconsistent.*" + testMethodNameRgx
                + "\\(FailFastOnInconsistent.*java:\\d\\d++\\).*";
    }

    void expectFail(String problemSourceLocation, int repeatCount, int failCount) {
        String failMessage = ".*count " + repeatCount + ".*";
        if (1 == failCount) {
            failMessage += "1 test failed";
        } else if (2 <= failCount) {
            failMessage += failCount + " tests failed";
        }
        expectFail.apply("").accept(failMessage + problemSourceLocation);
    }

    void expectDetectionDuring_pickNonRepeatable(String nameRgxPrefix) {
        expectFail
                .apply(nameRgxPrefix + LazyParamsCoreUtil.INCONSISTENCY_DETECTED_APPENDIX)
                .accept(DURING_pickNonRepeatable);
    }

    @Test
    public void pickNonRepeatable() {
        expectPass.accept(" non_repeatable=1");
        expectPass.accept("");
        expectFail(DURING_pickNonRepeatable, 2, 0);
    }

    @Test
    public void nonRecoverable1st() {
        expectPass.accept(" non_repeatable=1 repeatable=1");
        expectDetectionDuring_pickNonRepeatable("");
        expectFail(DURING_pickNonRepeatable, 2, 0);
    }

    @Test
    public void diffent1stParameterOnSecondRun() {
        expectPass.accept(" non_repeatable=1");
        expectDetectionDuring_pickNonRepeatable("");
        expectFail(DURING_pickNonRepeatable, 2, 0);
    }

    @Test
    public void nonRepeatable1stAndThenTwoRepeatableParams() {
        expectPass.accept(" non_repeatable=1 repeatable1=11 repeatable2=101");
        expectDetectionDuring_pickNonRepeatable("");
        expectFail(DURING_pickNonRepeatable, 2, 0);
    }

    @Test
    public void twoUnrepeatableParams_and_TwoGoodOnes() {
        String duringTestMethod = during("twoUnrepeatable.*");
        expectPass.accept(" non_repeatable2=-1 non_repeatable=1 good1=4 good2=73");
        expectFail.apply(LazyParamsCoreUtil.INCONSISTENCY_DETECTED_APPENDIX).accept(duringTestMethod);
        expectFail(duringTestMethod, 2, 0);
    }

    @Test
    public void lateConflictAfterFail1st() {
        expectFail.apply(" good-1st=1 non_repeatable=1 good-again=2");
        expectPass.accept(" good-1st=2 good-again=3");
        expectDetectionDuring_pickNonRepeatable(" good-1st=1");
        expectFail(DURING_pickNonRepeatable, 3, 1);
    }

    @Test
    public void failTwiceAndLaterIntroductionOfNonRepeatable() {
        expectPass.accept(" finally=99");
        expectFail.apply(" CLASS on3=-8 finally=99");
        expectPass.accept(" non_repeatable=1 finally=999");//Late intro on non-repeatable!
        expectFail.apply(" CLASS on3=-9 finally=999");
        expectDetectionDuring_pickNonRepeatable(" RUNTIME");
        expectFail(DURING_pickNonRepeatable, 5, 2);
    }

    @Test
    public void nonRepeatableLast() {
        expectPass.accept(" 1st=42 non_repeatable=1");
        expectPass.accept(" 1st=73");
        expectPass.accept(" 1st=42");
        expectFail(DURING_pickNonRepeatable, 3, 0);
    }

    enum VintageOrPlanetary implements MethodRule {
        vintage, planetary;

        TestRule createVerifyRule(final FailFastOnInconsistentPicksTest testInstance) {
            switch (this) {
                case vintage:
                    return new VerifyVintageRule(FailFastOnInconsistentVintage.class) {
                        {
                            testInstance.expectPass = this::pass;
                            testInstance.expectFail = testNameRgx
                                    -> fail(testNameRgx)::withMessage;
                        }
                        @Override
                        public Statement apply(Statement base, Description description) {
                            String methodName = description.getMethodName();
                            if ("pickNonRepeatable".equals(methodName)) {
                                /*
                                 * This test doesn't detect the inconsistency in
                                 * midair - but does instead detect when cheching
                                 * whether there are pending combinations. */
                                supportSingularDisplayNameDuplicate();
                            }
                            return super.apply(base, description);
                        }
                    };
                case planetary:
                    return new VerifyJupiterRule(FailFastOnInconsistentPicks.class) {
                        {
                            testInstance.expectPass = this::pass;
                            testInstance.expectFail = testNameRgx
                                    -> fail(testNameRgx)::withMessage;
                        }
                    };
                default:
                    throw new AssertionError("Unknown constant " + this.name());
            }
        }

        @Override
        public Statement apply(Statement base, FrameworkMethod method, Object target) {
            final Description description = Description.createTestDescription(
                    method.getDeclaringClass(), method.getName());
            return createVerifyRule((FailFastOnInconsistentPicksTest)target)
                    .apply(base, description);
        }
    }
}
