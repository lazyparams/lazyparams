/*
 * Copyright 2024-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.config;

/**
 * @author Henrik Kaipe
 * @see org.lazyparams.LazyParams#currentScopeConfiguration()
 */
public abstract class Configuration implements ReadableConfiguration {

    /** @hidden */
    public static final Configuration GLOBAL_CONFIGURATION = new Configuration() {
        @Override
        protected ReadableConfiguration parentConfiguration() {
            return GLOBAL_DEFAULTS;
        }
        @Override
        public <V> V getScopedCustomItem(Object configurationKey) {
            return null;
        }
        @Override
        protected <V> void internalSetScopedCustomItem(Object configurationKey,
                V configurationValue, ScopeRetirementPlan<? super V> onScopeRetirement) {
            throw new UnsupportedOperationException("Not supported for global configuration!");
        }
    };
    private static final ScopeRetirementPlan<Object> NO_RETIREMENT_PLAN = new ScopeRetirementPlan() {
        @Override public void apply(Object retiredCustomConfiguration) {}
    };

    private int maxFailureCount = 0;
    private int maxTotalCount = 0;
    private String valueDisplaySeparator = null;
    private Boolean alsoUseValueDisplaySeparatorBeforeToDisplayFunction = null;

    private final ReadableConfiguration virtualScopeConfiguration = new ReadableConfiguration() {
        @Override
        public int getMaxFailureCount() {
            return 0 < maxFailureCount ? maxFailureCount
                    : parentConfiguration().getMaxFailureCount();
        }
        @Override
        public int getMaxTotalCount() {
            return 0 < maxTotalCount ? maxTotalCount
                    : parentConfiguration().getMaxTotalCount();
        }
        @Override
        public String getValueDisplaySeparator() {
            return null != valueDisplaySeparator ? valueDisplaySeparator
                    : parentConfiguration().getValueDisplaySeparator();
        }
        @Override
        public boolean alsoUseValueDisplaySeparatorBeforeToDisplayFunction() {
            return null != alsoUseValueDisplaySeparatorBeforeToDisplayFunction
                    ? alsoUseValueDisplaySeparatorBeforeToDisplayFunction
                    : parentConfiguration()
                    .alsoUseValueDisplaySeparatorBeforeToDisplayFunction();
        }
    };

    /** @hidden */
    protected abstract ReadableConfiguration parentConfiguration();

    @Override public int getMaxFailureCount() {
        return virtualScopeConfiguration.getMaxFailureCount();
    }
    public void setMaxFailureCount(int maxFailureCountOrZeroToForceParentScope) {
        this.maxFailureCount = maxFailureCountOrZeroToForceParentScope;
    }
    @Override public int getMaxTotalCount() {
        return virtualScopeConfiguration.getMaxTotalCount();
    }

    /**
     * @param maxTotalCountOrZeroToForceParentScope or 1 to simply cancel
     * parametrization on current scope, therewith just taking primary value
     * from each introduced parameter and not appending anything to test-name
     */
    public void setMaxTotalCount(int maxTotalCountOrZeroToForceParentScope) {
        this.maxTotalCount = maxTotalCountOrZeroToForceParentScope;
    }
    @Override
    public String getValueDisplaySeparator() {
        return virtualScopeConfiguration.getValueDisplaySeparator();
    }

    public void setValueDisplaySeparator(String valueDisplaySeparator) {
        this.valueDisplaySeparator = valueDisplaySeparator;
    }
    @Override
    public boolean alsoUseValueDisplaySeparatorBeforeToDisplayFunction() {
        return virtualScopeConfiguration
                .alsoUseValueDisplaySeparatorBeforeToDisplayFunction();
    }
    public void setAlsoUseValueDisplaySeparatorBeforeToDisplayFunction(Boolean alsoUseValueDisplaySeparatorBeforeToDisplayFunction) {
        this.alsoUseValueDisplaySeparatorBeforeToDisplayFunction =
                alsoUseValueDisplaySeparatorBeforeToDisplayFunction;
    }

    public abstract <V> V getScopedCustomItem(Object scopedItemKey);
    public <V> void setScopedCustomItem(Object scopedItemKey, V scopedItemValue) {
        internalSetScopedCustomItem(scopedItemKey, scopedItemValue,
                NO_RETIREMENT_PLAN);
    }
    public <V> void setScopedCustomItem(Object scopedItemKey,
            V scopedItemValue, ScopeRetirementPlan<? super V> onScopeRetirement) {
        if (null == onScopeRetirement) {
            setScopedCustomItem(scopedItemKey, scopedItemValue);
        } else {
            internalSetScopedCustomItem(scopedItemKey, scopedItemValue, onScopeRetirement);
        }
    }
    /** @hidden */
    protected abstract <V> void internalSetScopedCustomItem(Object scopedItemKey,
            V scopedItemValue, ScopeRetirementPlan<? super V> onScopeRetirement);

    @FunctionalInterface
    public interface ScopeRetirementPlan<V> {
        public void apply(V retiredCustomConfigurationValue) throws Exception;
    }
}
