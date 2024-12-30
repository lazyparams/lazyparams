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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assume;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.lazyparams.core.Lazer;
import org.lazyparams.internal.LazerContext;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.*;

/**
 * @author Henrik Kaipe
 * @see LeafParameterizedJupiterTest#LeafParameterizedJupiterTest(InstallScenario,StaticScopeParam,Object,Object,Object,Object,StaticScopeParam,MaxCountsTweak)
 * @see LeafParameterizedJupiterTest#tweaks()
 */
public enum ResultTweak
implements TestExecutionExceptionHandler, BeforeEachCallback, AfterEachCallback, AfterAllCallback {
    NONE {
        @Override
        public void beforeEach(ExtensionContext ec) {
            /* Regular non-tweaked outcomes prevails! */
        }
        @Override
        void modifyExpectations(List<VerifyJupiterRule.ResultVerifier> expectations) {
            /* Regular non-tweaked outcomes prevails! */
        }
    },
    FAIL_FIRST {
        @Override
        public void afterEach(ExtensionContext ec) {
            if (false == ec.getExecutionException().isPresent()
                    && 1 == onMethodCount.get().intValue()) {
                throw new AssertionError(this.name());
            }
        }
        @Override
        void modifyExpectations(List<VerifyJupiterRule.ResultVerifier> expectations) {
            VerifyJupiterRule.ResultVerifier first = expectations.get(0);
            if (null == first.getMessageRgx()) {
                first.modifyExpectedFailureMessageRgx(this.name());
            }
            for (int i = 1; i < expectations.size(); ++i) {
                VerifyJupiterRule.ResultVerifier summaryCandidate = expectations.get(i);
                if (first.displayNameRgx.startsWith(summaryCandidate.displayNameRgx)) {
                    summaryCandidate.modifyExpectedFailureMessageRgx(
                            increaseFailureCountByOne(summaryCandidate.getMessageRgx(), i));
                }
            }
        }
    },
    FAIL_SECOND {
        @Override
        public void afterEach(ExtensionContext ec) {
            if (false == ec.getExecutionException().isPresent()
                    && 2 == onMethodCount.get().intValue()) {
                throw new AssertionError(FAIL_SECOND);
            }
        }
        @Override
        void modifyExpectations(List<VerifyJupiterRule.ResultVerifier> expectations) {
            if (expectations.size() < 2) {
                return;
            }
            VerifyJupiterRule.ResultVerifier second = expectations.get(1);
            if (null == second.getMessageRgx()) {
                second.modifyExpectedFailureMessageRgx(this.name());
                expectations.stream().skip(2)
                        .filter(summaryCandidate -> second.displayNameRgx
                                .startsWith(summaryCandidate.displayNameRgx))
                        .findFirst().ifPresent(resultSummary -> {
                    resultSummary.modifyExpectedFailureMessageRgx(increaseFailureCountByOne(
                            resultSummary.getMessageRgx(),
                            expectations.indexOf(resultSummary)));
                });
            }
        }
    },
    FAIL_LAST {
        @Override
        public void afterEach(ExtensionContext ec) {
            try {
                if (false == ec.getExecutionException().isPresent()
                        && null != latestMethod.get()
                        && false == LazerContext.resolveLazer().pendingCombinations()) {
                    throw new RuntimeException(name());
                }
            } catch (Lazer.ExpectedParameterRepetition ex) {
                throw new Error(ex);
            }
        }
        @Override
        void modifyExpectations(List<VerifyJupiterRule.ResultVerifier> expectations) {
            int earliestNoSummary = 0;
            for (int i = 1; i < expectations.size(); ++i) {
                VerifyJupiterRule.ResultVerifier
                        lastCandidate = expectations.get(i - 1),
                        summaryCandidate = expectations.get(i);
                if (lastCandidate.displayNameRgx.startsWith(summaryCandidate.displayNameRgx)) {
                    /* Identified last and summary! */
                    if (null == lastCandidate.getMessageRgx()) {
                        lastCandidate.modifyExpectedFailureMessageRgx(this.name());
                        String oldSummaryRgx = summaryCandidate.getMessageRgx();
                        int totalCount = i - earliestNoSummary;
                        summaryCandidate.modifyExpectedFailureMessageRgx(
                                increaseFailureCountByOne(oldSummaryRgx, totalCount));
                    }
                    earliestNoSummary = ++i; // Avoid summary as candidate for last!
                }
            }
        }
    },
    PASS_ALL {
        @Override
        public void handleTestExecutionException(ExtensionContext context, Throwable throwable) {
            /* Simply swallow all exceptions! */
        }
        @Override
        void modifyExpectations(List<VerifyJupiterRule.ResultVerifier> expectations) {
            expectations.forEach(exp -> exp.modifyExpectedFailureMessageRgx(null));
        }
    },
    FAIL_FIRST_ONLY {
        @Override
        public void handleTestExecutionException(ExtensionContext ec, Throwable throwable)
        throws Throwable {
            FAIL_FIRST.only(ec, throwable);
        }
        @Override
        public void afterEach(ExtensionContext ec) {
            FAIL_FIRST.only(ec);
        }
        @Override
        void modifyExpectations(List<VerifyJupiterRule.ResultVerifier> expectations) {
            String firstMessageExpectation = expectations.get(0).getMessageRgx();
            PASS_ALL.modifyExpectations(expectations);
            FAIL_FIRST.modifyExpectations(expectations);
            if (null != firstMessageExpectation) {
                expectations.get(0).modifyExpectedFailureMessageRgx(firstMessageExpectation);
            }
        }
    },
    FAIL_SECOND_ONLY {
        @Override
        public void handleTestExecutionException(ExtensionContext ec, Throwable throwable)
        throws Throwable {
            FAIL_SECOND.only(ec, throwable);
        }
        @Override
        public void afterEach(ExtensionContext ec) {
            FAIL_SECOND.only(ec);
        }
        @Override
        void modifyExpectations(List<VerifyJupiterRule.ResultVerifier> expectations) {
            if (3 <= expectations.size()) {
                String secondMessageExpectation = expectations.get(1).getMessageRgx();
                PASS_ALL.modifyExpectations(expectations);
                FAIL_SECOND.modifyExpectations(expectations);
                if (null != secondMessageExpectation) {
                    expectations.get(1)
                            .modifyExpectedFailureMessageRgx(secondMessageExpectation);
                }
            } else {
                PASS_ALL.modifyExpectations(expectations);
            }
        }
    },
    FAIL_LAST_ONLY {
        @Override
        public void handleTestExecutionException(ExtensionContext ec, Throwable throwable)
        throws Throwable {
            FAIL_LAST.only(ec, throwable);
        }
        @Override
        public void afterEach(ExtensionContext ec) {
            FAIL_LAST.only(ec);
        }
        @Override
        void modifyExpectations(List<VerifyJupiterRule.ResultVerifier> expectations) {
            int earliestNoSummary = 0;
            for (int i = 1; i < expectations.size(); ++i) {
                VerifyJupiterRule.ResultVerifier
                        lastCandidate = expectations.get(i - 1),
                        summaryCandidate = expectations.get(i);
                String lastMessageExpectation = lastCandidate.getMessageRgx();
                lastCandidate.modifyExpectedFailureMessageRgx(null);
                if (lastCandidate.displayNameRgx.startsWith(summaryCandidate.displayNameRgx)) {
                    /* Identified last and summary! */
                    summaryCandidate.modifyExpectedFailureMessageRgx(null);
                    FAIL_LAST.modifyExpectations(expectations.subList(
                            earliestNoSummary, earliestNoSummary = ++i));
                    if (null != lastMessageExpectation) {
                        lastCandidate.modifyExpectedFailureMessageRgx(lastMessageExpectation);
                    }
                }
            }
        }
    },
    FAIL_ALL {
        @Override
        public void afterEach(ExtensionContext ec) {
            if (ec.getTestMethod().filter(m -> m.isAnnotationPresent(TestFactory.class))
                    .isPresent()) {
                return;
            } else {
                throw new AssertionError(this.name());
            }
        }
        @Override
        void modifyExpectations(List<VerifyJupiterRule.ResultVerifier> expectations) {
            int countSincePreviousSummary = 0;
            int failureAddCount = 0;
            String latestNonSummaryDisplayName = "";
            for (VerifyJupiterRule.ResultVerifier verifier : expectations) {
                if (latestNonSummaryDisplayName.startsWith(verifier.displayNameRgx)) {
                    /* This is a summary! */
                    while (0 < failureAddCount) {
                        --failureAddCount;
                        verifier.modifyExpectedFailureMessageRgx(increaseFailureCountByOne(
                                verifier.getMessageRgx(), countSincePreviousSummary));
                    }
                    countSincePreviousSummary = 0;
                    latestNonSummaryDisplayName = "";
                } else {
                    ++countSincePreviousSummary;
                    latestNonSummaryDisplayName = verifier.displayNameRgx;
                    if (null == verifier.getMessageRgx()) {
                        ++failureAddCount;
                        verifier.modifyExpectedFailureMessageRgx(this.name());
                    }
                }
            }
            if (1 == failureAddCount && 1 == countSincePreviousSummary) {
                /* This indicates the final questionable pass in case of
                 * Jupiter parameterization: */
                expectations.get(expectations.size() - 1)
                        .modifyExpectedFailureMessageRgx(null);
            }
        }
    };

    static final ThreadLocal<Method> latestMethod = new ThreadLocal<>();
    static final ThreadLocal<AtomicInteger> onMethodCount = new ThreadLocal<AtomicInteger>() {
        @Override protected AtomicInteger initialValue() {
            return new AtomicInteger(0);
        }
    };

    static final Pattern oneTestFailed = Pattern
            .compile("^1 test.?.?(?= fail)");
    static final Pattern multipleTestsFailed = Pattern
            .compile("^\\d++(?= tests fail)");

    static String increaseFailureCountByOne(String messageRgx, int total) {
        if (null == messageRgx) {
            return "1 tests? failed.*total.*" + total + ".*";
        } else {
            Matcher m = multipleTestsFailed.matcher(messageRgx);
            if (m.find()) {
                return (Integer.parseInt(m.group()) + 1)
                        + messageRgx.substring(m.end());
            } else {
                m = oneTestFailed.matcher(messageRgx);
                if (false == m.find()) {
                    throw new Error("Unable to parse summary: " + messageRgx);
                }
                return "2 tests" + messageRgx.substring(m.end());
            }
        }
    }

    static ExtensionContext hideExecutionExceptionOf(ExtensionContext ec) {
        if (ec.getExecutionException().isPresent()) {
            ec = mock(ExtensionContext.class, withSettings()
                    .defaultAnswer(delegatesTo(ec)).stubOnly());
            when(ec.getExecutionException()).thenReturn(Optional.empty());
        }
        return ec;
    }

    void only(ExtensionContext ec, Throwable throwable) throws Throwable {
        try {
            afterEach(hideExecutionExceptionOf(ec));
        } catch (AssertionError | RuntimeException ___$) {
            throw throwable;
        }
    }

    void only(ExtensionContext ec) {
        if (false == ec.getExecutionException().isPresent()) {
            afterEach(ec);
        }
    }

    @Override
    public void beforeEach(ExtensionContext ec) {
        Method testMethod = ec.getTestMethod().orElse(null);
        if (null == testMethod) {
            return;
        } else if (testMethod != latestMethod.get()) {
            latestMethod.set(testMethod);
            onMethodCount.get().set(0);
        }
        if (testMethod.isAnnotationPresent(TestFactory.class)) {
            assumeTrue(this == ResultTweak.NONE,
                    "These tweaks dont work for Jupiter test-factory");
        }
        onMethodCount.get().incrementAndGet();
    }

    @Override
    public void afterEach(ExtensionContext ec) {}

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable)
    throws Throwable {
        throw throwable;
    }

    @Override
    public void afterAll(ExtensionContext ec) {
        latestMethod.remove();
        onMethodCount.remove();
    }

    final void modifyExpectations(VerifyJupiterRule verifyRule) {
        afterAll(null);//To reset everything again before running test with tweaks!
        if (verifyRule.getMethod().isAnnotationPresent(TestFactory.class)) {
            Assume.assumeTrue("These tweaks dont work for Jupiter test-factory",
                    this == ResultTweak.NONE);
        }
        modifyExpectations(verifyRule.getExpectations());
    }

    abstract void modifyExpectations(List<VerifyJupiterRule.ResultVerifier> expectations);
}
