/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.internal;

import org.lazyparams.config.Configuration;
import org.lazyparams.core.Lazer;

/**
 * @author Henrik Kaipe
 */
public class ContextLifecycleProviderFacade<ID> {

    private static final WeakIdentityHashMap<Object,CharSequence> scopeDisplayAppendixes =
            new WeakIdentityHashMap<Object,CharSequence>();

    /**
     * Informs LazyParams on opening of a new test-execution scope.
     * @param executionScopedIdentifier identifies test and scope
     */
    public void openExecutionScope(ID executionScopedIdentifier) {
        Configuration initialScopeConfig = ConfigurationContext.currentTestConfiguration();
        boolean newScope = ConfigurationContext.openScope(executionScopedIdentifier);
        if (newScope) {
//            System.out.println("Opens " + executionScopedIdentifier);
            scopeDisplayAppendixes.remove(executionScopedIdentifier);
            LazerContext.preparePendingRepeat(initialScopeConfig);
            DisplayAppendixContext.coverParentScope(initialScopeConfig);
        }

        Configuration scopeConfig = ConfigurationContext.currentTestConfiguration();
        assertProvider(scopeConfig);
    }

    /**
     * Closes specified scope and returns true if pending parameter-values
     * should trigger repetition of closed test-exection.
     *
     * @param executionScopedIdentifier identifies test and scope
     * @param result signals success if null; otherwise presents details on failure
     * @return true if scope is closed without pending parameter values or
     *         combinations;<br/>
     *         otherwise false if closed but with pending parameter values or
     *         pending parameter value combinations that demand repetition of
     *         test-execution unless there some other circumstances to prevent it
     *         (e.g. to many failures or repetitions)
     * @throws MaxRepeatCount if there are pending parameter values or combinations
     *         but repeated total- or failure-counts have reached its max.
     * @see org.lazyparams.config.Configuration#setMaxFailureCount(int)
     * @see org.lazyparams.config.Configuration#setMaxTotalCount(int)
     */
    public boolean closeExecutionScope(ID executionScopedIdentifier, Throwable result)
    throws MaxRepeatCount {
        if (scopeDisplayAppendixes.containsKey(executionScopedIdentifier)) {
            /* Scope is already closed but this should not be a problem.
             * Just dont signal any additional pending combinations: */
            return true;
        }
        Configuration scopeConfig = ConfigurationContext.currentTestConfiguration();
        assertProvider(scopeConfig);

        CharSequence closingDisplayAppendix =
                resolveDisplayAppendixForScope(executionScopedIdentifier);
        if (closingDisplayAppendix instanceof DualDisplayAppendix) {
            ((DualDisplayAppendix) closingDisplayAppendix).setResult(null == result);
        }
        scopeDisplayAppendixes.put(executionScopedIdentifier,
                null == closingDisplayAppendix ? "" : closingDisplayAppendix.toString());
        try {
            return MaxRepeatCount.verifyCountsOnPendingLazer(null == result);
        } finally {
//            System.out.println("Closes appendixed " + executionScopedIdentifier +
//                    ": " + scopeDisplayAppendixes.get(executionScopedIdentifier));
            ConfigurationContext.retireScope(executionScopedIdentifier);
        }
    }

    /**
     * @param executionScopedIdentifier identifies test
     * @return display appendix for specified test
     */
    public CharSequence resolveDisplayAppendixForScope(ID executionScopedIdentifier) {
        CharSequence appendix = scopeDisplayAppendixes.get(executionScopedIdentifier);
        return null != appendix ? appendix : DisplayAppendixContext.resolve(false);
    }

    private void assertProvider(Configuration scopeConfig) {
        // TODO or not TODO - that is the question.
    }

    public static final class MaxRepeatCount extends Throwable {
        private static final WeakIdentityHashMap<Lazer, MaxRepeatCount> repeatCounts =
                new WeakIdentityHashMap<Lazer, MaxRepeatCount>();

        private int totalCount, failureCount;
        private String message;

        /**
         * Using {@link Configuration#setMaxTotalCount(int)} to set max total
         * count one is a way to quietly turn off LazyParams' repetition and
         * just use the primary value on each parameter introduction. Nothing
         * will be appended to test-name.
         */
        private boolean reachedMaxTotalCountAtOne;

        private MaxRepeatCount() {}

        @Override public String getMessage() {
            return message;
        }

        /**
         * Increase counts on current test if there is a Lazer with pending combination.
         * @return true if there are no pending parameter values or combinations;
         *         or false if there are pending pending parameter or combinations
         *         and no max-count constraint has been reached
         * @throws MaxRepeatCount if any of the counts exceed its configured max!
         * @see org.lazyparams.config.Configuration#setMaxFailureCount(int)
         * @see org.lazyparams.config.Configuration#setMaxTotalCount(int)
         */
        static boolean verifyCountsOnPendingLazer(boolean success) throws MaxRepeatCount {
            Lazer.ExpectedParameterRepetition inconsistency = null;
            try {
                if (false == LazerContext.isPendingRepeat()) {
                    return true;
                }
            } catch (Lazer.ExpectedParameterRepetition detectedInconsistency) {
                inconsistency = detectedInconsistency;
            }
            Lazer lazer = LazerContext.resolveLazer();
            MaxRepeatCount counter = repeatCounts.get(lazer);
            if (null != inconsistency) {
                counter.message = "Inconsistency detected at repetition count "
                        + ++counter.totalCount;
                if (1 <= counter.failureCount) {
                    counter.message += " (" + counter.failureCount
                            + (1 == counter.failureCount ? " test" : " tests")
                            + " failed)";
                }
                counter.message += "\n" + inconsistency.getMessage();
                counter.setStackTrace(inconsistency.getStackTrace());
                throw counter;
            } else if (null == counter) {
                counter = new MaxRepeatCount();
                repeatCounts.put(lazer, counter);
            }
            counter.increaseAndCheckOnConfiguredMaxCounts(success);
            if (null == counter.message) {
                return false;
            } else {
                LazerContext.preventRepeat(lazer);
                if (counter.reachedMaxTotalCountAtOne) {
                    /* Parameterization turned off - no repetition will be made: */
                    return true;
                }
                throw counter;
            }
        }

        private void increaseAndCheckOnConfiguredMaxCounts(boolean success)
        throws MaxRepeatCount {
            Configuration scopeConfig = ConfigurationContext.currentTestConfiguration();
            if (false == success
                    && scopeConfig.getMaxFailureCount() <= ++failureCount) {
                message = "Failure count has reached its max at "
                        + scopeConfig.getMaxFailureCount();
            }
            if (scopeConfig.getMaxTotalCount() <= ++totalCount) {
                message = "Total repetition count has reached its max at "
                        + scopeConfig.getMaxTotalCount();
                reachedMaxTotalCountAtOne = 1 == totalCount;
            }
        }
    }
}
