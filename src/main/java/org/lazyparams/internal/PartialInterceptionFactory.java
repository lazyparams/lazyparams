/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.function.Function;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.BindingPriority;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * @author Henrik Kaipe
 */
class PartialInterceptionFactory<T> {

    private final Constructor<? extends T> constructor;
    private final Function<T,Object> implementation4abstracts;

    PartialInterceptionFactory(
            Class<T> abstractClass,
            Function<T,Object> implementation4abstracts,
            Class<?>... constructorParameterTypes) {
        try {
            this.constructor = new ByteBuddy()
                    .subclass(abstractClass)
                    .method(ElementMatchers.not(
                            ElementMatchers.isDeclaredBy(abstractClass)))
                    .intercept(MethodDelegation.to(this))
                    .make()
                    .load(abstractClass.getClassLoader(),
                            ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded()
                    .getConstructor(constructorParameterTypes);
        } catch (NoSuchMethodException ex) {
            throw new Error(ex);
        }
        this.constructor.setAccessible(true);
        this.implementation4abstracts = implementation4abstracts;
    }

    T newInstance(Object... constructorArgs) {
        try {
            return constructor.newInstance(constructorArgs);
        } catch (Exception mustNotHappen) {
            throw new Error(mustNotHappen);
        }
    }

    @BindingPriority(Integer.MAX_VALUE)
    @RuntimeType
    final Object interceptAbstractMethod(
            @This                T instance,
            @Origin         Method method,
            @AllArguments Object[] arguments) {
        method.setAccessible(true);
        return this.<RuntimeException>interceptAbstractMethodInternal(
                    implementation4abstracts.apply(instance), method, arguments);
    }

    protected <E extends Throwable> Object interceptAbstractMethodInternal(
            Object target, Method method, Object[] arguments)
    throws E {
        try {
            return method.invoke(target, arguments);
        } catch (VirtualMachineError ex) {
            throw ex;
        } catch (Throwable ex) {
            if (ex instanceof InvocationTargetException) {
                ex = ((InvocationTargetException)ex).getTargetException();
            }
            if (ex instanceof UndeclaredThrowableException) {
                ex = ((UndeclaredThrowableException)ex).getUndeclaredThrowable();
            }
            throw (E) ex;
        }
    }
}
