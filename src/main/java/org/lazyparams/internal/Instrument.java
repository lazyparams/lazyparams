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

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.matcher.ElementMatchers;

import org.junit.platform.engine.support.hierarchical.ThrowableCollector;
import org.lazyparams.showcase.TestInstance_Lifecycle_PER_METHOD_REPETITION;

/**
 * @author Henrik Kaipe
 */
public class Instrument {
    private Instrument() {}

    static final Instrumentation instrumentation = new Object() {
        Instrumentation locate() {
            try {
                try {
                    Instrumentation alreadyInstalled = ByteBuddyAgent.getInstrumentation();
                    if (null != alreadyInstalled) {
                        return alreadyInstalled;
                    }
                } catch (Exception possibyNotYetInstalled) {/* Instead try to install: */}
                return ByteBuddyAgent.install();
            } catch (Throwable ex) {
                ex.printStackTrace();
                System.err.println("Unable to install Lazyparams!");
                return null;
            }
        }
    }.locate();

    private static final AgentBuilder builderStub = new AgentBuilder.Default() {
        AgentBuilder withSelectionOfIgnores_and_disabledClassFormatChanges() {
            List<AgentBuilder.RawMatcher> commonIgnores = new ArrayList<RawMatcher>();
            commonIgnores.add(new AgentBuilder.RawMatcher.ForElementMatchers(
                ElementMatchers.nameStartsWith("java.")
                .or(ElementMatchers.nameStartsWith("jdk."))
                .or(ElementMatchers.nameStartsWith("com.sun."))
                .or(ElementMatchers.nameStartsWith("net.bytebuddy."))
                .or(ElementMatchers.nameContains("$ByteBuddy$"))
                .or(ElementMatchers.not(ElementMatchers.nameStartsWith("org.junit."))
                    .and(ElementMatchers.not(ElementMatchers.nameContains("Test")))
                    .and(ElementMatchers.not(ElementMatchers.nameContains("Node")))
                    .and(ElementMatchers.not(ElementMatchers.nameContains("Desc")))
                    .and(ElementMatchers.not(ElementMatchers.nameContains("Runner"))))
            ));
            try {
                if (null != this.ignoreMatcher) {
                    commonIgnores.add(this.ignoreMatcher);
                }
            } catch (Throwable oldBytebuddy_perhaps_1_7_x) {
                try {
                    Object ignoreMatcher = AgentBuilder.Default.class
                            .getDeclaredField("ignoredTypeMatcher").get(this);
                    commonIgnores.add((AgentBuilder.RawMatcher) ignoreMatcher);
                } catch (Exception giveUp_and_dontAttemptToFindTheDefaultIgnoreMatcher) {
                    System.err.println(giveUp_and_dontAttemptToFindTheDefaultIgnoreMatcher.getMessage());
                }
            }
            commonIgnores.add(new AgentBuilder.RawMatcher
                    .ForElementMatchers(ElementMatchers.isInterface()));

            AgentBuilder.Ignored builderWithIgnores = new AgentBuilder.Default()
                    .disableClassFormatChanges()
                    .ignore(commonIgnores.get(0));
            for (AgentBuilder.RawMatcher nextIgnore : commonIgnores.subList(1, commonIgnores.size())) {
                builderWithIgnores = builderWithIgnores.or(nextIgnore);
            }
            return builderWithIgnores;
        }
    }.withSelectionOfIgnores_and_disabledClassFormatChanges();

    private static AgentBuilder transformerDefinition;

    private static final ConcurrentMap<Instrumentation,ResettableClassFileTransformer> installations =
            new ConcurrentHashMap<Instrumentation, ResettableClassFileTransformer>();
    private static final Lock installationsLock = new ReentrantLock();

    private static boolean junitPlatformOnClasspath() {
        try {
            return 0 < ThrowableCollector.class.getName().length();
        } catch (Throwable platformNotOnClasspath_orOldJavaVersion) {
            return false;
        }
    }

    private static AgentBuilder appendAllPlatformHierarchialAdvice(AgentBuilder builder) {
        if (false == junitPlatformOnClasspath()) {
            return builder;
        }
        for (Class<?> eachClass : ProvideJunitPlatformHierarchical.class.getDeclaredClasses()) {
            if (AdviceFor.class.isAssignableFrom(eachClass)) {
                try {
                    Constructor<? extends AdviceFor> constr = eachClass
                            .asSubclass(AdviceFor.class).getDeclaredConstructor();
                    constr.setAccessible(true);
                    builder = constr.newInstance().append(builder);
                } catch (Exception ex) {
                    System.err.println("Fail to append " + eachClass.getSimpleName()
                            + ": " + ex.getMessage());
//                    ex.printStackTrace();
                }
            }
        }
        try {
            for (AdviceFor<?> eachAdvice : nestedAdviceOf(
                    TestInstance_Lifecycle_PER_METHOD_REPETITION.class)) {
                builder = eachAdvice.append(builder);
            }
        } catch (Throwable ignoreSupportForLazyInitialization) {}
        return builder;
    }

    private static AgentBuilder appendAllVintageAdvice(AgentBuilder builder) {
        Collection<AdviceFor<?>> allVintageAdvice;
        try {
            allVintageAdvice = nestedAdviceOf(ProvideJunitVintage.class);
        } catch (VirtualMachineError oom) {
            throw oom;
        } catch (Throwable junitNotOnClasspath) {
            return builder;
        }
        for (AdviceFor<?> eachAdvice : allVintageAdvice) {
            try {
                builder = eachAdvice.append(builder);
            } catch (Throwable ex) {
                System.err.println("Fail to append "
                        + eachAdvice.getClass().getSimpleName() + ": " + ex.getMessage());
//                ex.printStackTrace();
            }
        }
        return builder;
    }

