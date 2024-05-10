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

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.lazyparams.internal.PowerMockRunnerLight;
import org.powermock.core.classloader.annotations.PrepareForTest;

/**
 * @author Henrik Kaipe
 */
@RunWith(PowerMockRunnerLight.class)
public class FailFastOnInconsistentStaticFieldAssignmentTest {

    private final String DURING_pickNonRepeatable = ".*"
            + FailFastOnInconsistentStaticFieldAssignment.class.getName()
            + ".pickNonRepeatable\\("
            + FailFastOnInconsistentStaticFieldAssignment.class.getSimpleName()
            + "\\.java:\\d\\d+\\).*";

    @Rule
    public final VerifyJupiterRule expect = new VerifyJupiterRule(
            FailFastOnInconsistentStaticFieldAssignment.class) {

        private String methodPrefix;

        @Override
        public Statement apply(final Statement base, Description description) {
            return super.apply(new Statement() {
                @Override public void evaluate() throws Throwable {
                    String simpleName = testClassToVerify.getSimpleName();
                    String fullName = testClassToVerify.getName();

                    methodPrefix = " nonrepeatable_static_field=1";
                    base.evaluate();
                    pass(simpleName + methodPrefix, fullName + methodPrefix);
                    methodPrefix = "";
                    base.evaluate();
                    pass(simpleName, fullName);
                    fail(simpleName, fullName).withMessage(".*" + fullName
                            + ".<clinit>\\(" + simpleName + "\\.java:\\d\\d+\\).*");
                }
            }, description);
        }
        @Override
        public VerifyJupiterRule.SpecifyFailMessageOrNextResult fail(String nameRgx) {
            return super.fail(methodPrefix + nameRgx);
        }
        @Override
        public VerifyJupiterRule.NextResult pass(String nameRgx) {
            return super.pass(methodPrefix + nameRgx);
        }
    };

    @Test @PrepareForTest(ToForceSeparateClassloader1.class)
    public void notParameterized() {
        expect.pass(" / *");
    }

    @Test @PrepareForTest(ToForceSeparateClassloader2.class)
    public void oneParameter() {
        expect
                .pass(" / method-scope_int=2")
                .pass(" / method-scope_int=3")
                .pass("")
                ;
    }

    @Test @PrepareForTest(ToForceSeparateClassloader3.class)
    public void pickNonRepeatable() {
        expect
                .pass(" / not_repeatable=99")
                .pass(" /")
                .fail("").withMessage(DURING_pickNonRepeatable)
                ;
    }

    @Test @PrepareForTest(ToForceSeparateClassloader4.class)
    public void earlyNonRepeatablePick() {
        expect
                .pass(" / +method-scope_int=2")
                .pass(" / try_nonrepeatable not_repeatable=99 method-scope_int=2")
                .fail(" / try_nonrepeatable" + LazyParamsCoreUtil.INCONSISTENCY_DETECTED_APPENDIX)
                        .withMessage(DURING_pickNonRepeatable)
                .fail("").withMessage(".*count 3" + DURING_pickNonRepeatable)
                ;
    }

    static class ToForceSeparateClassloader1 {}
    static class ToForceSeparateClassloader2 {}
    static class ToForceSeparateClassloader3 {}
    static class ToForceSeparateClassloader4 {}
}
