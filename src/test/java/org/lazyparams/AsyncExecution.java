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

import java.lang.reflect.Method;
import java.time.Duration;

import org.junit.jupiter.api.extension.DynamicTestInvocationContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * @author Henrik Kaipe
 * @see LeafParameterizedJupiterTest#LeafParameterizedJupiterTest(InstallScenario,StaticScopeParam,Object,Object,Object,Object,StaticScopeParam,MaxCountsTweak)
 */
public enum AsyncExecution implements InvocationInterceptor {
    INTERCEPTOR;

    private <T> T proceedOnNewThread(Invocation<T> invocation)
    throws Throwable {
        /*
         * Make sure no implicit install is delegated to child-thread,
         * because it would make parameters on child-thread unknown to
         * main test-execution thread context: */
        LazyParams.install();

        return assertTimeoutPreemptively(
                Duration.ofSeconds(2), invocation::proceed);
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> __, ExtensionContext ___)
    throws Throwable {
        proceedOnNewThread(invocation);
    }

    @Override
    public <T> T interceptTestFactoryMethod(Invocation<T> invocation,
            ReflectiveInvocationContext<Method> __, ExtensionContext ___)
    throws Throwable {
        return proceedOnNewThread(invocation);
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> __, ExtensionContext ___)
    throws Throwable {
        proceedOnNewThread(invocation);
    }

    @Override
    public void interceptDynamicTest(Invocation<Void> invocation,
            DynamicTestInvocationContext __, ExtensionContext ___)
    throws Throwable {
        proceedOnNewThread(invocation);
    }
}
