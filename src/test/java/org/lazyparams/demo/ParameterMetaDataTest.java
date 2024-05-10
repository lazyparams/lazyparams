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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lazyparams.LazyParams;
import org.lazyparams.LazyParamsCoreUtil;
import org.lazyparams.ToDisplayFunction;
import org.lazyparams.showcase.ScopedLazyParameter;
import org.lazyparams.showcase.ToPick;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Henrik Kaipe
 */
public class ParameterMetaDataTest {

    private static final List<Class<?>> mainClassesToVerify = readonlyListOf(
            LazyParams.class, LazyParamsCoreUtil.class,
            ToDisplayFunction.class, ScopedLazyParameter.class, ToPick.class);
    private static final List<Class<?>> testClassesToVerify = readonlyListOf(
            GlobalAndLocalLifecycleMethodParameters.class, Tpi.class,
            ParameterMetaDataTest.class);

    /**
     * Lazy parameter!
     * It is here fully defined but it is only introduced when
     * {@link ScopedLazyParameter#pickValue()} is assigned to
     * field {@link #methodOnTest} during class-instance initialization.
     * The parameter is kept here in static context because the methods are not
     * consistently sorted. Therefore stability is preserved by making sure the
     * parameter definition is not reproduced from scratch on every repetition
     * and is instead kept here with all values on the original order from the
     * first and only creation of the parameter.
     */
    static ScopedLazyParameter<Method> methodsToTest;

    final Method methodOnTest = methodsToTest.pickValue();
    final boolean expectParameterNameMetadata = testClassesToVerify
            .contains(methodOnTest.getDeclaringClass());

    @Test void verify() {
        Parameter methodParam = LazyParams.pickValue(p -> (expectParameterNameMetadata
                ? "is test-class method that should have name metadata present"
                : "is main-class method that must not have name metadata"
                ) + " on parameter " + p.getType().getSimpleName() + " " + p.getName(),
                methodOnTest.getParameters());
        assertEquals(expectParameterNameMetadata, methodParam.isNamePresent());
    }

    @BeforeAll static void setupMethodsParameter() {
        methodsToTest = Stream.concat(testClassesToVerify.stream(), mainClassesToVerify.stream())
                .flatMap(c -> Stream.of(c.getDeclaredMethods()))
                .filter(m -> false == m.isSynthetic() && 1 <= m.getParameterCount())
                .collect(ToPick.from())
                .asParameter(m -> m.getDeclaringClass().getSimpleName() + "::" + m.getName());
    }
    @AfterAll static void cleanup() {
        methodsToTest = null;
    }

    private static List<Class<?>> readonlyListOf(Class<?>... classes) {
        return Collections.unmodifiableList(Arrays.asList(classes));
    }
}
