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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.lazyparams.LazyParams;
import org.lazyparams.LazyParamsCoreUtil;
import org.lazyparams.config.Configuration;
import org.lazyparams.core.Lazer;

/**
 * Proof-of-concept for an intended future parametrization feature that shall allow pushing
 * of the regular test-execution scope so that a new scope is opened in midair during
 * test-execution, in order to trigger immediate evaluation on how to (pairwise) combine
 * values of parameters that are introduced in this new scope, whereas parameters of
 * regular test-execution scope (core scope) keep their values until the combinations on
 * the new scope are fully evaluated.
 * This a PoC implementation relies on JUnit-4/5/6 high-level extension
 * features, e.g. there are a {@link JUnit-4 rule #junit4rule()} and a
 * {@link JUnit-5/6 Jupiter extension PushResults.Extension} that must be
 * properly used by test-class for this to work. However, the long-term
 * intention is to not rely on such boiler-plating and instead allow the
 * push and opening of new scope to be truly lazy and achievable through
 * imperative API that doesn't require any test-class extension boiler-plating.
 *
 * As of now this PoC introduces very limited functionality that just allows
 * registration of tasks that will be reported as separate repetitions of
 * the core test and new parameters are deferred to the core scope and
 * not separately combined on the new scope. Still, it's enough to allow
 * a customized assertion framework to register each individual assertion
 * as a separate test-case and therewith elegantly honor the
 * one-assertion-per-test philosophy.
 *
 * @author Henrik Kaipe
 */
public class PushResults extends Lazer {

    private static final Consumer<Lazer> currentLazerReplacer =
            new Consumer<Lazer>() {
        final Method setter;
        {
            try {
                setter = LazerContext.class.getDeclaredMethod(
                        "startNewCombinationOn", Lazer.class);
            } catch (NoSuchMethodException mustNotHappen) {
                throw new Error(mustNotHappen);
            }
        }
        @Override
        public void accept(Lazer replacementLazer) {
            setter.setAccessible(true);
            try {
                setter.invoke(null,replacementLazer);
            } catch (IllegalAccessException mustNotHappen) {
                throw new Error(mustNotHappen);
            } catch (InvocationTargetException ex) {
                throw PushResults
                        .<RuntimeException>unchecked(ex.getTargetException());
            }
        }
    };

    private final Lazer coreLazer;

    /* Properties that keep track of state as test execution repeats: */

    private final List<Runnable> pushedResultsToReport = new LinkedList<>();
    private boolean allSuccess;
    private String appendixBase;
    private int maxTotal, maxFail;

    private PushResults(Lazer coreLazer) {
        this.coreLazer = coreLazer;
    }

    /**
     * @return scoped PushResults instance; or null if nothing has been scoped
     */
    private static PushResults resolve() {
        Lazer current = LazerContext.resolveLazer();
        return current instanceof PushResults ? (PushResults)current : null;
    }

    private static <E extends Throwable> E unchecked(Throwable failure) {
        return (E) failure;
    }

    @Override
    public int pick(Object paramId, boolean combinePairwise, int numberOfValues) {
        return coreLazer.pick(paramId, combinePairwise, numberOfValues);
    }
    @Override
    public boolean pendingCombinations() throws ExpectedParameterRepetition {
        return false == pushedResultsToReport.isEmpty()
                || coreLazer.pendingCombinations();
    }
    @Override public void startNew() {
        if (pushedResultsToReport.isEmpty()
                && /*Is it already is applied?*/ null != appendixBase) {
            coreLazer.startNew();
        }
    }

    private Appendable setupAppendable(boolean success) {
        final StringBuilder appendixSuffix = new StringBuilder(appendixBase);
        if (success) {
            LazyParamsCoreUtil.displayOnSuccess(appendixSuffix, appendixSuffix);
        } else {
            LazyParamsCoreUtil.displayOnFailure(appendixSuffix, appendixSuffix);
        }
        appendixSuffix.append(" |");
        return new Appendable() {
            boolean lastWasSpace = false;

            @Override public Appendable append(CharSequence csq) {
                return append(csq, 0, csq.length());
            }
            @Override public Appendable append(CharSequence csq, int start, int end) {
                for (int i = start; i < end; ++i) {
                    append(csq.charAt(i));
                }
                return this;
            }
            @Override public Appendable append(char c) {
                if (false == Character.isWhitespace(c)) {
                    lastWasSpace = false;
                    appendixSuffix.append(
                            '<' == c ? '\uff1c' :
                            '>' == c ? '\uff1e' : c);

                } else if (false == lastWasSpace) {
                    appendixSuffix.append(' ');
                    lastWasSpace = true;
                }
                return this;
            }
        };
    }

    private void registerInternal(
            final Consumer<Appendable> testNameAppender,
            final Throwable pushedResult) {
        final boolean success = null == pushedResult;
        allSuccess &= success;
        pushedResultsToReport.add(new Runnable() {
            public void run() {
                testNameAppender.accept(setupAppendable(success));
                if (false == success) {
                    throw PushResults.<Error>unchecked(pushedResult);
                }
            }
        });
    }

    private boolean apply() {
        if (pushedResultsToReport.isEmpty()) {
            allSuccess = true;
            appendixBase = LazyParamsCoreUtil
                    .displayOnFailure(this, "").toString();
            return false;
        }

        Configuration config = LazyParams.currentScopeConfiguration();
        config.setMaxFailureCount(maxFail);
        config.setMaxTotalCount(maxTotal);
        pushedResultsToReport.remove(0).run();
        return true;
    }

