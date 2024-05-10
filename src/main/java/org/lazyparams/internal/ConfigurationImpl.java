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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lazyparams.config.Configuration;
import org.lazyparams.config.ReadableConfiguration;

/**
 * @author Henrik Kaipe
 */
class ConfigurationImpl extends Configuration {

    private final ReadableConfiguration parentConfiguration;
    private final Map<Object,RetirableValue<?>> customConfigurations =
            new HashMap<Object, RetirableValue<?>>();
    private final List<Throwable> retirementFailures = Collections
            .synchronizedList(new ArrayList<Throwable>());
    private volatile Object retiredBy;

    ConfigurationImpl(ReadableConfiguration parentConfiguration) {
        this.parentConfiguration = parentConfiguration;
    }

    @Override
    protected ReadableConfiguration parentConfiguration() {
        return parentConfiguration;
    }

    @Override
    public <V> V getScopedCustomItem(Object configurationKey) {
        RetirableValue<?> value = customConfigurations.get(configurationKey);
        if (null != value) {
            return (V) value.value;
        }
        ReadableConfiguration parentConfig = parentConfiguration();
        return parentConfig instanceof Configuration
                ? (V) ((Configuration)parentConfig)
                        .getScopedCustomItem(configurationKey)
                : null;
    }

    @Override
    protected <V> void internalSetScopedCustomItem(Object configurationKey,
            V configurationValue, ScopeRetirementPlan<? super V> onScopeRetirement) {
        RetirableValue<V> newConfiguration = new RetirableValue<V>(
                configurationValue, onScopeRetirement);
        RetirableValue<?> retiredConfiguration;
        synchronized (customConfigurations) {
            if (isRetired()) {
                newConfiguration.retire(retirementFailures);
                return;
            }
            retiredConfiguration = customConfigurations.put(
                    configurationKey, newConfiguration);
        }
        if (null != retiredConfiguration
                && retiredConfiguration.value != configurationValue) {
            retiredConfiguration.retire(retirementFailures);
        }
    }

    boolean isRetired() {
        return null != retiredBy;
    }
    boolean isRetiredBy(Object scopeRef) {
        return scopeRef == retiredBy;
    }

    List<Throwable> retire(Object scopreRef) {
        synchronized (customConfigurations) {
            if (null == retiredBy) {
                retiredBy = scopreRef;
                for (RetirableValue<?> eachValue : customConfigurations.values()) {
                    eachValue.retire(retirementFailures);
                }
            }
        }
        return retirementFailures;
    }

    private static class RetirableValue<T> {
        final T value;
        private final ScopeRetirementPlan<? super T> retirementPlan;

        RetirableValue(T value, ScopeRetirementPlan<? super T> retirementPlan) {
            this.value = value;
            this.retirementPlan = retirementPlan;
        }

        void retire(Collection<Throwable> failureCollection) {
            try {
                retirementPlan.apply(value);
            } catch (Throwable t) {
                failureCollection.add(t);
            }
        }
    }
}
