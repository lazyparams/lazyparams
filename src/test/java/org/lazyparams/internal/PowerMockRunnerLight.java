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

import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import javassist.util.proxy.DefineClassHelper;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import org.powermock.core.MockGateway;
import org.powermock.core.classloader.MockClassLoader;
import org.powermock.core.testlisteners.GlobalNotificationBuildSupport;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.powermock.reflect.internal.WhiteboxImpl;

/**
 * Quick-and-dirty {@link PowerMockRunner} extension that applies a few fixes,
 * which allow some of this framework's regression tests to execute on recent
 * JVMs (versions 17 and 21). Official PowerMock release (e.g. version 2.0.9)
 * breaks modularity in ways that are not accepted by recent JVMs (e.g. version
 * 17 and later). This hack works around some of these issues by disabling a few
 * PowerMock features. Therewith it's possible to use PowerMock on newer JVMs.
 * <br>
 * Of course, it breaks some PowerMock functionality but this breakage concerns
 * corner-case functionality that is not used very often (e.g. exception
 * stubbing and whiteboxing internal functionality of module "java.base").
 * <br>
 * Functionality on this runner is only tested for the PowerMock features that
 * are used by the regression tests of this framework. E.g. test-executions
 * on separate class-loaders and static mocking with the EasyMock API.
 *
 * @author Henrik Kaipe
 */
public class PowerMockRunnerLight extends PowerMockRunner {

    public PowerMockRunnerLight(Class<?> klass) throws Exception {
        super(makeSureNecessaryFixesAreInstalled(klass));
    }

    private static Class<?> makeSureNecessaryFixesAreInstalled(Class<?> klass)
    throws ClassNotFoundException, UnmodifiableClassException {
        if (klass.isInterface() || klass.isPrimitive() || klass.isArray()
                ||/*to be a little future proof:*/false == Object.class.isAssignableFrom(klass)) {
            throw new IllegalArgumentException("Not a valid test-class: " + klass);
        }

        try {
            GlobalNotificationBuildSupport.Callback dummyCallback =
                    WorkaroundFailureOnGlobalNotificationBuildSupport.dummyCallback();
            try {
                String testClassName = "test.foo.bar";
                GlobalNotificationBuildSupport.prepareTestSuite(testClassName, dummyCallback);
                GlobalNotificationBuildSupport.prepareTestSuite(testClassName, dummyCallback);
            } catch (IllegalStateException needsFix) {
                WorkaroundFailureOnGlobalNotificationBuildSupport.install();
            } finally {
                GlobalNotificationBuildSupport.closePendingTestSuites(dummyCallback);
            }
        } catch (Throwable ignoreThisFunctionality) {}

        try {
            WhiteboxImpl.getAllMethods(klass);
        } catch (Exception needsFix) {
            WhiteboxFix.install();
            WorkaroundFailureOnMockGateway.install();
        }

        try {
            MockClassLoader fakeLoader = new MockClassLoader(new String[0], new String[0]) {
                @Override
                public Class<?> defineClass(String name, ProtectionDomain protectionDomain, byte[] clazz) {
                    return getClass();
                }
                @Override
                protected byte[] defineAndTransformClass(String name, ProtectionDomain protectionDomain) throws ClassNotFoundException {
                    return new byte[0];
                }
            };
            DefineClassHelper.class.getName();
            try {
                DefineClassHelper.toClass("foo", null, fakeLoader, null, new byte[3]);
            } catch (Exception needsFix) {
                DefineClassHelperFix.install();
            }
        } catch (Throwable ignoreThisFunctionality) {}

        return klass;
    }