    private void stageMaxes(int increasement) {
        Configuration config = LazyParams.currentScopeConfiguration();
        maxTotal = config.getMaxTotalCount() + increasement;
        if (maxTotal <= 0) {
            maxTotal = Integer.MAX_VALUE / 2;
        }
        maxFail = config.getMaxFailureCount() + increasement;
        if (maxFail <= 0) {
            maxFail = Integer.MAX_VALUE / 2;
        }
    }

    private void stageAppendixSuffix(boolean primerunSuccess) {
        Object displayKey = new Object();
        CharSequence fullAppendix = (allSuccess &= primerunSuccess)
                ? LazyParamsCoreUtil.displayOnSuccess(displayKey, "")
                : LazyParamsCoreUtil.displayOnFailure(displayKey, "");
        ((DualDisplayAppendix)fullAppendix).setResult(allSuccess);
        appendixBase = fullAppendix.length() < appendixBase.length()
                ? fullAppendix.toString()
                : fullAppendix.subSequence(
                        appendixBase.length(), fullAppendix.length()).toString();
    }

    private void stage(boolean executionSuccess) {
        if (pushedResultsToReport.isEmpty()) {
            /* Nothing to see here: */
            return;
        }

        stageAppendixSuffix(executionSuccess);
        stageMaxes(pushedResultsToReport.size() + (executionSuccess ? 0 : 1));

        if (executionSuccess) { /* Stage first assertion as result: */
            String appendixSuffixBackup = appendixBase;
            try {
                appendixBase = "";
                pushedResultsToReport.remove(0).run();
            } finally {
                appendixBase = appendixSuffixBackup;
            }
        } else {
            LazyParamsCoreUtil.displayOnFailure(new Object(), " |");
        }
    }

    static boolean enforceBefore() {
        Lazer currentLazer = LazerContext.resolveLazer();
        if (false == currentLazer instanceof PushResults) {
            currentLazer = new PushResults(currentLazer);
            currentLazerReplacer.accept(currentLazer);
        }
        return ((PushResults)currentLazer).apply();
    }

    static void enforceAfter(boolean executionSuccess) {
        ((PushResults)LazerContext.resolveLazer()).stage(executionSuccess);
    }

    public static void register(
            Consumer<Appendable> testNameAppender, Throwable pushedResult) {
        PushResults results = resolve();
        if (null != results) {
            results.registerInternal(testNameAppender, pushedResult);
        } else if (null != pushedResult) {
            throw PushResults.<Error>unchecked(pushedResult);
        }
    }

    public static <TestRule> TestRule junit4rule() {
        return (TestRule) JUnit4Rule.SINGLETON;
    }

    private enum JUnit4Rule implements org.junit.rules.TestRule { SINGLETON;
        @Override
        public Statement apply(Statement base, Description __) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    if (enforceBefore()) {
                        return;
                    }
                    try {
                        /* Primary execution: */
                        base.evaluate();
                    } catch (VirtualMachineError jvmError) {
                        throw jvmError;
                    } catch (Throwable failure) {
                        enforceAfter(false);
                        throw failure;
                    }
                    enforceAfter(true);
                }
            };
        }
    }

    /*
     * Orchestration of JUnit-5/6 Jupiter extension is more convoluted
     * than the above the JUnit4Rule enum singleton.
     */
    public static class Extension
    implements AfterEachCallback, BeforeEachCallback, InvocationInterceptor {
        private static final Object scopeKey = new Object();

        private boolean itsOn() {
            return null != LazyParams
                    .currentScopeConfiguration().getScopedCustomItem(scopeKey);
        }

        private <T> T intercept(Invocation<T> invocation) throws Throwable {
            if (itsOn()) {
                invocation.skip();
                return (T) "just a non-null value that will be ignored";
            } else {
                return invocation.proceed();
            }
        }

        @Override public <T> T interceptTestClassConstructor(Invocation<T> invocation,
                ReflectiveInvocationContext<Constructor<T>> __, ExtensionContext ___)
        throws Throwable { return intercept(invocation); }
        @Override public void interceptAfterEachMethod(Invocation<Void> invocation,
                ReflectiveInvocationContext<Method> __, ExtensionContext ___)
        throws Throwable { intercept(invocation); }
        @Override public void interceptBeforeEachMethod(Invocation<Void> invocation,
                ReflectiveInvocationContext<Method> __, ExtensionContext ___)
        throws Throwable { intercept(invocation); }
        @Override public void interceptTestMethod(Invocation<Void> invocation,
                ReflectiveInvocationContext<Method> __, ExtensionContext ___)
        throws Throwable { intercept(invocation); }
        @Override public void interceptTestTemplateMethod(Invocation<Void> invocation,
                ReflectiveInvocationContext<Method> __, ExtensionContext ___)
        throws Throwable { intercept(invocation); }

        @Override
        public void afterEach(ExtensionContext context) {
            if (false == itsOn()) {
                enforceAfter(false == context.getExecutionException().isPresent());
            }
        }

        @Override
        public void beforeEach(ExtensionContext context) {
            boolean on =/*Default value when pushed failure:*/true;
            try {
                on = enforceBefore();
            } finally {
                if (on) {
                    LazyParams.currentScopeConfiguration()
                            .setScopedCustomItem(scopeKey, "");
                }
            }
        }
    }
}
