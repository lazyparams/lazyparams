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
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.BindingPriority;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * AOP utility for specifying advice and its target classes.
 *
 * @author Henrik Kaipe
 */
public abstract class AdviceFor<T> {

    private final Class<? extends T> classToAdvice;
    private T methodInterceptor;
    private Junction<MethodDescription> methods2advice;

    AdviceFor() {
        this(null);
    }

    protected AdviceFor(Class<? extends T> classToRedefine) {
        Class<T> tClass = resolveClassToRedefineFromGenericTypeParameter();
        this.classToAdvice = null != classToRedefine ? classToRedefine : tClass;

        if (null == tClass) {
            tClass = (Class<T>) this.classToAdvice;
        }
        try {
            this.methodInterceptor = Object.class == tClass ? (T)"" : new ByteBuddy()
                    .subclass(tClass,ConstructorStrategy.Default.IMITATE_SUPER_CLASS)
                    .method(isMethod()
                            .and(not(isDeclaredBy(Object.class))))
                    .intercept(MethodDelegation.to(this, AdviceFor.class))
                    .make()
                    .load(tClass.isInterface()
                            ? new ClassLoader(getClass().getClassLoader()) {}
                            : getClass().getClassLoader(),
                            ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded()
                    .newInstance();
        } catch (Exception propablyAConstructorIssue_tryInsteadToOnlySupportInterfaces) {
            this.methodInterceptor = (T) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    collectInterfacesOf(this.classToAdvice).toArray(new Class[1]),
                    new InvocationHandler() {
                @Override
                public Object invoke(Object o, Method method, Object[] os) {
                    return on(method);
                }
            });
        }
    }

    AgentBuilder append(AgentBuilder builder) {
        final Class<?> adviceClass = getClass();
        return builder.type(definitionsToInstrument()).transform(new AgentBuilder.Transformer() {
            /*@Override on ByteBuddy versions until 1.12.12 */
            public DynamicType.Builder<?> transform(
                    DynamicType.Builder<?> builder,
                    TypeDescription typeDescription,
                    ClassLoader classLoader,
                    JavaModule module) {
                return builder.visit(Advice.to(adviceClass).on(methods2advice));
            }
            /*@Override on ByteBuddy version 1.12.13 and later!*/
            public DynamicType.Builder<?> transform(
                    DynamicType.Builder<?> builder,
                    TypeDescription typeDescription,
                    ClassLoader classLoader,
                    JavaModule module,
                    ProtectionDomain protectionDomainIsIgnored) {
                try {
                    return transform(builder, typeDescription, classLoader, module);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new Error(ex);
                }
            }
        });
    }

    protected ElementMatcher.Junction<? super TypeDescription> definitionsToInstrument() {
        if (classToAdvice.isInterface()) {
            return ElementMatchers.not(ElementMatchers.isInterface())
                    .and(ElementMatchers.isSubTypeOf(classToAdvice))
                    .and(ElementMatchers.declaresMethod(methods2advice));

        } else if (Object.class == classToAdvice) {
            /* Special case that implies this method must be overridden: */
            try {
                StackTraceElement[] trace = Thread.currentThread().getStackTrace();
                assert trace[0].getMethodName().equals(trace[1].getMethodName())
                        || trace[1].getMethodName().equals(trace[2].getMethodName());
            } catch (RuntimeException ignoreCheck) {
            } catch (AssertionError badState) {
                System.err.println(getClass() + " has java.lang.Object as "
                        + "#classToRedefine and therefore really should"
                        + " override this method!");
            }
            /* Specify a non-interface that declares either of #methods2advice
             * and expect actual class implementation to append with
             * additional conditions on which class to be transformed:*/
            return ElementMatchers.not(ElementMatchers.isInterface())
                    .and(ElementMatchers.declaresMethod(methods2advice));
        } else {
            return ElementMatchers.is(classToAdvice);
        }
    }

    private static Collection<Class> collectInterfacesOf(Class<?> classToRedefine) {
        if (Object.class == classToRedefine) {
            return new HashSet<Class>();
        } else {
            Collection<Class> interfaces = collectInterfacesOf(
                    classToRedefine.getSuperclass());
            Collections.addAll(interfaces, classToRedefine.getInterfaces());
            return interfaces;
        }
    }

    private Class<T> resolveClassToRedefineFromGenericTypeParameter() {
        Type superClass = getClass().getGenericSuperclass();
        return superClass instanceof ParameterizedType
                ? (Class<T>) ((ParameterizedType)superClass).getActualTypeArguments()[0]
                : null;
    }

    protected Method[] getDeclaredMethods() {
        return classToAdvice.getDeclaredMethods();
    }

    @BindingPriority(Integer.MAX_VALUE)
    @RuntimeType
    public final Object on(@Origin Method m) {
//        System.out.println("Intercepted: " + m);
        if (null == methods2advice) {
            methods2advice = none();
        }
        Junction<MethodDescription> intercepted = hasMethodName(m.getName())
                .and(not(isAbstract()))
                .and(takesArguments(m.getParameterTypes()))
                .and(returns(isSubTypeOf(m.getReturnType())));
        methods2advice = methods2advice.or(intercepted);
        if (hasGenericArguments(m)) {
            Class[] paramTypes = m.getParameterTypes();
            intercepted = isOverriddenFrom(m.getDeclaringClass())
                    .and(takesArguments(paramTypes.length))
                    .and(hasMethodName(m.getName()))
                    .and(not(isAbstract()))
                    .and(returns(isSubTypeOf(m.getReturnType())));
            for (int i = 0; i < paramTypes.length; ++i) {
                intercepted = intercepted
                        .and(takesArgument(i, isSubTypeOf(paramTypes[i])));
            }
            methods2advice = methods2advice.or(intercepted);
        }
        return null;
    }

    private boolean hasGenericArguments(Method m) {
        for (Type argType : m.getGenericParameterTypes()) {
            if (Class.class != argType.getClass()) {
                return true;
            }
        }
        return false;
    }

    protected final T on() {
        return methodInterceptor;
    }
}
