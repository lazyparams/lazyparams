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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.Request;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;
import org.junit.runners.model.Statement;

import org.lazyparams.internal.ProvideJunitVintage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Henrik Kaipe
 */
public class VerifyVintageRule implements TestRule {

    final Class<?> testClassToVerify;
    private String methodName;
    private boolean forceFilterDelegation = false;
    private boolean expectSkipMsgOnStaticParams = false;
    private boolean singularDuplicateSupport = false;

    private final List<ResultVerifier> expectations = new ArrayList<ResultVerifier>();

    public VerifyVintageRule(Class<?> testClassToVerify) {
        this.testClassToVerify = testClassToVerify;
    }

    @Override
    public String toString() {
        return testClassToVerify.getSimpleName();
    }

    public Class<?> getTestClass() {
        return testClassToVerify;
    }

    protected void customize(List<ResultVerifier> expectations) {}

    /**
     * Enables a filter backdoor that can be useful when there is a
     * non-{@link org.junit.runner.manipulation.Filterable filterable} runner
     * hiding in the runner delegate hierarchy.
     */
    public VerifyVintageRule forceFilterDelegation() {
        this.forceFilterDelegation = true;
        return this;
    }

    public VerifyVintageRule expectSkipMessageOnParametersInStaticScope() {
        this.expectSkipMsgOnStaticParams = true;
        return this;
    }

