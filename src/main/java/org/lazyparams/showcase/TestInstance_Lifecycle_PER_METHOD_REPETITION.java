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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.locks.ReentrantLock;

import net.bytebuddy.asm.Advice;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.execution.TestInstancesProvider;
import org.junit.jupiter.engine.extension.ExtensionRegistrar;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.engine.support.hierarchical.ThrowableCollector;

import org.lazyparams.LazyParams;
import org.lazyparams.config.Configuration;
import org.lazyparams.internal.AdviceFor;

/**
 * Experimental Jupiter extension that will attempt to force creation of a new
 * test-instance whenever LazyParams triggers repeated execution of a Jupiter test-method.
 * LazyParams default behavior during Jupiter test-execution is to reuse the
 * test instance when lazy parametrization triggers repeated test-execution.
 * This is because Jupiter's most test-instance generous creation mode is
 * {@link TestInstance.Lifecycle#PER_METHOD}, so that a new test instance is
 * created when execution shifts to next test-method - but not when repeated
 * test-method execution is triggered (by LazyParams). This is sometimes
 * undesirable and not equivalent with the behavior on JUnit versions 3 and 4.
 * The purpose of this experimental annotation is to apply
 * {@link TestInstance.Lifecycle#PER_METHOD} and have it further enhanced by
 * trying to make Jupiter also create a new test-instance for each repeated
 * test-method execution.
 *
 * @author Henrik Kaipe
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(TestInstance_Lifecycle_PER_METHOD_REPETITION.Extension.class)
@Inherited
@Documented
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public @interface TestInstance_Lifecycle_PER_METHOD_REPETITION {
    boolean value() default true;

    class Extension implements BeforeAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) {
            LazyParams.install();
        }
    }

    public class JupiterTweak extends AdviceFor<Object> {
        JupiterTweak() {
            super(JupiterEngineExecutionContext.class);
            for (Method m : JupiterEngineExecutionContext.class.getMethods()) {
                if (TestInstancesProvider.class == m.getReturnType()) {
                    on(m);
                }
            }
        }

        @Advice.OnMethodExit(inline = true)
        @SuppressWarnings("UnusedAssignment"/*because of how Byte Buddy works!*/)
        private static void decorateTestInstancesProvider(
                @Advice.Return(readOnly=false) TestInstancesProvider provider,
                @Advice.This JupiterEngineExecutionContext context) {
            provider = decorate(provider, context);
        }

        public static TestInstancesProvider decorate(
                final TestInstancesProvider provider,
                JupiterEngineExecutionContext context) {
            Class<?> testClass = context.getExtensionContext().getTestClass().orElse(void.class);
            TestInstance_Lifecycle_PER_METHOD_REPETITION lazy = testClass
                    .getAnnotation(TestInstance_Lifecycle_PER_METHOD_REPETITION.class);
            return null != lazy && lazy.value()
                    ? new TestInstancesProviderDecoration(provider)
                    : provider;
        }

        private static Method resolve_target_getTestInstances_method() {
            String methodName = "getTestInstances";
            try {
                return TestInstancesProvider.class.getMethod(methodName,
                        ExtensionRegistry.class, ExtensionRegistrar.class,
                        ThrowableCollector.class);
            } catch (NoSuchMethodException betterBeJupiterVersion_5_6_or_5_5) {
                try {
                    return TestInstancesProvider.class.getMethod(methodName,
                            ExtensionRegistry.class, ExtensionRegistrar.class);
                } catch (NoSuchMethodException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }

        /**
         * The decorator class that implements both editions of
         * {@link TestInstancesProvider}. I.e. it implements the recent edition
         * that was introduced by jupiter engine version 5.7.0 - but also
         * the older edition (from versions 5.5 and 5.6) that doesn't have the
         * ThrowableCollector method argument.
         */
        private static class TestInstancesProviderDecoration implements TestInstancesProvider {
            static final Method proxiedMethod = resolve_target_getTestInstances_method();

            private final TestInstancesProvider coreProvider;

            TestInstancesProviderDecoration(TestInstancesProvider coreProvider) {
                this.coreProvider = coreProvider;
            }

            private TestInstances newProxy(final Object... getTestInstancesArguments) {

                return (TestInstances) Proxy.newProxyInstance(
                        TestInstances.class.getClassLoader(),
                        new Class[] { TestInstances.class},
                        new InvocationHandler() {

                    private final ReentrantLock lockOnScope = new ReentrantLock();

                    private TestInstances resolveLazyTestInstances()
                    throws InvocationTargetException, IllegalAccessException {
                        Configuration currentScope = LazyParams.currentScopeConfiguration();
                        lockOnScope.lock();
                        try {
                            TestInstances coreInstances =
                                    currentScope.getScopedCustomItem(this);
                            if (null == coreInstances) {
                                coreInstances = (TestInstances) proxiedMethod
                                        .invoke(coreProvider, getTestInstancesArguments);
                                currentScope.setScopedCustomItem(this, coreInstances);
                            }
                            return coreInstances;
                        } finally {
                            lockOnScope.unlock();
                        }
                    }

                    @Override
                    public Object invoke(Object _ignore_, Method method, Object[] arguments)
                    throws Throwable {
                        try {
                            return method.invoke(resolveLazyTestInstances(), arguments);
                        } catch (InvocationTargetException ex) {
                            throw ex.getTargetException();
                        }
                    }
                });
            }

            @Override // on JUnit platform version 5.7.0 and later
            public TestInstances getTestInstances(
                    ExtensionRegistry extensionRegistry, ExtensionRegistrar extensionRegistrar,
                    ThrowableCollector throwableCollector) {
                return newProxy(extensionRegistry, extensionRegistrar, throwableCollector);
            };
            //@Override // on JUnit platform version 5.6.x and earlier
            public TestInstances getTestInstances(
                    ExtensionRegistry extensionRegistry, ExtensionRegistrar extensionRegistrar) {
                return newProxy(extensionRegistry, extensionRegistrar);
            }
        }
    }
}
