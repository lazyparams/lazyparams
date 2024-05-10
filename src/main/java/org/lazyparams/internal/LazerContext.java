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
public class LazerContext {
    private LazerContext() {}

    private static final ThreadLocal<Lazer> pendingRepeat = new ThreadLocal<Lazer>();
    private static final WeakIdentityHashMap<Lazer,Object> cannotBeRepeated =
            new WeakIdentityHashMap<Lazer, Object>();

    private static Lazer createNewLazer() {
        Lazer newLazer = new Lazer();
        RetirementKey.LAZER.setCurrent(newLazer);
        return newLazer;
    }

    private static void startNewCombinationOn(Lazer lazer2start) {
        lazer2start.startNew();
        RetirementKey.LAZER.setCurrent(lazer2start);
    }

    static boolean preventRepeat(Lazer lazer2prevent) {
        if (lazer2prevent == pendingRepeat.get()) {
            pendingRepeat.remove();
        }
        if (cannotBeRepeated.containsKey(lazer2prevent)) {
            return false;
        } else {
            cannotBeRepeated.put(lazer2prevent, "");
            return true;
        }
    }

    /**
     * Used by {@link ScopedLifecycleProviderFacade} to inform that new scope
     * has been opened and might be in need of details from a pending repetition.
     * @param parentScopeConfig is provided in case there is another ongoing
     *         lazer session that must be temporarily suspended while execution
     *         steps into child scopes.
     * @return true if there is a pending repetition, for which an existing
     *         {@link Lazer Lazer-instance} was prepared; otherwise false
     */
    static void preparePendingRepeat(Configuration parentScopeConfig) {
        Lazer lazer4repeat = pendingRepeat.get();
        if (null != lazer4repeat) {
            startNewCombinationOn(lazer4repeat);
        } else if (null != RetirementKey.LAZER.getCurrent()) {
            createNewLazer();
        }
    }

    static boolean isPendingRepeat() throws Lazer.ExpectedParameterRepetition {
        Lazer current = RetirementKey.LAZER.getCurrent();
        return null != current && false == cannotBeRepeated.containsKey(current)
                && current.pendingCombinations();
    }

    public static Lazer resolveLazer() {
        Lazer lazer = RetirementKey.LAZER.getCurrent();
        if (null == lazer) {
            lazer = createNewLazer();
        }
        return lazer;
    }

    private enum RetirementKey implements Configuration.ScopeRetirementPlan<Lazer> {
        LAZER;

        private Configuration currentConfig() {
            return ConfigurationContext.currentTestConfiguration();
        }

        Lazer getCurrent() {
            return currentConfig().getScopedCustomItem(this);
        }
        void setCurrent(Lazer lazer) {
            currentConfig().setScopedCustomItem(this, lazer, this);
            pendingRepeat.remove();
        }

        @Override
        public void apply(Lazer lazerAtCloseOfScope) {
            try {
                if (false == cannotBeRepeated.containsKey(lazerAtCloseOfScope)
                        && lazerAtCloseOfScope.pendingCombinations()) {
                    pendingRepeat.set(lazerAtCloseOfScope);
                }
            } catch (Lazer.ExpectedParameterRepetition requiresRepeatPrevention) {
                preventRepeat(lazerAtCloseOfScope);
            }
        }
    }
}
