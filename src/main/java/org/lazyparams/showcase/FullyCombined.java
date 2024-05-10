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
 * @author Henrik Kaipe
 */
public class FullyCombined {
    private FullyCombined() {}

    public static <T> T pickFullyCombined(
            ToDisplayFunction<? super T> toDisplay, T[] possibleParamValues) {
        return ScopedLazyParameter.from(possibleParamValues)
                .withExtraIdDetails(ExtraIdDetail.array_displayed)
                .fullyCombinedGlobally()
                .asParameter(toDisplay).pickValue();
    }

    public static <T> T pickFullyCombined(
            String parameterName, T[] possibleParamValues) {
        return ScopedLazyParameter.from(possibleParamValues)
                .withExtraIdDetails(ExtraIdDetail.array_named)
                .fullyCombinedGlobally()
                .asParameter(parameterName).pickValue();
    }

    public static <T> T pickFullyCombined(
            ToDisplayFunction<? super T> toDisplay, T primaryValue, T... otherValues) {
        return ScopedLazyParameter.from(primaryValue, otherValues)
                .withExtraIdDetails(ExtraIdDetail.varargs_displayed)
                .fullyCombinedGlobally()
                .asParameter(toDisplay).pickValue();
    }

    public static <T> T pickFullyCombined(
            String parameterName, T primaryValue, T... otherValues) {
        return ScopedLazyParameter.from(primaryValue, otherValues)
                .withExtraIdDetails(ExtraIdDetail.varargs_named)
                .fullyCombinedGlobally()
                .asParameter(parameterName).pickValue();
    }

    /**
     * This is the {@link FullyCombined} equivalent of
     * {@link org.lazyparams.LazyParams#pickValue(Enum...) LazyParams.pickValue(E... values)}
     * @see org.lazyparams.LazyParams#pickValue(Enum... values)
     */
    public static <E extends Enum<E>> E pickFullyCombined(E... values) {
        Class<?> enumType = values.getClass().getComponentType();
        if (0 == values.length) {
            if (false == enumType.isEnum()) {
                throw new IllegalArgumentException("Not an enum type: " + enumType);
            }
        }
        return ScopedLazyParameter
                .from(0 == values.length ? (E[])enumType.getEnumConstants() : values)
                .fullyCombinedGlobally()
                .withExtraIdDetails(values.length, ExtraIdDetail.enum_constants)
                .asParameter(new ToDisplayFunction<E>() {
            @Override public CharSequence apply(E value) {
                return String.valueOf(value);
            }
        }).pickValue();
    }

    private enum ExtraIdDetail {
        varargs_named, varargs_displayed, array_named, array_displayed,
        enum_constants;
    }
}
