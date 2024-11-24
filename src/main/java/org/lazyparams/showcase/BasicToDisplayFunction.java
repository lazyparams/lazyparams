/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.showcase;

import java.lang.reflect.Array;
import java.util.Arrays;
import org.lazyparams.LazyParams;
import org.lazyparams.ToDisplayFunction;
import org.lazyparams.config.Configuration;

/**
 * @author Henrik Kaipe
 */
class BasicToDisplayFunction<T> implements ToDisplayFunction<T> {

    /** Used by {@link ScopedLazyParameter.FactoryHandler#parameterId(java.util.List,String,ToDisplayFunction)}*/
    final String parameterName;

    private final String valuePrefix;

    BasicToDisplayFunction(String parameterName) {
        this.parameterName = parameterName;

        StringBuilder buildPrefix = new StringBuilder(parameterName).append('=');
        Configuration scopedConfig = LazyParams.currentScopeConfiguration();
        if (false == scopedConfig.alsoUseValueDisplaySeparatorBeforeToDisplayFunction()) {
            this.valuePrefix = scopedConfig.getValueDisplaySeparator() + buildPrefix;
        } else {
            this.valuePrefix = buildPrefix.toString();
        }
    }

    private Object toPrettyString(Object value) {
        if (null == value || false == value.getClass().isArray()) {
            return value;
        } else if (value instanceof Object[]) {
            return Arrays.deepToString((Object[])value);
        }
        Object[] valueArray = new Object[Array.getLength(value)];
        for (int i = 0; i < valueArray.length; ++i) {
            valueArray[i] = Array.get(value, i);
        }
        return Arrays.toString(valueArray);
    }

    @Override
    public CharSequence apply(T value) {
        return valuePrefix + toPrettyString(value);
    }
}
