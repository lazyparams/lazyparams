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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.DynamicTestInvocationContext;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.lazyparams.core.Lazer;
import org.lazyparams.internal.LazerContext;

/**
 * @author Henrik Kaipe
 * @see LeafParameterizedJupiter#mutableExtensions
 */
public class CompositeExtensions
implements AfterAllCallback, BeforeAllCallback, BeforeEachCallback,
        TestExecutionExceptionHandler, AfterEachCallback, InvocationInterceptor {

    private final List<Extension> extensions = new ArrayList<>();

    private <E extends Extension> Stream<E> extStreamOf(Class<E> extType) {
        return extensions.stream().filter(extType::isInstance).map(extType::cast);
    }

    private <E extends Extension> void extractAndApply(Class<E> extType,
            ExtenstionContextOp<E> operation, ExtensionContext extContext)
    throws Exception {
        Exception toThrow = extStreamOf(extType)
                .map(operation.toExceptionFor(extContext))
                .filter(Exception.class::isInstance)
                .findFirst().orElse(null);
        if (null != toThrow) {
            throw toThrow;
        }
    }

    public void add(Extension x) {
        extensions.add(x);
    }

    public void clear() {
        extensions.clear();
    }

    @Override
    public void afterAll(ExtensionContext ec) throws Exception {
        try {
            extractAndApply(AfterAllCallback.class,
                    AfterAllCallback::afterAll, ec);
        } finally {
            try {
                if (false == LazerContext.resolveLazer().pendingCombinations()) {                
                    extensions.clear();
                }
            } catch (Lazer.ExpectedParameterRepetition ex) {
                throw new Error(ex);
            }
        }
    }

    @Override
    public void beforeAll(ExtensionContext ec) throws Exception {
        extractAndApply(BeforeAllCallback.class,
                BeforeAllCallback::beforeAll, ec);
    }

    @Override
    public void beforeEach(ExtensionContext ec) throws Exception {
        extractAndApply(BeforeEachCallback.class,
                BeforeEachCallback::beforeEach, ec);
    }

    @Override
    public void afterEach(ExtensionContext ec) throws Exception {
        extractAndApply(AfterEachCallback.class,
                AfterEachCallback::afterEach, ec);
    }

    @Override
    public void handleTestExecutionException(ExtensionContext ec, Throwable thrwbl)
    throws Throwable {
        extStreamOf(TestExecutionExceptionHandler.class).reduce(
                (handler1,handler2) -> (ctx,thrown) -> {
            try {
                handler1.handleTestExecutionException(ctx, thrown);
            } catch (Throwable rethrown) {
                handler2.handleTestExecutionException(ctx, rethrown);
            }
        }).orElse((ctx,thrown) -> { throw thrown; })
                .handleTestExecutionException(ec, thrwbl);
    }

    @FunctionalInterface
    interface ExtenstionContextOp<E extends Extension> {
        void apply(E extension, ExtensionContext ec) throws Exception;

        default Function<E,Exception> toExceptionFor(ExtensionContext ec) {
            return extension -> {
                try {
                    apply(extension, ec);
                    return null;
                } catch (Exception ex) {
                    return ex;
                }
            };
        }
    }

    private void proceed(Invocation<Void> invocation,
            Function<InvocationInterceptor,InvocationConsumer> interceptorOp)
    throws Throwable {
        extStreamOf(InvocationInterceptor.class).map(interceptorOp)
                .reduce((ii1,ii2) -> inv -> ii1.accept(() -> ii2.apply(inv)))
                .orElse(Invocation::proceed)
                .accept(invocation);
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext)
    throws Throwable {
        proceed(invocation, ii -> inv -> ii.interceptTestMethod(
                inv, invocationContext, extensionContext));
    }
    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext)
    throws Throwable {
        proceed(invocation, ii -> inv -> ii.interceptTestTemplateMethod(
                inv, invocationContext, extensionContext));
    }
    @Override
    public void interceptDynamicTest(Invocation<Void> invocation,
            DynamicTestInvocationContext invocationContext,
            ExtensionContext extensionContext)
    throws Throwable {
        proceed(invocation, ii -> inv -> ii.interceptDynamicTest(
                inv, invocationContext, extensionContext));
    }
    @Override
    public <T> T interceptTestFactoryMethod(Invocation<T> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext)
    throws Throwable {
        return extStreamOf(InvocationInterceptor.class)
                .<InvocationFunction<T>>map(ext -> inv -> ext.interceptTestFactoryMethod(
                        inv, invocationContext, extensionContext))
                .reduce((f1,f2) -> inv -> f1.apply(() -> f2.apply(inv)))
                .orElse(Invocation::proceed).apply(invocation);
    }

    @FunctionalInterface
    interface InvocationConsumer {
        void accept(Invocation<Void> inv) throws Throwable;
        default Void apply(Invocation<Void> inv) throws Throwable {
            accept(inv);
            return null;
        }
    }
    @FunctionalInterface
    interface InvocationFunction<T> {
        T apply(Invocation<T> inv) throws Throwable;
    }
}