    public VerifyVintageRule supportSingularDisplayNameDuplicate() {
        this.singularDuplicateSupport = true;
        return this;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        methodName = description.getMethodName();
        int methodNameEnd = methodName.indexOf('[');
        if (0 < methodNameEnd) {
            /* An issue with method-name when using runner Parameterized! */
            methodName = methodName.substring(0,methodNameEnd);
        }

        final Description classDescription = Description
                .createSuiteDescription(testClassToVerify);

        return new Statement() {
            String containedDuplicateDisplayName;

            boolean failForDuplicate(String displayName) {
                if (singularDuplicateSupport && null == containedDuplicateDisplayName) {
                    containedDuplicateDisplayName = displayName;
                    return false;
                } else {
                    return true;
                }
            }
            boolean enforceDuplicate(String displayName) {
                if (displayName.equals(containedDuplicateDisplayName)) {
                    containedDuplicateDisplayName = null;
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public void evaluate() throws Throwable {
                base.evaluate();
                customize(expectations);
                if (expectSkipMsgOnStaticParams) {
                    expectations.add(new ResultVerifier(
                            "No static parameterization support.*JUnit-3/4.*", null) {
                        boolean assumptionFailureVerified = false;

                        @Override
                        void verifyCompleted(Description description) {
                            assertTrue("Assumption failure expected",
                                    assumptionFailureVerified);
                            super.verifyCompleted(description);
                        }
                        @Override
                        void verifyFailure(Failure failure) {
                            assertThat(failure.getException())
                                    .as("expected skip message")
                                    .isInstanceOf(AssumptionViolatedException.class);
                            assumptionFailureVerified = true;
                        }
                    });
                }

                ListIterator<ResultVerifier> iter = expectations.listIterator();
                while (iter.hasNext()) {
                    iter.next().setResultNumber(iter.nextIndex());
                }

                final Filter invokeTargetMethodOnly = new Filter() {
                    @Override
                    public boolean shouldRun(Description description) {
                        if (classDescription.equals(description)) {
                            return true;
                        }
                        for (Description anyChild : description.getChildren()) {
                            if (anyChild != description && shouldRun(anyChild)) {
                                return true;
                            }
                        }
                        if (null == description.getTestClass()
                                || false == testClassToVerify.getName().equals(
                                        description.getTestClass().getName())) {
                            return false;
                        }
                        String testMethod = description.getMethodName();
                        if (null == testMethod) {
                            return false;
                        } else {
                            return testMethod.equals(methodName)
                                    || testMethod.startsWith(methodName + '[');
                        }
                    }
                    @Override
                    public String describe() {
                        return Filter.matchMethodDescription(Description
                                .createTestDescription(testClassToVerify, methodName))
                                .describe();
                    }
                };
                Request runRequest = Request
                        .aClass(testClassToVerify)
                        .filterWith(invokeTargetMethodOnly);

                JUnitCore core = new JUnitCore() {
                    @Override public Result run(Runner runner) {
                        if (forceFilterDelegation) {
                            LazyParams.install();
                            runner = new RunnerFilteringWrapper(
                                    runner, invokeTargetMethodOnly);
                        }
                        return super.run(runner);
                    }
                };
                final AtomicReference<Throwable> verifyFailure =
                        new AtomicReference<Throwable>();
                core.addListener(new RunListener() {
                    final Set<String> started = new HashSet<String>();

                    void pleaseStopOnFailure() {
                        if (null != verifyFailure.get()) {
                            throw new StoppedByUserException();
                        }
                    }
                    void collectFailure(Runnable verification) {
                        if (null == verifyFailure.get()) {
                            try {
                                verification.run();
                            } catch (StoppedByUserException ex) {
                                throw ex;
                            } catch (Throwable ex) {
                                verifyFailure.set(ex);
                            }
                        }
                    }

                    void assertPendingExpectations(Description desc) {
                        assertThat(expectations)
                                .as("Expectations when %s is reported",
                                        desc.getDisplayName())
                                .isNotEmpty();
                    }
                    void assertStarted(Description desc) {
                        assertThat(started)
                                .as("Display-names of tests that have been started")
                                .contains(desc.getDisplayName());
                    }

                    @Override
                    public void testStarted(final Description description) {
                        if (false == classDescription.equals(description)
                                && false == started.add(description.getDisplayName())) {
                            if (failForDuplicate(description.getDisplayName())) {
                                collectFailure(new Runnable() {
                                    @Override public void run() {
                                        throw new AssertionError(description.getDisplayName()
                                                + " has already been started: " + started);
                                    }
                                });
                            }
                        }
                        pleaseStopOnFailure();
                    }
                    @Override
                    public void testFailure(final Failure failure) {
//                        failure.getException().printStackTrace();
                        collectFailure(new Runnable() {
                            @Override public void run() {
                                assertPendingExpectations(failure.getDescription());
                                expectations.get(0).verifyFailure(failure);
                                assertStarted(failure.getDescription());
                            }
                        });
                    }
                    @Override
                    public void testAssumptionFailure(Failure failure) {
                        testFailure(failure);
                    }
                    @Override
                    public void testFinished(final Description description) {
                        if (classDescription.equals(description)) {
                            return;
                        }
                        collectFailure(new Runnable() {
                            @Override public void run() {
                                assertPendingExpectations(description);
                                expectations.remove(0).verifyCompleted(description);
                                assertStarted(description);
                                assertTrue(description.getDisplayName()
                                        + " must have been started",
                                        started.remove(description.getDisplayName()));
                                if (enforceDuplicate(description.getDisplayName())) {
                                    started.add(description.getDisplayName());
                                }
                            }
                        });
                    }
                });
                core.run(runRequest);

                Throwable firstFailure = verifyFailure.get();
                if (null != firstFailure) {
                    throw firstFailure;
                }
                assertThat(expectations)
                        .as("Pending verifications after test-run finished!")
                        .isEmpty();
            }
        };
    }

    public NextResult pass(String nameRgx) {
        expectations.add(new ResultVerifier(nameRgx, null));
        return new NextResult();
    }
    public SpecifyFailMessageOrNextResult fail(String nameRgx) {
        ResultVerifier expectedFailure = new ResultVerifier(nameRgx, "(?s).*");
        expectations.add(expectedFailure);
        return new SpecifyFailMessageOrNextResult(expectedFailure);
    }

    public class NextResult {
        public NextResult pass(String nameRgx) {
            return VerifyVintageRule.this.pass(nameRgx);
        }
        public SpecifyFailMessageOrNextResult fail(String nameRgx) {
            return VerifyVintageRule.this.fail(nameRgx);
        }
    }

    public class SpecifyFailMessageOrNextResult extends NextResult {
        private final AtomicReference<ResultVerifier> failureResultRef;

        SpecifyFailMessageOrNextResult(ResultVerifier failureResult) {
            this.failureResultRef = new AtomicReference<ResultVerifier>(failureResult);
        }

        public NextResult withMessage(String messageRgx) {
            messageRgx = "(?s)" + messageRgx;
            ResultVerifier failureVerifier = failureResultRef.getAndSet(null);
            if (null == failureVerifier) {
                throw new IllegalStateException(
                        "Expected message pattern is already specified");
            }
            failureVerifier.modifyExpectedFailureMessageRgx(messageRgx);
            return new NextResult();
        }
    }

    class ResultVerifier {
        final String displayNameRgx;
        private String messageRgx;
        private int resultNumber;

        public ResultVerifier(String displayNameRgx, String messageRgx) {
            if ("".equals(displayNameRgx)) {
                displayNameRgx = methodName;
            } else if (' ' == displayNameRgx.charAt(0)) {
                displayNameRgx = methodName + displayNameRgx;
            }
            this.displayNameRgx =
                    displayNameRgx + "(\\(" + getTestClass().getName() + "\\))?";
            this.messageRgx = messageRgx;
        }

        String getMessageRgx() {
            return messageRgx;
        }

        void modifyExpectedFailureMessageRgx(String messageRgxReplacement) {
            this.messageRgx = messageRgxReplacement;
        }

        void setResultNumber(int resultNumber) {
            this.resultNumber = resultNumber;
        }

        void verifyFailure(Failure failure) {
            if (null == messageRgx) {
                throw new AssertionError(failure.toString());
            }
            String expectedMessage = messageRgx;
            messageRgx = null;
            Description desc = failure.getDescription();
            verifyCompleted(desc);
            String actualMessage = failure.getMessage();
            if (null == actualMessage) {
                actualMessage = "";
            }
            assertThat(actualMessage)
                    .as("Failure message on result #%s - %s",
                            resultNumber, desc.getDisplayName())
                    .matches(expectedMessage);
        }
        void verifyCompleted(Description description) {
            String displayName = description.getDisplayName();
            assertThat(description.getDisplayName())
                    .as("Display-name on result #%s", resultNumber)
                    .matches(displayNameRgx);
            if (null != messageRgx) {
                assertThat((String)null)
                        .as("Error on result #%s - %s",
                                resultNumber, displayName)
                        .isEqualTo(messageRgx);
            }
        }
        @Override public String toString() {
            return displayNameRgx + ':' + messageRgx;
        }
    }

    private static class RunnerFilteringWrapper extends Runner {
        private final Runner coreRunner;

        RunnerFilteringWrapper(Runner coreRunner, Filter coreFilter) {
            this.coreRunner = coreRunner;
            try {
                ProvideJunitVintage.FilteredRunnerAdvice.record(coreRunner, coreFilter);
            } catch (Throwable ex) {
                throw new Error(ex);
            }
        }
        @Override
        public Description getDescription() {
            return coreRunner.getDescription();
        }
        @Override
        public void run(final RunNotifier coreNotifier) {
            /**
             * Wrap with notifier that triggers forwarding of filter to delegate runner ...
             */
            coreRunner.run(new ProvideJunitVintage.RepeatNotifier(null) {
                @Override public void addFirstListener(RunListener listener) {
                    coreNotifier.addFirstListener(listener);
                }
                @Override public void pleaseStop() {
                    coreNotifier.pleaseStop();
                }
                @Override public void fireTestFinished(Description description) {
                    coreNotifier.fireTestFinished(description);
                }
                @Override public void fireTestIgnored(Description description) {
                    coreNotifier.fireTestIgnored(description);
                }
                @Override public void fireTestAssumptionFailed(Failure failure) {
                    coreNotifier.fireTestAssumptionFailed(failure);
                }
                @Override public void fireTestFailure(Failure failure) {
                    coreNotifier.fireTestFailure(failure);
                }
                @Override public void fireTestStarted(Description description)
                          throws StoppedByUserException {
                    coreNotifier.fireTestStarted(description);
                }
                @Override public void fireTestSuiteFinished(Description description) {
                    coreNotifier.fireTestSuiteFinished(description);
                }
                @Override public void fireTestSuiteStarted(Description description) {
                    coreNotifier.fireTestSuiteStarted(description);
                }
                @Override public void fireTestRunFinished(Result result) {
                    coreNotifier.fireTestRunFinished(result);
                }
                @Override public void fireTestRunStarted(Description description) {
                    coreNotifier.fireTestRunStarted(description);
                }
                @Override public void removeListener(RunListener listener) {
                    coreNotifier.removeListener(listener);
                }
                @Override public void addListener(RunListener listener) {
                    coreNotifier.addListener(listener);
                }
            });
        }
    }
}