    private static Collection<AdviceFor<?>> nestedAdviceOf(Class<?> outerClass) {
        TreeMap<String,AdviceFor<?>> sortedAdvices =
                new TreeMap<String,AdviceFor<?>>();
        for (Class<?> nestedClass : outerClass.getDeclaredClasses()) {
            if (AdviceFor.class.isAssignableFrom(nestedClass)) {
                try {
                    Constructor<? extends AdviceFor> constr = nestedClass
                            .asSubclass(AdviceFor.class).getDeclaredConstructor();
                    constr.setAccessible(true);
                    sortedAdvices.put(nestedClass.getName(), constr.newInstance());
                } catch (VirtualMachineError oom) {
                    throw oom;
                } catch (Throwable ex) {
                    System.err.println("Failed to append "
                            + nestedClass.getSimpleName() + ": " + ex.getMessage());
//                    ex.printStackTrace();
                }
            }
        }
        return sortedAdvices.values();
    }

    public static void install() {
        installOn(instrumentation);
    }
    public static void uninstall() {
        resetOn(instrumentation);
    }

    private static Method resolveOnLoader(ClassLoader loader,
            Class cls, String methodName, Class... methodParams)
    throws Exception {
        if (cls.getClassLoader() == loader) {
            /* Already on right class loader! */
            return null;
        } else {
            return Class.forName(cls.getName(), false, loader)
                    .getMethod(methodName, methodParams);
        }
    }

    public static Method resolveOnProvidingClassLoader(
            Class cls, String methodName, Class... methodParams) {
        for (ClassLoader loader : new ClassLoader[] {
            resolveVintageLoader(), resolvePlatformLoader(),
            null == instrumentation ? null : instrumentation.getClass().getClassLoader()
        }) {
            if (null != loader) {
                try {
                    return resolveOnLoader(loader, cls, methodName, methodParams);
                } catch (Exception ignoreAndProceedWithNextClassLoader) {}
            }
        }
        System.err.println("Unable to resolve " + cls.getSimpleName()
                + "#" + methodName + "(...) on provider class-loader");
        return null;
    }

    private static ClassLoader resolveVintageLoader() {
        try {
            return org.junit.runner.notification.RunNotifier.class.getClassLoader();
        } catch (Error noSuchClass) {
            return null;
        }
    }
    private static ClassLoader resolvePlatformLoader() {
        try {
            return org.junit.platform.engine.support.hierarchical.Node.class.getClassLoader();
        } catch (Error noSuchClass) {
            return null;
        }
    }

    private static AgentBuilder.RedefinitionStrategy redefStrategyFor(
            Instrumentation instrumentation) {
        return instrumentation.isRetransformClassesSupported()
                ? AgentBuilder.RedefinitionStrategy.RETRANSFORMATION
                : AgentBuilder.RedefinitionStrategy.REDEFINITION;
    }

    private static void installOn(final Instrumentation instrumentation2install) {
        if (installations.containsKey(instrumentation2install)) {
            /* Already installed ... */
            return;
        }
        installationsLock.lock();
        try {
            if (null == transformerDefinition) {
                transformerDefinition = appendAllVintageAdvice(
                        appendAllPlatformHierarchialAdvice(builderStub));
            }
            if (false == installations.containsKey(instrumentation2install)) {
                ResettableClassFileTransformer newTransformer = transformerDefinition
                        .with(redefStrategyFor(instrumentation2install))
                        .with(AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.SinglePass.INSTANCE)
//                        .with(new AgentBuilder.Listener() {
//                    @Override
//                    public void onDiscovery(String string, ClassLoader cl, JavaModule jm, boolean bln) {
////                        System.err.println("Discovered " + string);
//                    }
//                    @Override
//                    public void onTransformation(TypeDescription td, ClassLoader cl, JavaModule jm, boolean bln, DynamicType dt) {
//                        System.err.println("Transformed " + td.getName());
//                    }
//                    public void onIgnored(TypeDescription td, ClassLoader cl, JavaModule jm, boolean bln) {
//                    }
//                    public void onError(String string, ClassLoader cl, JavaModule jm, boolean bln, Throwable thrwbl) {
//                        System.err.println("Error on " + string);
//                    }
//                    public void onComplete(String string, ClassLoader cl, JavaModule jm, boolean bln) {
////                        System.err.println("Completed " + string);
//                    }
//                        })
                        .installOn(instrumentation2install);
                ResettableClassFileTransformer oldTransformerThatShouldNotHaveExisted =
                        installations.put(instrumentation2install, newTransformer);
                if (null != oldTransformerThatShouldNotHaveExisted) {
                    new Error("Something is wrong ..."
                            + "It seems a concurrent transformer was installed and will now be uninstalled")
                            .printStackTrace();
                    reset(oldTransformerThatShouldNotHaveExisted,
                            instrumentation2install);
                }
            }
        } finally {
            installationsLock.unlock();
        }
    }
    private static void reset(
            ResettableClassFileTransformer transformer2reset,
            Instrumentation instrumentation2uninstallFrom) {
        transformer2reset.reset(instrumentation2uninstallFrom,
                redefStrategyFor(instrumentation2uninstallFrom),
                AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.SinglePass.INSTANCE);
    }
    private static void resetOn(Instrumentation instrumentation2uninstall) {
        installationsLock.lock();
        try {
            ResettableClassFileTransformer transformer2reset =
                    installations.remove(instrumentation2uninstall);
            if (null != transformer2reset) {
                reset(transformer2reset, instrumentation2uninstall);
                ConfigurationContext.resetAllCurrentConfig();
            }
        } finally {
            installationsLock.unlock();
        }
    }
}
