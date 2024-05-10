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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.DynamicTestInvocationContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.lazyparams.config.ReadableConfiguration;
import org.lazyparams.internal.ConfigurationContext;

/**
 * @author Henrik Kaipe
 */
public enum MaxCountsTweak implements InvocationInterceptor {
    GLOBAL_DEFAULTS(
            ReadableConfiguration.GLOBAL_DEFAULTS.getMaxFailureCount(),
            ReadableConfiguration.GLOBAL_DEFAULTS.getMaxTotalCount()) {
        @Override
        void modifyExpectations(VerifyJupiterRule verifier) {
            super.modifyExpectations(verifier);
            /* Force default config by resetting global config ... */
            LazyParamsCoreUtil.globalConfiguration().setMaxFailureCount(0);
            LazyParamsCoreUtil.globalConfiguration().setMaxTotalCount(0);
            /* ... and destroy any existing scope: */
            for (Field f : ConfigurationContext.class.getDeclaredFields()) {
                int modifiers = f.getModifiers();
                if (Modifier.isStatic(modifiers)
                        && Modifier.isFinal(modifiers)
                        && ThreadLocal.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    try {
                        ((ThreadLocal)f.get(null)).remove();
                    } catch (IllegalAccessException | IllegalArgumentException ex) {
                        throw new Error(ex);
                    }
                }
            }
        }
        @Override
        void configureMaxCounts(Invocation<?> invocation) throws Throwable {
            invocation.proceed();
        }
        @Override
        BeforeAllCallback resetStaticConfigBeforeAll() {
            /* Dont!
             * because otherwise it might trigger implicit install during
             * @BeforeAll - and we dont want that! */
            return ctx -> {};
        }
    },
    FAIL_1_TOTAL_200(1,200),
    FAIL_2_TOTAL_3(2,3);

    static final String maxCountReachedMessageRgx = ".*count has reached its max.*";
    final int maxFails, maxTotal;

    MaxCountsTweak(int maxFails, int maxTotal) {
        this.maxFails = maxFails;
        this.maxTotal = maxTotal;
    }

    private <T extends VerifyJupiterRule.ResultVerifier>
            Iterable<List<T>> splitIntoSummarizedSubSections(List<T> expectations) {
        int[] sectionEnds = IntStream.range(1, expectations.size())
                .filter(i -> expectations.get(i-1).displayNameRgx
                        .startsWith(expectations.get(i).displayNameRgx)
                        || i + 1 < expectations.size()
                        && 4 <= expectations.get(i).displayNameRgx.length()
                        && false == expectations.get(i + 1).displayNameRgx
                        .startsWith(expectations.get(i).displayNameRgx.substring(0,4)))
                .map(i -> i + 1).toArray();
        List<List<T>> sections = new ArrayList<>();
        int sectionStart = 0;
        for (int sectionEnd : sectionEnds) {
            sections.add(new ArrayList(
                    expectations.subList(sectionStart, sectionEnd)));
            sectionStart = sectionEnd;
        }
        if (sectionStart < expectations.size()) {
            sections.add(new ArrayList<>(
                    expectations.subList(sectionStart, expectations.size())));
        }
        return sections;
    }

    void modifyExpectationsOnCountedSection(
            List<VerifyJupiterRule.ResultVerifier> countedSection) {
        if (countedSection.size() <= 1) {
            return;/*right away, because this is not really parameterized*/
        }

        /* Total count: */
        if (maxTotal + 1 < countedSection.size()) {
            countedSection.subList(maxTotal, countedSection.size() - 1).clear();
            countedSection.get(countedSection.size() - 1)
                    .modifyExpectedFailureMessageRgx(maxCountReachedMessageRgx);
        }
        /* Fail count: */
        int failCount = 0;
        for (Iterator<VerifyJupiterRule.ResultVerifier> iter = countedSection.iterator();
                iter.hasNext();) {
            VerifyJupiterRule.ResultVerifier next = iter.next();
            if (iter.hasNext()
                    && null != next.getMessageRgx() && maxFails <= ++failCount
                    && null != iter.next() && iter.hasNext()) {
                do {
                    iter.remove();
                    iter.next();
                } while (iter.hasNext());
                countedSection.get(countedSection.size() - 1)
                        .modifyExpectedFailureMessageRgx(maxCountReachedMessageRgx);
            }
        }
    }

    void modifyExpectations(VerifyJupiterRule verifierRule) {
        List<VerifyJupiterRule.ResultVerifier> expectations = verifierRule.getExpectations();
        Iterable<List<VerifyJupiterRule.ResultVerifier>> subSections =
                splitIntoSummarizedSubSections(expectations);
        subSections.forEach(this::modifyExpectationsOnCountedSection);
        expectations.clear();
        subSections.forEach(expectations::addAll);
    }

    void modifyAfterAllExpectations(VerifyJupiterRule verifierRule) {
        int failCount = 0, totalCount = 0;
        String staticPrefix = verifierRule.testClassToVerify.getName() + " ";
        Iterator<VerifyJupiterRule.ResultVerifier> iter =
                verifierRule.getExpectations().iterator();
        while (iter.hasNext()) {
            VerifyJupiterRule.ResultVerifier verifier = iter.next();
            if (verifier.legacyNameRgx.startsWith(staticPrefix)) {
                if (maxTotal <= ++totalCount
                        || null != verifier.getMessageRgx()
                        && maxFails <= ++failCount) {
                    break;                    
                }
            }
        }
        if (iter.hasNext()) {
            VerifyJupiterRule.ResultVerifier verifier = iter.next();
            if (false == iter.hasNext() && verifierRule.testClassToVerify
                    .getName().equals(verifier.legacyNameRgx)) {
                return;
            }
            iter.remove();
            while (iter.hasNext()) {
                iter.next(); iter.remove();
            }
            verifierRule.fail(
                    verifierRule.testClassToVerify.getSimpleName(),
                    verifierRule.testClassToVerify.getName())
                    .withMessage(maxCountReachedMessageRgx);
        }
    }

    void configureMaxCounts(Invocation<?> invocation) throws Throwable {
        try {
            invocation.proceed();
        } finally {
            LazyParams.currentScopeConfiguration().setMaxFailureCount(maxFails);
            LazyParams.currentScopeConfiguration().setMaxTotalCount(maxTotal);
        }
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        configureMaxCounts(invocation);
    }
    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        configureMaxCounts(invocation);
    }
    @Override
    public void interceptDynamicTest(Invocation<Void> invocation, DynamicTestInvocationContext invocationContext, ExtensionContext extensionContext) throws Throwable {
        configureMaxCounts(invocation);
    }

    BeforeAllCallback resetStaticConfigBeforeAll() {
        return ctx -> {
            LazyParams.currentScopeConfiguration().setMaxFailureCount(0);
            LazyParams.currentScopeConfiguration().setMaxTotalCount(0);
        };
    }
    AfterAllCallback asAfterAllCallback() {
        return ctx -> {
            try {
                configureMaxCounts(() -> "");
            } catch (Exception | Error x) {
                throw x;
            } catch (Throwable x) {
                throw new Error(x);
            }
        };
    }
}
