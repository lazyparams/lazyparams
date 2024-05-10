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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author Henrik Kaipe
 */
abstract class Handler implements InvocationHandler {

    private void assertInterface(Class<?> actualType) {
        assert actualType.isInterface()
                : "Interface-type required but was " + actualType;
    }

    @Override
    public final Object invoke(Object o, Method method, Object[] args)
    throws Throwable {
        if ("toString".equals(method.getName()) && 0 == method.getParameterTypes().length) {
            return toString();
        }
        assertInterface(method.getDeclaringClass());
        try {
            return invoke(method.getName(), args);
        } catch (InvocationTargetException targetEx) {
            throw targetEx.getTargetException();
        }
    }

    abstract Object invoke(String name, Object[] args) throws Throwable;

    <T> T asHandlerFor(Class<T> interfaceType, Class<?>... extraInterfaces) {
        Class[] allInterfaces = new Class[extraInterfaces.length + 1];
        assertInterface(allInterfaces[0] = interfaceType);
        for (int i = 0; i < extraInterfaces.length; ++i) {
            assertInterface(allInterfaces[i+1] = extraInterfaces[i]);
        }
        return interfaceType.cast(Proxy.newProxyInstance(
                getClass().getClassLoader(), allInterfaces, this));
    }
}
