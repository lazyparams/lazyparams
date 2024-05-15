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

import org.lazyparams.ToDisplayFunction;

/**
 * pickValue-methods which parametrization functionality match their signature
 * sibling methods of {@link org.lazyparams.LazyParams} - but the parameter
 * value-pick is not appended to test-name if test passes.
 * I.e. the picked value is made part of test-name only if the test fails. This
 * can be very useful for tests with many parameters!
 * <br><br>
 * This strategy for hiding parameter for successful test is achieved by making
 * use of {@link ScopedLazyParameter.Silencer#qronicly()} when creating a
 * parameter.
 *
 * @see org.lazyparams.LazyParams
 * @see ScopedLazyParameter.Silencer#qronicly()
 *
 * @author Henrik Kaipe
 */
public class Qronicly {
    private Qronicly() {}

    /**
     * Used by {@link #pickValue(Enum...) pickValue(E... values)}
     */
    private static final ToDisplayFunction<Object> stringValueOf =
            new ToDisplayFunction<Object>() {
        @Override public CharSequence apply(Object value) {
            return String.valueOf(value);
        }
    };

    /**
     * @see org.lazyparams.LazyParams#pickValue(ToDisplayFunction,Object[])
     * @see ScopedLazyParameter.Silencer#qronicly()
     */
    public static <T> T pickValue(
            ToDisplayFunction<? super T> toDisplay,
            T[] possibleParameterValues) {
        return ScopedLazyParameter
                .from(possibleParameterValues)
                .withExtraIdDetails(ExtraIdDetail.array_displayed)
                .qronicly()
                .asParameter(toDisplay).pickValue();
    }

    /**
     * @see org.lazyparams.LazyParams#pickValue(String,Object[])
     * @see ScopedLazyParameter.Silencer#qronicly()
     */
    public static <T> T pickValue(
            final String parameterName, T[] possibleParamValues) {
        return ScopedLazyParameter
                .from(possibleParamValues)
                .withExtraIdDetails(ExtraIdDetail.array_named)
                .qronicly()
                .asParameter(parameterName).pickValue();
    }

    /**
     * @see org.lazyparams.LazyParams#pickValue(ToDisplayFunction,Object,Object...) 
     * @see ScopedLazyParameter.Silencer#qronicly()
     */
    public static <T> T pickValue(
            ToDisplayFunction<? super T> toDisplay,
            T primaryValue, T... otherValues) {
        return ScopedLazyParameter
                .from(primaryValue, otherValues)
                .withExtraIdDetails(ExtraIdDetail.varargs_displayed)
                .qronicly()
                .asParameter(toDisplay).pickValue();
    }

    /**
     * @see org.lazyparams.LazyParams#pickValue(String,Object,Object...)
     * @see ScopedLazyParameter.Silencer#qronicly()
     */
    public static <T> T pickValue(
            String parameterName, T primaryValue, T... otherValues) {
        return ScopedLazyParameter
                .from(primaryValue, otherValues)
                .withExtraIdDetails(ExtraIdDetail.varargs_named)
                .qronicly()
                .asParameter(parameterName).pickValue();
    }

    /**
     * Special function to promote lazy enum parameters with minimal boiler plating.
     * To pick a parameter value from the constants of enum type <code>MyEnum</code>
     * it is sufficient to use statements like these:<pre><code>
     *   Qronicly.&lt;MyEnum&gt;pickValue()
     *   MyEnum value = Qronicly.pickValue()
     * </code></pre>
     * Parameter value toString() result will be displayed. (Default
     * {@link Enum#toString()} implementation returns constant name.)
     *
     * @see org.lazyparams.LazyParams#pickValue(java.lang.Enum...)
     * @see ScopedLazyParameter.Silencer#qronicly()
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
                .qronicly()
                .asParameter(stringValueOf).pickValue();
    }

    private enum ExtraIdDetail {
        varargs_named, varargs_displayed, array_named, array_displayed;
    }
}
