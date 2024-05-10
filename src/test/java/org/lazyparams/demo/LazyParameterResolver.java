/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.demo;

import java.lang.reflect.Array;
import java.lang.reflect.Parameter;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.lazyparams.LazyParams;

/**
 * {@link GlobalAndLocalLifecycleMethodParameters} provides an example on how
 * a {@link ParameterResolver Jupiter ParameterResolver}, such as this one,
 * can be used to have LazyParams populate all sorts of Jupiter lifecycle
 * methods with values that are pairwise combined.
 *
 * @author Henrik Kaipe
 */
public class LazyParameterResolver implements ParameterResolver {

    Optional<?>  valuesOf( Parameter parameter) {
        return Stream.of(parameter.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(ExtendWith.class))
                .filter(a -> Stream.of(a.annotationType().getAnnotation(ExtendWith.class).value())
                        .anyMatch(Predicate.isEqual(getClass())))
                .map(a -> { try {

                    return a.annotationType().getMethod("value").invoke(a);

                } catch (Exception x) {throw new Error(x);}}).findAny();
    }

    @Override
    public boolean supportsParameter( ParameterContext paramCtx, ExtensionContext extCtx) {
        return valuesOf(paramCtx.getParameter()).isPresent();
    }

    @Override
    public Object resolveParameter( ParameterContext paramCtx, ExtensionContext extCtx) {
        Parameter parameter = paramCtx.getParameter();
        Object rawValue = valuesOf(parameter).get();

        Object[] parameterValues = rawValue.getClass().isArray()
                ? IntStream.range(0, Array.getLength(rawValue))
                        .mapToObj(i -> Array.get(rawValue, i))
                        .toArray()
                : new Object[] {rawValue};

        return LazyParams.pickValue(parameter.getName(), parameterValues);
    }
}