    private static void installAgent(String startOfClassName, AdviceFor<?>... allAdvice) {
        AgentBuilder builder = new AgentBuilder.Default()
                .disableClassFormatChanges()
                .ignore(ElementMatchers.not(ElementMatchers.nameStartsWith(startOfClassName)));
        for (AdviceFor<?> eachAdvice : allAdvice) {
            builder = eachAdvice.append(builder);
        }
        builder.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.SinglePass.INSTANCE)
                .installOn(Instrument.instrumentation);
    }


    /********************************************************************
     * @see GlobalNotificationBuildSupport#prepareTestSuite(
     *              String, GlobalNotificationBuildSupport.Callback)
     */
    private static class WorkaroundFailureOnGlobalNotificationBuildSupport
    extends AdviceFor<GlobalNotificationBuildSupport> {
        private WorkaroundFailureOnGlobalNotificationBuildSupport()
        throws NoSuchMethodException {
            on(GlobalNotificationBuildSupport.class.getMethod("prepareTestSuite",
                    String.class, GlobalNotificationBuildSupport.Callback.class));
        }

        @Advice.OnMethodExit(onThrowable = IllegalStateException.class)
        @SuppressWarnings({"UnusedAssignment","unused"})
        private static void suppressIllegalStateException(
                @Advice.Thrown(readOnly = false) IllegalStateException issue) {
            issue = null;
        }

        static void install() throws NoSuchMethodException {
            logInstall();
            installAgent(GlobalNotificationBuildSupport.class.getName(),
                    new WorkaroundFailureOnGlobalNotificationBuildSupport());
        }

        static GlobalNotificationBuildSupport.Callback dummyCallback() {
            return (GlobalNotificationBuildSupport.Callback) Proxy.newProxyInstance(
                    WorkaroundFailureOnGlobalNotificationBuildSupport.class.getClassLoader(),
                    new Class[] {GlobalNotificationBuildSupport.Callback.class},
                    new InvocationHandler() {
                @Override
                public Object invoke(Object o, Method method, Object[] os) {
                    return null;
                }
            });
        }
    }


    /********************************************************************
     * @see MockGateway#methodCall(Object,String,Object[],Class[],String)
     */
    private static class WorkaroundFailureOnMockGateway extends AdviceFor<MockGateway> {
        private WorkaroundFailureOnMockGateway() {
            for (Method m : getDeclaredMethods()) {
                if (Modifier.isPublic(m.getModifiers())
                        && Object.class == m.getReturnType()) {
                    Class[] paramTypes = m.getParameterTypes();
                    if (3 <= paramTypes.length
                            && Object.class == paramTypes[0]
                            && String.class == paramTypes[1]) {
                        on(m);
                    }
                }
            }
        }

        @Advice.OnMethodExit(onThrowable = Exception.class)
        @SuppressWarnings({"UnusedAssignment","unused"})
        private static void proceedOnFailure(
                @Advice.Return(readOnly = false) Object returnValue,
                @Advice.Thrown(readOnly = false) Exception thrown) {
            if (null != thrown) {
                thrown = null;
                returnValue = MockGateway.PROCEED;
            }
        }

        static void install() {
            logInstall();
            installAgent(MockGateway.class.getName(),
                    new WorkaroundFailureOnMockGateway());
        }
    }


    /********************************************************************
     * @see WhiteboxImpl
     */
    private static class WhiteboxFix extends AdviceFor<Object> {
        private WhiteboxFix(Class<?> classToRedefine) {
            super(classToRedefine);
            for (Method m : getDeclaredMethods()) {
                if (Method[].class == m.getReturnType()) {
                    on(m);
                }
            }
        }

        @Advice.OnMethodExit
        @SuppressWarnings({"UnusedAssignment","unused"})
        private static void filterJdkNonPublicMethods(
                @Advice.Return(readOnly = false) Method[] declaredMethods) {
            List<Method> open4access = new ArrayList<Method>();
            for (Method m : declaredMethods) {
                Class<?> declaringClass = m.getDeclaringClass();
                if (Modifier.isPublic(m.getModifiers()) && Modifier.isPublic(declaringClass.getModifiers())
                        || false == declaringClass.getName().startsWith("java.")
                        && false == declaringClass.getName().startsWith("javax.")) {
                    open4access.add(m);
                }
            }
            if (open4access.size() < declaredMethods.length) {
                declaredMethods = open4access.toArray(new Method[0]);
            }
        }

        private static Class<?> resolveAnonymousClassToInstrument()
        throws ClassNotFoundException {
            for (int i = 1; true/*will loop until it throws exception or returns*/; ++i) {
                Class<?> candidate = Whitebox.getAnonymousInnerClassType(WhiteboxImpl.class, i);
                if (PrivilegedAction.class.isAssignableFrom(candidate)) {
                    for (Method declaredMethod : candidate.getDeclaredMethods()) {
                        if (0 == declaredMethod.getParameterCount()
                                && Method[].class == declaredMethod.getReturnType()
                                && "run".equals(declaredMethod.getName())) {
                            return candidate;
                        }
                    }
                }
            }
        }

        static void install() throws ClassNotFoundException {
            logInstall();
            installAgent(WhiteboxImpl.class.getName(),
                    new WhiteboxFix(WhiteboxImpl.class),
                    new WhiteboxFix(resolveAnonymousClassToInstrument()));
        }
    }


    /********************************************************************
     * @see DefineClassHelper#toClass(String,Class,ClassLoader,ProtectionDomain,byte[])
     */
    private static class DefineClassHelperFix extends AdviceFor<Object> {
        private DefineClassHelperFix(Class<?> classToRedefine) {
            super(classToRedefine);
            for (Method m : getDeclaredMethods()) {
                if (Class.class == m.getReturnType()) {
                    on(m);
                }
            }
        }

        @Advice.OnMethodExit(onThrowable = Exception.class)
        @SuppressWarnings({"UnusedAssignment","unused"})
        private static void retryOnMockClassLoader(
                @Advice.AllArguments Object[] args,
                @Advice.Return(readOnly = false) Class<?> newClass,
                @Advice.Thrown(readOnly = false) Exception thrown) {
            if (null != thrown && null != args && 4 < args.length
                    && args[0] instanceof String
                    && args[2] instanceof MockClassLoader
                    && args[4] instanceof byte[]) {
                newClass = ((MockClassLoader)args[2]).defineClass(
                        (String)args[0], (ProtectionDomain) args[3], (byte[]) args[4]);
                thrown = null;
            }
        }

        static void install() throws NoSuchMethodException {
            logInstall();
            Class<?> class2fix = DefineClassHelper.class;
            installAgent(class2fix.getName(), new DefineClassHelperFix(class2fix));
        }
    }

    private static void logInstall() {
//        for (StackTraceElement ste : new Throwable().getStackTrace()) {
//            String msg = "" + ste;
//            if (msg.contains("install")) {
//                System.out.println(msg);
//                return;
//            }
//        }
    }
}
