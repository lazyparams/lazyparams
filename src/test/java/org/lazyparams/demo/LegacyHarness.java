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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.lazyparams.LazyParams;
import org.lazyparams.showcase.TestInstance_Lifecycle_PER_METHOD_REPETITION;
import org.lazyparams.showcase.ToPick;

/**
 * A Jupiter equivalent of JUnit-3 test super-class {@link junit.framework.TestCase}
 * but it provides a twist by using an extra special parameter that carries the
 * test methods as its values.
 * I.e. also the test-methods are pairwise combined with their parameters, which
 * are still pairwise combined among themselves but across all test-methods.
 * <br/>
 * On top of being a demo - there is here an intention to verify LazyParams
 * compatibility with Jupiter parameter resolution and Jupiter annotation
 * {@link DisplayName @DisplayName}. Also it provides some coverage on
 * feature {@link ToPick#as(org.lazyparams.ToDisplayFunction), which is
 * deprecated but still convenient if you know what you're doing and make sure
 * to have your stream consistently sorted before picking the parameter value.
 *
 * @author Henrik Kaipe
 */
@TestInstance_Lifecycle_PER_METHOD_REPETITION
abstract class LegacyHarness {

    @RegisterExtension
    static final ParameterResolver testMethodResolver = new ParameterResolver() {
        @Override
        public boolean supportsParameter(
                ParameterContext parameterContext, ExtensionContext extensionContext) {
            return parameterContext.getParameter().getType() == TestMethod.class;
        }
        @Override
        public Object resolveParameter(
                ParameterContext parameterContext, ExtensionContext extensionContext) {
            boolean useSeparatorBeforeToDisplay = LazyParams.currentScopeConfiguration()
                    .alsoUseValueDisplaySeparatorBeforeToDisplayFunction();
            try {
                if (useSeparatorBeforeToDisplay) {
                    LazyParams.currentScopeConfiguration()
                            .setAlsoUseValueDisplaySeparatorBeforeToDisplayFunction(false);
                }
                return new TestMethod(Stream
                        .of(extensionContext.getRequiredTestClass().getMethods())
                        .filter(m -> 0 == m.getParameterCount())
                        .filter(m -> m.getName().startsWith("test"))
                        .sorted(Comparator.comparing(Method::getName))
                        .collect(ToPick.as(m -> m.getName().substring("test".length()))));
            } finally {
                if (useSeparatorBeforeToDisplay) {
                    LazyParams.currentScopeConfiguration()
                            .setAlsoUseValueDisplaySeparatorBeforeToDisplayFunction(true);
                }
            }
        }
    };

    static class TestMethod {
        private final Method testMethod;
        TestMethod(Method testMethod) { this.testMethod = testMethod; }

        void invokeOn(LegacyHarness testInstance) throws Throwable {
            try {
                testMethod.invoke(testInstance);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        };
    }

    @Test @DisplayName("test")
    void test(TestMethod legacyTestMethod) throws Throwable {
        try {
            setUp();
            legacyTestMethod.invokeOn(this);
        } finally {
            tearDown();
        }
    }

    protected void setUp() throws Exception {}
    protected void tearDown() throws Exception {}
}
