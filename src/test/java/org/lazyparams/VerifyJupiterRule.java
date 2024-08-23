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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.junit.Assume;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.lazyparams.internal.ProvideJunitPlatformHierarchical;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Henrik Kaipe
 */
public class VerifyJupiterRule implements TestRule {

    private static final Pattern cdataIncompability = Pattern.compile("\\]\\](?=$|\\>)");
    private static final Map<UniqueId,?> postbonedAfterInvocations =
            mapOnAdviceRepeatableNode("postbonedAfterInvocations");

    final Class<?> testClassToVerify;
    private String methodName;
    private DiscoverySelector selector;

    private final List<ResultVerifier> expectations = new ArrayList<>();
    private final List<Consumer<VerifyJupiterRule>> expectationsTweakers = new ArrayList<>();

    private String defaultPrefix;

    public VerifyJupiterRule(Class<?> testClassToVerify) {
        this.testClassToVerify = testClassToVerify;
    }

    private static <T> Map<UniqueId,T> mapOnAdviceRepeatableNode(String fieldName) {
        try {
            Field mapField = ProvideJunitPlatformHierarchical.AdviceRepeatableNode.class
                    .getDeclaredField(fieldName);
            mapField.setAccessible(true);
            return (Map<UniqueId,T>) mapField.get(null);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public NextResult methodParameterTypes(Class<?>... parameterTypes) {
        String parameterTypesText = Stream.of(parameterTypes)
                .map(Class::getName)
                .collect(Collectors.joining(","));
//        System.out.println("\nParameter-types for method " + methodName
//                + ": " + parameterTypesText);
        selector = DiscoverySelectors.selectMethod(
                testClassToVerify, methodName, parameterTypesText);
        defaultPrefix = defaultPrefix.replaceFirst("\\W.++",
                "\\(([^,)]++\\,){" + (parameterTypes.length - 1)
                + "}[^,)]++\\)");
        return new NextResult();
    }

    public void addTweaker(Consumer<VerifyJupiterRule> tweaker) {
        expectationsTweakers.add(tweaker);
    }

    public Method getMethod() {
        return ((MethodSelector)selector).getJavaMethod();
    }

    public Class<?> getTestClass() {
        return testClassToVerify;
    }

    @SuppressWarnings("NonPublicExported")
    public List<ResultVerifier> getExpectations() {
        return expectations;
    }

    private void assertPendingAfterInvocations(Matcher<? extends Iterable<?>> expected) {
        assertThat("Descriptors with pending after invocations",
                new ArrayList(postbonedAfterInvocations.keySet()) {
                    /** Prevents broken JUnit XML-report ... */
                    @Override public String toString() {
                        return super.toString().replace("]]", "]}");
                    }
                }, (Matcher) expected);
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        methodName = description.getMethodName();
        int methodNameEnd = methodName.indexOf('[');
        if (0 < methodNameEnd) {
            /* An issue with method-name when using runner Parameterized! */
            methodName = methodName.substring(0, methodNameEnd);
        }

        /* Default selector grabs for same method-name inside testClassToVerify: */
        selector = DiscoverySelectors.selectMethod(testClassToVerify, methodName);
        defaultPrefix = methodName + "(\\(\\))?";

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                base.evaluate();
                /* Clean up! Otherwise end of test verifications risk being
                 * confused with remnants of previous tests */
                postbonedAfterInvocations.clear();

                expectationsTweakers.forEach(tweaker
                        -> tweaker.accept(VerifyJupiterRule.this));

                customizeBeforeLaunch();

                firstStaticScopeParameterDuringAfterAllRequires_Jupiter_5_8_orLaterForTestTemplates();

                ListIterator<ResultVerifier> iter = expectations.listIterator();
                while (iter.hasNext()) {
                    iter.next().setResultNumber(iter.nextIndex());
                }
                if (expectations.isEmpty()) {
                    fail(testClassToVerify.getSimpleName(),
                            testClassToVerify.getName());
                } else if (false == testClassToVerify.getName()
                        .equals(iter.previous().legacyNameRgx)) {
                    String classLegacyStart = testClassToVerify.getName() + ' ';
                    /* Setup expected class summary: */
                    int expectedFails = 0, expectedTotal = 0;
                    for (ResultVerifier verifier : expectations) {
                        if (verifier.legacyNameRgx.startsWith(classLegacyStart)) {
                            ++expectedTotal;
                            if (null != verifier.getMessageRgx()) {
                                ++expectedFails;
                            }
                        }
                    }
                    if (expectedFails <= 0) {
                        pass(testClassToVerify.getSimpleName(),
                                testClassToVerify.getName());
                    } else if (2 <= expectedTotal) {
                        fail(testClassToVerify.getSimpleName(),
                                testClassToVerify.getName())
                                .withMessage(expectedFails + " test"
                                + ".*total " + expectedTotal + "\\D*");
                    } else {
                        fail(testClassToVerify.getSimpleName(),
                                testClassToVerify.getName());
                    }
                    expectations.get(expectations.size() - 1)
                            .setResultNumber(expectations.size());
                }

                LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder
                        .request().selectors(selector).build();
                final Launcher launcher = LauncherFactory.create(LauncherConfig.builder()
                        .enableTestEngineAutoRegistration(false)
                        .addTestEngines(new JupiterTestEngine())
                        .build());

                final AtomicReference<Throwable> verifyFailure = new AtomicReference<>();
                final TestExecutionListener listener = new TestExecutionListener() {
                    final Set<TestIdentifier> dynamiclyRegistered = newIdentitySet();
                    final Set<TestIdentifier> started = newIdentitySet();

                    <T> Set<T> newIdentitySet() {
                        final Map<T,Object> delegateMap = new IdentityHashMap<>();
                        return new AbstractSet<T>() {
                            @Override
                            public Iterator<T> iterator() {
                                return delegateMap.keySet().iterator();
                            }
                            @Override
                            public int size() {
                                return delegateMap.size();
                            }
                            @Override @SuppressWarnings("element-type-mismatch")
                            public boolean remove(Object o) {
                                return null != delegateMap.remove(o);
                            }
                            @Override
                            public boolean add(T e) {
                                return null == delegateMap.put(e, "");
                            }
                            /**
                             * Overridden in order to not break JUnit XML reports
                             */
                            @Override public String toString() {
                                return cdataIncompability.matcher(super.toString())
                                        .replaceAll("] ]");
                            }
                        };
                    }

                    /**
                     * Ugly!
                     * What would have been desirable is to use something
                     * equivalent to the "pleaseStop" feature of JUnit4
                     * but as of now it seems JUnit5 does not have it.
                     */
                    void collect1stFailure(Runnable eventVerification) {
                        if (null == verifyFailure.get()) {
                            try {
                                eventVerification.run();
                            } catch (Throwable failure) {
                                verifyFailure.set(failure);
                            }
                        }
                    }

                    @Override
                    public void dynamicTestRegistered(TestIdentifier testIdentifier) {
//                        System.out.println("Dynamic " + testIdentifier.getDisplayName()
//                                + " ;; " + testIdentifier.getLegacyReportingName());
                        collect1stFailure(() -> {
                            if (false == testIdentifier.getUniqueId().contains("LazyParams")
                                    || testIdentifier.getSource().isPresent()) {
                                String parentUniqueId = testIdentifier
                                        .getParentId().orElseThrow(()
                                                -> new AssertionError("Dynamicly registered "
                                                        + testIdentifier + " has no parent!"));
                                if (started.stream()
                                        .map(TestIdentifier::getUniqueId)
                                        .noneMatch(parentUniqueId::equals)) {
                                    throw new AssertionError("Parent of dynamicly registered "
                                            + testIdentifier + " is not started!");
                                }
                            }
                            dynamiclyRegistered.add(testIdentifier);
                        });
                    }

                    @Override
                    public void executionStarted(TestIdentifier testIdentifier) {
                        if (false == testIdentifier.getParentId().isPresent()) {
//                            System.out.println("Don't track engine: "
//                                    + testIdentifier.getDisplayName());
                            return;
                        }
//                        System.out.println("Starting " + testIdentifier.getDisplayName()
//                                + " ;; " + testIdentifier.getLegacyReportingName());
//                        System.out.println(" ... of source " + testIdentifier.getSource()
//                                .map(org.junit.platform.engine.TestSource::toString).orElse("'none'"));
                        collect1stFailure(() -> {
                            if (started.stream().map(TestIdentifier::getSource).anyMatch(src
                                    -> src.filter(MethodSource.class::isInstance).isPresent())) {
                                assertTrue(dynamiclyRegistered.remove(testIdentifier),
                                        "A nested test (" + testIdentifier + ")"
                                        + "\nmust have been dynamicly registered!");
                                started.add(testIdentifier);
                            } else if (dynamiclyRegistered.remove(testIdentifier)
                                    || testIdentifier.getSource()
                                    .filter(MethodSource.class::isInstance).isPresent()
                                    || testIdentifier.getSource()
                                    .filter(ClassSource.class::isInstance).isPresent()
                                    ) {
                                started.add(testIdentifier);
                            }
                        });
                    }

                    @Override
                    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
                        if (false == testIdentifier.getParentId().isPresent()) {
//                            System.out.println("Don't track engine: "
//                                    + testIdentifier.getDisplayName());
                            return;
                        }
//                        System.out.println("Finishing " + testIdentifier.getDisplayName()
//                                + " ;; " + testIdentifier.getLegacyReportingName());
//                        System.out.println(" ... of source " + testIdentifier.getSource()
//                                .map(org.junit.platform.engine.TestSource::toString).orElse("'none'"));
                        collect1stFailure(() -> {
                            if (expectations.isEmpty()) {
                                assertFalse(testIdentifier.getSource()
                                        .filter(MethodSource.class::isInstance).isPresent(),
                                        "No more method-sourced results are expected!");
                                assertThat("Dynamic tests that are not yet started",
                                        dynamiclyRegistered, empty());
                                assertThat("Started tests that have not finished",
                                        started, empty());

                            } else {
                                if (false == started.remove(testIdentifier)) {
                                    throw new AssertionError("Finished test "
                                            + testIdentifier + "\nwas never started!");
                                }
                                expectations.remove(0).verifyResult(
                                        testIdentifier, testExecutionResult);
                            }
                            assertPendingAfterInvocations(
                                    not(hasItem(testIdentifier.getUniqueId())));
                        });
                    }

                    @Override public String toString() {
                        return "" + expectations;
                    }
                };

                launcher.execute(request, listener);

                Throwable firstVerifyFailure = verifyFailure.get();
                if (null != firstVerifyFailure) {
                    postbonedAfterInvocations.clear();
                    throw firstVerifyFailure;

                }
                if (false == expectations.isEmpty()) {
                    postbonedAfterInvocations.clear();
                    throw new AssertionError("There are " + expectations.size()
                            + " pending verifications!!");
                }
                try {
                    assertPendingAfterInvocations(empty());
                } finally {
                    postbonedAfterInvocations.clear();
                }
            }
        };
    }

    /**
     * To introduce first static parameter during @AfterAll does not work
     * for @TestTemplate tests (e.g. @ParamterizedTest, @RepeatedTest)
     * unless Jupiter-5.8 or later is used!
     * There are no plans to fix this, because it only concerns old Jupiter
     * releases and because introduction of parameters during
     * @AfterAll is considered a rare corner-case.
     * Instead we just attempt to ignore such test executions!
     */
    private void firstStaticScopeParameterDuringAfterAllRequires_Jupiter_5_8_orLaterForTestTemplates() {

        try {
            if (0 < TestClassOrder.class.getName().length()) {
//                System.out.println("On Jupiter-5.8 or later - so no worries");
                return;
            }
        } catch (Throwable onJupiter_5_7_orEarlier) {}
        if (Stream.of(getMethod().getAnnotations())
                .flatMap(a -> Stream.of(a.annotationType().getAnnotations()))
                .noneMatch(TestTemplate.class::isInstance)) {
//            System.out.println(getMethod()
//                    + " has no TestTemplate-annotation - so not a concern");
            return;
        }
        if (expectations.get(0).displayNameRgx.contains("/")) {
//            System.out.println("Static parameter applied before method exeuction,"
//                    + " so is probably fully supported");
            return;
        }

        Assume.assumeFalse("Jupiter-5.8 or later is required for"
                + " @TestTemplate-tests (e.g. @ParameterizedTest, @RepeatedTest)"
                + " that introduce static scope parameterization during @AfterAll",
                1 < expectations.stream().map(v -> v.displayNameRgx)
                        .filter(s -> s.contains("/")).count());
    }

    protected void customizeBeforeLaunch() {}

    private String prefixAdjusted(String appendix) {
        return null == appendix || 1 <= appendix.length() && ' ' != appendix.charAt(0)
                ? appendix
                : defaultPrefix + appendix;
    }

    public NextResult pass(String nameRgx) {
        return pass(nameRgx,nameRgx);
    }
    public NextResult pass(String displayNameRegex, String legacyNameRegex) {
        displayNameRegex = prefixAdjusted(displayNameRegex);
        legacyNameRegex = prefixAdjusted(legacyNameRegex);
        expectations.add(new ResultVerifier(
                displayNameRegex, legacyNameRegex,
                null));
        return new NextResult();
    }
    public SpecifyFailMessageOrNextResult fail(String nameRgx) {
        return fail(nameRgx, nameRgx);
    }
    public SpecifyFailMessageOrNextResult fail(
            String displayNameRegex, String legacyNameRegex) {
        displayNameRegex = prefixAdjusted(displayNameRegex);
        legacyNameRegex = prefixAdjusted(legacyNameRegex);
        ResultVerifier expectedFailure = new ResultVerifier(
                displayNameRegex, legacyNameRegex,
                ".*");
        expectations.add(expectedFailure);
        return new SpecifyFailMessageOrNextResult(
                expectedFailure::modifyExpectedFailureMessageRgx);
    }

    public class NextResult {
        public NextResult pass(String nameRxg) {
            return VerifyJupiterRule.this.pass(nameRxg);
        }
        public NextResult pass(String displayNameRegex, String legacyNameRegex) {
            return VerifyJupiterRule.this.pass(displayNameRegex, legacyNameRegex);
        }
        public SpecifyFailMessageOrNextResult fail(String nameRgx) {
            return VerifyJupiterRule.this.fail(nameRgx);
        }
        public SpecifyFailMessageOrNextResult fail(
                String displayNameRegex, String legacyNameRegex) {
            return VerifyJupiterRule.this.fail(displayNameRegex, legacyNameRegex);
        }
    }

    public class SpecifyFailMessageOrNextResult extends NextResult {
        private final AtomicReference<Consumer<String>> messageRgxSetterRef;

        public SpecifyFailMessageOrNextResult(Consumer<String> messageRgxSetter) {
            this.messageRgxSetterRef = new AtomicReference<>(messageRgxSetter);
        }

        public NextResult withMessage(String messageRgx) {
            Consumer<String> messageRgxSetter = messageRgxSetterRef.getAndSet(null);
            if (null == messageRgxSetter) {
                throw new IllegalStateException(
                        "Expected message pattern is already specified");
            }
            messageRgxSetter.accept(messageRgx);
            return new NextResult();
        }
    }

    protected static class ResultVerifier implements BiFunction<String,String,Consumer<TestExecutionResult>> {
        public final String displayNameRgx;
        public final String legacyNameRgx;
        private String messageRgx;
        private int resultNumber;

        public ResultVerifier(
                String expectedDisplayNameRgx,
                String expectedLegacyNameRgx,
                String expectedMessageRgx) {
            this.displayNameRgx = expectedDisplayNameRgx;
            this.legacyNameRgx = expectedLegacyNameRgx;
            this.messageRgx = expectedMessageRgx;
        }

        public String getMessageRgx() {
            return messageRgx;
        }

        void modifyExpectedFailureMessageRgx(String messageRgxReplacement) {
            this.messageRgx = messageRgxReplacement;
        }

        void setResultNumber(int resultNumber) {
            this.resultNumber = resultNumber;
        }

        void verifyResult(TestIdentifier testId, TestExecutionResult result) {
            apply(testId.getDisplayName(), testId.getLegacyReportingName())
                    .accept(result);
        }
        @Override
        public Consumer<TestExecutionResult> apply(
                String actualDisplayName, String actualLegacyName) {
            org.assertj.core.api.Assertions.assertThat(actualDisplayName)
                    .as(actual("display-name"))
                    .matches(displayNameRgx);
            org.assertj.core.api.Assertions.assertThat(actualLegacyName)
                    .as(actual("legacy-name"))
                    .matches(legacyNameRgx);

            String desc = actual("(for " + actualDisplayName + ")");
            if (null == messageRgx) {
                return actualResult -> assertThat(desc, actualResult,
                        sameInstance(TestExecutionResult.successful()));
            } else {
                return actualResult -> {
                    assertThat(desc + " expect failure " + messageRgx,
                            actualResult.getStatus(),
                            sameInstance(TestExecutionResult.Status.FAILED));
                    org.assertj.core.api.Assertions
                            .assertThat(actualResult.getThrowable().map(Throwable::getMessage).orElse("null"))
                            .as(desc + " message")
                            .matches("(?s)" + messageRgx);
                };
            }
        }

        private String actual(String resultProperty) {
            return "Result #" + resultNumber + " " + resultProperty;
        }

        @Override public String toString() {
            return displayNameRgx + ':' + messageRgx;
        }
    }
}
