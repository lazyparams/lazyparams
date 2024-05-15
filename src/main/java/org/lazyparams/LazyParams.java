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

import org.lazyparams.config.Configuration;
import org.lazyparams.internal.ConfigurationContext;
import org.lazyparams.internal.Instrument;
import org.lazyparams.showcase.ScopedLazyParameter;

/**
 * @author Henrik Kaipe
 */
public class LazyParams {
    private LazyParams() {}

    /**
     * Used by {@link #pickValue(Enum...) pickValue(E... values)}
     */
    private static final ToDisplayFunction<Object> stringValueOf =
            new ToDisplayFunction<Object>() {
        @Override public CharSequence apply(Object value) {
            return String.valueOf(value);
        }
    };

    public static <T> T pickValue(
            ToDisplayFunction<? super T> toDisplay,
            T[] possibleParameterValues) {
        return ScopedLazyParameter
                .from(possibleParameterValues)
                .withExtraIdDetails(ExtraIdDetail.array_displayed)
                .asParameter(toDisplay).pickValue();
    }

    public static <T> T pickValue(
            final String parameterName, T[] possibleParamValues) {
        return ScopedLazyParameter
                .from(possibleParamValues)
                .withExtraIdDetails(ExtraIdDetail.array_named)
                .asParameter(parameterName).pickValue();
    }

    public static <T> T pickValue(
            ToDisplayFunction<? super T> toDisplay,
            T primaryValue, T... otherValues) {
        return ScopedLazyParameter
                .from(primaryValue, otherValues)
                .withExtraIdDetails(ExtraIdDetail.varargs_displayed)
                .asParameter(toDisplay).pickValue();
    }

    public static <T> T pickValue(
            String parameterName, T primaryValue, T... otherValues) {
        return ScopedLazyParameter
                .from(primaryValue, otherValues)
                .withExtraIdDetails(ExtraIdDetail.varargs_named)
                .asParameter(parameterName).pickValue();
    }

    /**
     * Special function to promote lazy enum parameters with minimal boiler plating.
     * To pick a parameter value from the constants of enum type <code>MyEnum</code>
     * it is sufficient to use statements like these:<pre><code>
     *   LazyParams.&lt;MyEnum&gt;pickValue()
     *   MyEnum value = LazyParams.pickValue()
     * </code></pre>
     * Parameter value toString() result will be displayed. (Default
     * {@link Enum#toString()} implementation returns constant name.)
     */
    public static <E extends Enum<E>> E pickValue(E... values) {
        E[] frozenValues = values.clone();
        Class<E> enumType = (Class<E>) values.getClass().getComponentType();
        StringBuilder sb = new StringBuilder(enumType.getName());
        for (Enum e : frozenValues) {
            sb.append('\n').append(e.ordinal()).append(e.getClass().getName());
        }
        final String idToString = sb.toString();
        final Object explicitId = new Object() {
            @Override
            public String toString() {
                return idToString;
            }
            @Override
            public boolean equals(Object obj) {
                return this.getClass().getName().equals(obj.getClass().getName())
                        && toString().equals(obj.toString());
            }
            @Override
            public int hashCode() {
                return 203 + toString().hashCode();
            }
        };
        if (0 == frozenValues.length && false == enumType.isEnum()) {
            throw new IllegalArgumentException("Not an enum type: " + enumType);
        }
        return ScopedLazyParameter
                .from(0 == frozenValues.length ? (E[])enumType.getEnumConstants() : frozenValues)
                .withExplicitParameterId(explicitId)
                .asParameter(stringValueOf).pickValue();
    }

    /**
     * To explicitly install LazyParams with this method is not strictly
     * necessary, because it will be implicitly installed anyway on initial
     * usage as soon as the very first parameter value is picked.
     * But separate installation with this method has a couple of benefits:<ul>
     * <li>
     * Installation requires a small but noticeable amount of time. If test
     * execution times are examined then isolated installation with this method
     * during static initialization of test-class is recommended.
     * </li>
     * <li>
     * LazyParams can support multi-threaded test executions if the concurrency
     * does not effect the order by which the parameter values are picked.
     * But this only works as long as LazyParams can successfully associate a
     * parameter value pick on a child thread with its main test-execution thread.
     * If this method is used to make sure LazyParams is installed before the
     * main thread starts the test (e.g. during static
     * initialization of test-class) then there is a greater chance to
     * successfully associate the main test-execution thread with parameter
     * values that are picked from child threads.<br>
     * A situation of concern is when test is executed with a timeout that fails
     * the test if it takes too long, because this is usually supported
     * by having the actual test execute on a child thread, while the main
     * test-execution thread sits waiting for the specified amount of time.
     * </li>
     * </ul>
     * @see #uninstall()
     */
    public static void install() {
        currentScopeConfiguration();
    }

    /**
     * Temporarily uninstalls LazyParams but it will be reinstalled again on
     * next usage.
     * This function should be of no concern as long as everything works but
     * there is a risk for the deep installation of LazyParams to end up in
     * technical conflict with some other framework. Invoking this function
     * during initialization of test-class that deals with the conflicting
     * framework could then be a way to workaround the problem.
     *
     * @see #install()
     */
    public static void uninstall() {
        Instrument.uninstall();
    }

    /**
     * Returns current test scope configuration.
     * Typically the configuration gets out of scope when current test
     * (or test-suite) execution is completed.
     * If method is used outside of any detectable test then a thread scoped
     * configuration is retrieved.
     */
    public static Configuration currentScopeConfiguration() {
        return ConfigurationContext.currentTestConfiguration();
    }

    private enum ExtraIdDetail {
        varargs_named, varargs_displayed, array_named, array_displayed;
    }
}
