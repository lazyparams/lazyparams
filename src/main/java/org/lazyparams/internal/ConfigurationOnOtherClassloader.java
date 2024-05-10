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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.lazyparams.config.Configuration;
import org.lazyparams.config.ReadableConfiguration;

/**
 * @author Henrik Kaipe
 */
class ConfigurationOnOtherClassloader extends Configuration {

    private final Object configOnProviderClassLoader;
    private final ReadableConfiguration configReader = (ReadableConfiguration) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class[] {ReadableConfiguration.class},
            new InvocationHandler() {
        @Override
        public Object invoke(Object o, Method method, Object[] arguments)
        throws Throwable {
            return invokeOnProviderClassLoader(
                    method.getName(), method.getParameterTypes(), arguments);
        }
    });
    private final Class targetScopeRetirementPlanClass;
    private final Method targetInternalSetScopedCustomItem;

    ConfigurationOnOtherClassloader(Object targetOnCoreClassLoader)
    throws ClassNotFoundException, NoSuchMethodException {
        this.configOnProviderClassLoader = targetOnCoreClassLoader;
        this.targetScopeRetirementPlanClass = Class.forName(
                ScopeRetirementPlan.class.getName(), true,
                targetOnCoreClassLoader.getClass().getClassLoader());
        this.targetInternalSetScopedCustomItem = Class.forName(
                ConfigurationImpl.class.getName(), true,
                targetOnCoreClassLoader.getClass().getClassLoader())
                .getDeclaredMethod("internalSetScopedCustomItem",
                        Object.class,Object.class,targetScopeRetirementPlanClass);
        this.targetInternalSetScopedCustomItem.setAccessible(true);
    }

    private Method accessableMethod(
            Class cls, String methodName, Class[] parameterTypes)
    throws NoSuchMethodException {
        Method m = cls.getMethod(methodName, parameterTypes);
        m.setAccessible(true);
        return m;
    }

    private <E extends Throwable> Object invoke(
            String methodName, Class[] methodParams, Object[] arguments)
    throws E {
        try {
            return accessableMethod(
                    configOnProviderClassLoader.getClass(), methodName, methodParams)
                    .invoke(configOnProviderClassLoader, arguments);
        } catch (InvocationTargetException ex) {
            throw (E) ex.getTargetException();
        } catch (Exception ex) {
            throw (E) ex;
        }
    }

    private <T> T invokeOnProviderClassLoader(String methodName,
            Class[] parameterTypes, Object... arguments) {
        return (T) this.<RuntimeException>invoke(methodName, parameterTypes, arguments);
    }

    @Override
    protected ReadableConfiguration parentConfiguration() {
        return configReader;
    }

    private Object convertRetirementPlan(final ScopeRetirementPlan<?> retirementPlan) {
        return Proxy.newProxyInstance(
                new ClassLoader(targetScopeRetirementPlanClass.getClassLoader()) {
                    /* ... to keep class away from core class-loader
                     * for a better chance to have it garbage collected.*/
                },
                new Class[] {targetScopeRetirementPlanClass},
                new InvocationHandler() {
            @Override
            public Object invoke(Object o, Method method, Object[] arguments)
            throws Throwable {
                try {
                    return accessableMethod(ScopeRetirementPlan.class,
                            method.getName(), method.getParameterTypes())
                            .invoke(retirementPlan, arguments);
                } catch (InvocationTargetException ex) {
                    throw ex.getTargetException();
                }
            }
        });
    }

    @Override
    protected <V> void internalSetScopedCustomItem(
            Object scopedItemKey, V scopedItemValue, ScopeRetirementPlan<? super V> onScopeRetirement) {
        try {
            targetInternalSetScopedCustomItem.invoke(configOnProviderClassLoader,
                    scopedItemKey, scopedItemValue, convertRetirementPlan(onScopeRetirement));
        } catch (InvocationTargetException ex) {
            throw new Error(ex.getTargetException());
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    @Override
    public <V> V getScopedCustomItem(Object scopedItemKey) {
        return invokeOnProviderClassLoader(
                "getScopedCustomItem", new Class[] {Object.class}, scopedItemKey);
    }

    @Override
    public void setAlsoUseValueDisplaySeparatorBeforeToDisplayFunction(
            Boolean alsoUseValueDisplaySeparatorBeforeToDisplayFunction) {
        invokeOnProviderClassLoader(
                "setAlsoUseValueDisplaySeparatorBeforeToDisplayFunction",
                new Class[] {Boolean.class},
                alsoUseValueDisplaySeparatorBeforeToDisplayFunction);
    }

    @Override
    public void setValueDisplaySeparator(String valueDisplaySeparator) {
        invokeOnProviderClassLoader("setValueDisplaySeparator",
                new Class[] {String.class}, valueDisplaySeparator);
    }

    @Override
    public void setMaxTotalCount(int maxTotalCountOrZeroToForceParentScope) {
        invokeOnProviderClassLoader("setMaxTotalCount",
                new Class[] {int.class}, maxTotalCountOrZeroToForceParentScope);
    }

    @Override
    public void setMaxFailureCount(int maxFailureCountOrZeroToForceParentScope) {
        invokeOnProviderClassLoader("setMaxFailureCount",
                new Class[] {int.class}, maxFailureCountOrZeroToForceParentScope);
    }
}
