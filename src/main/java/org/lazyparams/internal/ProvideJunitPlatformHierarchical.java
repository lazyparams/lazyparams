/*
 * Copyright 2024-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.internal;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.engine.support.hierarchical.EngineExecutionContext;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutorService;
import org.junit.platform.engine.support.hierarchical.Node;
import org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService;
import org.junit.platform.engine.support.hierarchical.ThrowableCollector;

import org.lazyparams.LazyParamsCoreUtil;

/**
 * Support hierarchical execution with JUnit Platform.
 *
 * @author Henrik Kaipe
 */
public class ProvideJunitPlatformHierarchical implements EngineExecutionListener {

    private static final Consumer<EngineExecutionListener> noopListenerConsumer =
            new Consumer<EngineExecutionListener>() {
        @Override public void accept(EngineExecutionListener delegateListener) {}
    };
    private static final Predicate<Object> nonePredicate = new Predicate<Object>() {
        @Override public boolean test(Object t) { return false; }
    };

    private static final ContextLifecycleProviderFacade<TestDescriptor> lifecycleFacade =
            new ContextLifecycleProviderFacade<TestDescriptor>();

    private static final WeakIdentityHashMap<TestDescriptor,DescriptorContextGuard>
            pendingContextGuards = new WeakIdentityHashMap<TestDescriptor,DescriptorContextGuard>();

    private final EngineExecutionListener coreListener;

    /**
     * Only used when repeated test executions are launched after detection of
     * LazyParams test-execution with pending parameter value combinations.
     */
    private volatile int failureCount = 0, totalCount = 0;

    private ProvideJunitPlatformHierarchical(EngineExecutionListener delegateListener) {
        this.coreListener = delegateListener;
    }

    /**
     * Used by {@link #executionSkipped(TestDescriptor,String)}
     * and {@link #reportingEntryPublished(TestDescriptor,ReportEntry)},
     * which are not properly tested - so there could very possibly be some
     * issue with this method and how it is used.
     * TODO: Verify this functionality!
     */
    private EngineExecutionListener designateListenerFor(TestDescriptor testDescriptor) {
        if (testDescriptor instanceof DescriptorContextGuard) {
            return ((DescriptorContextGuard)testDescriptor).delayingListener;
        }

        Optional<TestDescriptor> parent = testDescriptor.getParent();
        if (false == parent.isPresent()) {
            return coreListener;
        } else if (parent
                .filter(DescriptorContextGuard.isDescriptorContextGuard)
                .isPresent()) {
            return ((DescriptorContextGuard)parent.get()).delayingListener;
        }
        DescriptorContextGuard<?,?> mappedGuard = pendingContextGuards.get(parent.get());
        if (null != mappedGuard) {
            testDescriptor.setParent(mappedGuard);
            return mappedGuard.delayingListener;
        }

        Map<TestDescriptor,Object> ancestorsCovered = new IdentityHashMap<TestDescriptor,Object>();
        while (null == ancestorsCovered.put(testDescriptor = parent.get(), "")
                && (parent = testDescriptor.getParent()).isPresent()) {
            if (parent
                    .filter(DescriptorContextGuard.isDescriptorContextGuard)
                    .isPresent()) {
                return ((DescriptorContextGuard)parent.get()).delayingListener;
            }
        }
        return coreListener;
    }

    @Override
    public void dynamicTestRegistered(TestDescriptor testDescriptor) {
        if (false == testDescriptor instanceof DescriptorContextGuard
                && false == testDescriptor.getParent()
                .filter(DescriptorContextGuard.isDescriptorContextGuard)
                .isPresent()) {
            if (null == resolvePendingAppendixOnParentOf(testDescriptor)) {
                coreListener.dynamicTestRegistered(testDescriptor);
            }
        }
    }

    /* TODO: Have this method verified and tested!*/
    @Override
    public void executionSkipped(TestDescriptor testDescriptor, String reason) {
        designateListenerFor(testDescriptor)
                .executionSkipped(testDescriptor, reason);
    }

    private CharSequence resolvePendingAppendixOnParentOf(
            TestDescriptor childScopeDescriptor) {
        TestDescriptor parent = childScopeDescriptor instanceof DescriptorContextGuard
                ? ((DescriptorContextGuard)childScopeDescriptor).getSourceParentOrItsGuard()
                : childScopeDescriptor.getParent().orElse(null);

        if (null == parent) {
            /* No parent to inherit from: */
            return null;
        }
        if (false == parent instanceof DescriptorContextGuard) {
            DescriptorContextGuard parentGuard = pendingContextGuards.get(parent);
            if (null != parentGuard) {
                parent = parentGuard;
            } else if (null == lifecycleFacade
                    .resolveDisplayAppendixForScope(parent)) {
                /* No parameters for display to inherit: */
                return null;
            } else {
                parent = DescriptorContextGuard.of(parent, true);
            }
            childScopeDescriptor.setParent(parent);
        } else {
            /* Make sure there is some kind of parent appendix: */
            display("");
        }
        return ((DescriptorContextGuard)parent).getCurrentAppendix();
    }

    private void display(CharSequence displayText) {
        Object displayKey = new Object();
        LazyParamsCoreUtil.displayOnFailure(displayKey, displayText);
        LazyParamsCoreUtil.displayOnSuccess(displayKey, displayText);
    }

    @Override
    public void executionStarted(TestDescriptor testDescriptor) {
//        System.out.println("Starting " + testDescriptor.getDisplayName());
        CharSequence pendingParentAppendix =
                resolvePendingAppendixOnParentOf(testDescriptor);
        lifecycleFacade.openExecutionScope(testDescriptor);
        if (false == testDescriptor instanceof DescriptorContextGuard
                && false == testDescriptor.getParent()
                .filter(DescriptorContextGuard.isDescriptorContextGuard)
                .isPresent()) {
            coreListener.executionStarted(testDescriptor);

        } else if (null != pendingParentAppendix) {
            /*
             * This is not very pretty.
             * It's a workaround, because many test-execution environments
             * will only display parent class- or method-name, regardless
             * of what is specified by {@link TestDescriptor#getDisplayName()}.
             * Therefore the display-name appendix of parent is here made
             * first part of its child display-name appendix.
             */
            display(pendingParentAppendix);
            display(" /");
        }
    }

    private TestExecutionResult loopThroughPendingRepeats(
            DescriptorContextGuard<?,?> repeatContext,
            TestExecutionResult firstResult) {
        if (null != repeatContext.maxReached) {
            return TestExecutionResult.failed(repeatContext.maxReached);
        }

        ProvideJunitPlatformHierarchical repeatListener =
                new ProvideJunitPlatformHierarchical(repeatContext.delayingListener);
        repeatListener.totalCount = 1;
        repeatListener.failureCount = TestExecutionResult.Status
                .SUCCESSFUL.equals(firstResult.getStatus()) ? 0 : 1;
        DescriptorContextGuard<?,?> repeatDescriptor;
        do {
            repeatDescriptor = DescriptorContextGuard.of(
                    repeatContext.guardedTestDescriptor.get(),
                    false);
            repeatDescriptor.preservedState = repeatContext.preservedState;
            repeatContext.dynamicExecutor.execute(
                    repeatDescriptor, repeatListener);
            repeatDescriptor.finalizeParent(repeatContext.finalParent);
            repeatDescriptor.pendingNotifications.getAndSet(noopListenerConsumer)
                    .accept(repeatContext.delayingListener);
        } while (false == repeatDescriptor.closeScope());

        if (null != repeatDescriptor.maxReached) {
            return TestExecutionResult.failed(repeatDescriptor.maxReached);

        } else if (1 <= repeatListener.failureCount) {
            return TestExecutionResult.failed(new AssertionError(repeatListener.failureCount
                    + (1 == repeatListener.failureCount ? " test failed (total " : " tests failed (total ")
                    + repeatListener.totalCount + ")"));
        } else {
            return TestExecutionResult.successful();
        }
    }

    @Override
    public void executionFinished(
            TestDescriptor testDescriptor,
            TestExecutionResult testExecutionResult) {
        if (testDescriptor instanceof DescriptorContextGuard) {
            /*
             * This is repetition in progress ...
             */
            ((DescriptorContextGuard)testDescriptor)
                    .finishAndCloseScope(testExecutionResult);
            ++totalCount;
            if (false == TestExecutionResult.Status.SUCCESSFUL
                    .equals(testExecutionResult.getStatus())) {
                ++failureCount;
            }
//            System.out.println("Finishing " + testDescriptor.getDisplayName());
            return;
        }

        final DescriptorContextGuard<?,?> guard0 =
                pendingContextGuards.remove(testDescriptor);
        if (null == guard0) {
            /*
             * This can theoretically happen if initial install (e.g. initial usage)
             * took place during NodeTestTask#reportCompletion() and would
             * probably require for it to have happened on a concurrent thread.
             * We here try to resolve the theoretical scenario by treating test
             * as a completed non-parameterized test!
             */
            try {
                /*
                 * Close possibly pending execution scope, in order to avoid
                 * leaking live parameters to the next test that
                 * is about to be executed on this thread.
                 */
                if (false == lifecycleFacade.closeExecutionScope(
                        testDescriptor, null)) {
                    System.err.println("Discard pending repeats from parameterization that seems"
                            + " to have been introduced during NodeTestTask#reportCompletion()");
                    lifecycleFacade.closeExecutionScope(
                            testDescriptor.getParent().orElse(testDescriptor),
                            null);
                }
            } catch (ContextLifecycleProviderFacade.MaxRepeatCount canBeIgnoredBecause) {
                /*this situation has cleared any pending parameter combinations from pending scope*/
            }

            coreListener.executionFinished(testDescriptor, testExecutionResult);
//            System.out.println("Finishing " + testDescriptor.getDisplayName());
            return;
        }

        DescriptorContextGuard pendingParent = (DescriptorContextGuard) testDescriptor
                .getParent()
                .filter(DescriptorContextGuard.isDescriptorContextGuard)
                .orElse(null);

        if (false == guard0.finishAndCloseScope(testExecutionResult)
                || null != guard0.maxReached) {
            if (null != pendingParent) {
                testDescriptor = asLateParent(testDescriptor);
            }
            guard0.finalizeParent(testDescriptor);
            testExecutionResult = loopThroughPendingRepeats(
                    guard0, testExecutionResult);

            EngineExecutionListener delegateListener = testDescriptor.getParent()
                    .filter(DescriptorContextGuard.isDescriptorContextGuard)
                    .map(new Function<TestDescriptor,EngineExecutionListener>() {
                @Override public EngineExecutionListener apply(TestDescriptor td) {
                    DescriptorContextGuard parentGuard = (DescriptorContextGuard) td;
                    parentGuard.ensureStarted();
                    return parentGuard.delayingListener;
                }
            }).orElse(coreListener);
            if (testDescriptor instanceof DescriptorContextGuard) {
                DescriptorContextGuard<?,?> guardRepeatRoot =
                        (DescriptorContextGuard<?,?>) testDescriptor;
                CharSequence displayAppendixOnParent = lifecycleFacade
                        .resolveDisplayAppendixForScope(guardRepeatRoot.finalParent);
                if (null != displayAppendixOnParent) {
                    guardRepeatRoot.finalizedDisplayAppendix =
                            displayAppendixOnParent.toString();
                }
                guardRepeatRoot.ensureStarted();
                guardRepeatRoot.pendingNotifications.getAndSet(noopListenerConsumer)
                        .accept(delegateListener);
            }
            guard0.pendingNotifications.getAndSet(noopListenerConsumer)
                    .accept(delegateListener);
            delegateListener.executionFinished(testDescriptor,
                    executeSuspendedCleanUp(guard0, testExecutionResult));

        } else if (null != pendingParent) {
            pendingParent.ensureStarted();
            guard0.finalizeParent(pendingParent);
            guard0.pendingNotifications.getAndSet(noopListenerConsumer)
                    .accept(pendingParent.delayingListener);
            executeSuspendedCleanUp(guard0, testExecutionResult);

        } else {
            Consumer<EngineExecutionListener> pendingNotifications =
                    guard0.pendingNotifications.getAndSet(noopListenerConsumer);
            if (testDescriptor.getChildren().isEmpty()
                    && false == guard0.startupIsPending.get()) {
                /*Need to handle possible lazy parameterization on dynamicly registered tests:*/
                pendingNotifications.accept(
                        coreListenerWithFilterForEventsOn(guard0));

            } else if (testDescriptor.getChildren().stream().anyMatch(
                            new Predicate<TestDescriptor>() {
                @Override public boolean test(TestDescriptor child) {
                    return guard0 == child.getParent().orElse(null);
                }
            })) {
                if (false == guard0.getParent()
                        .filter(DescriptorContextGuard.isDescriptorContextGuard)
                        .isPresent()) {
                    guard0.finalizeParent(testDescriptor);
                }
                pendingNotifications.accept(coreListener);
            }
            coreListener.executionFinished(testDescriptor,
                    executeSuspendedCleanUp(guard0, testExecutionResult));
        }
//        System.out.println("Finishing " + testDescriptor.getDisplayName());
    }

    private EngineExecutionListener coreListenerWithFilterForEventsOn(
            final DescriptorContextGuard<?,?> guard2filter) {
        return (EngineExecutionListener) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[] {EngineExecutionListener.class},
                new InvocationHandler() {

            boolean descriptorContextGuardIsFirstOf(Object[] args) {
                return null != args && 1 <= args.length
                        && args[0] instanceof DescriptorContextGuard;
            }

            @Override
            public Object invoke(Object __, Method event, Object[] args)
            throws Throwable {

                if (descriptorContextGuardIsFirstOf(args)) {
                    DescriptorContextGuard<?,?> guardDescriptor =
                            (DescriptorContextGuard<?,?>) args[0];
                    if (guard2filter == guardDescriptor) {
                        return null;
                    } else if (guard2filter == guardDescriptor.getParent().orElse(null)) {
                        guardDescriptor.finalizeParent(guard2filter.guardedTestDescriptor.get());
                    }
                }

                try {
                    return event.invoke(coreListener, args);
                } catch (InvocationTargetException ex) {
                    System.err.println(event.getName() + " issue for arguments "
                            + (null == args ? null : Arrays.asList(args)));
                    throw ex.getTargetException();
                }
            }
        });
    }

    private TestExecutionResult executeSuspendedCleanUp(
            DescriptorContextGuard<?,?> descriptorGuard,
            TestExecutionResult preliminaryResult) {
        descriptorGuard.preservedState = null;
        if (descriptorGuard.suspendedCleanups.isEmpty()) {
//            System.err.println("Expected some expected suspended cleanup - but nothing to see here!");
            return preliminaryResult;

        } else if (null == resolvePendingAppendixOnParentOf(descriptorGuard)) {
            ThrowableCollector collector = new ThrowableCollector(nonePredicate);
            switch (preliminaryResult.getStatus()) {
                default:
                    final Optional<Throwable> failure = preliminaryResult.getThrowable();
                    if (failure.isPresent()) {
                        collector.execute(new ThrowableCollector.Executable() {
                            @Override public void execute() throws Throwable {
                                throw failure.get();
                            }
                        });
                    }
                    break;
                case ABORTED: case SUCCESSFUL:
            }
            ThrowableCollector.Executable[] cleanups = descriptorGuard.suspendedCleanups
                    .values().toArray(new ThrowableCollector.Executable[0]);
            descriptorGuard.suspendedCleanups.clear();
            for (ThrowableCollector.Executable eachCleanup : cleanups) {
                collector.execute(eachCleanup);
            }
            return TestExecutionResult.Status.ABORTED == preliminaryResult.getStatus()
                    || collector.isEmpty()
                    ? preliminaryResult/*is preserved and made final*/
                    : collector.toTestExecutionResult()/*replaces preliminary result*/;

        } else {
            DescriptorContextGuard
                    .of(descriptorGuard.getSourceParentOrItsGuard(), true)
                    .suspendedCleanups.putAll(descriptorGuard.suspendedCleanups);
            descriptorGuard.suspendedCleanups.clear();
            return preliminaryResult;
        }
    }

    private TestDescriptor asLateParent(TestDescriptor templateDescriptor) {
        DescriptorContextGuard<?,?> lateParent =
                DescriptorContextGuard.asGuardOf(templateDescriptor);
        lateParent.finalizeParent(templateDescriptor.getParent().get());
        return lateParent;
    }

    /* TODO: Have this method verified and tested!*/
    @Override
    public void reportingEntryPublished(TestDescriptor testDescriptor, ReportEntry entry) {
        designateListenerFor(testDescriptor)
                .reportingEntryPublished(testDescriptor, entry);
    }

    static void throwUnchecked(Throwable t) throws RuntimeException {
        ProvideJunitPlatformHierarchical.<RuntimeException>throwGeneric(t);
    }
    private static <E extends Throwable> void throwGeneric(Throwable t) throws E {
        throw (E)t;
    }

    private static abstract class ReturnValueAdjustment<R,T extends R> {
        private final Class<R> returnTypeOfConcern;
        private final Class<T> desiredReturnValueImplementationType;
        {
            Type[] typeParams = ((ParameterizedType)getClass().getGenericSuperclass())
                    .getActualTypeArguments();
            returnTypeOfConcern = (Class<R>) typeParams[0];
            desiredReturnValueImplementationType = (Class<T>) typeParams[1];
            assert returnTypeOfConcern != desiredReturnValueImplementationType;
        }
        boolean concernsReturnTypeOf(Method method) {
            Class<?> methodReturnType = method.getReturnType();
            return returnTypeOfConcern.isAssignableFrom(methodReturnType)
                    && methodReturnType.isAssignableFrom(desiredReturnValueImplementationType);
        }
        Object apply(Object originalReturnValue) {
            return returnTypeOfConcern.isInstance(originalReturnValue)
                    && false == desiredReturnValueImplementationType.isInstance(originalReturnValue)
                    ? adjust(returnTypeOfConcern.cast(originalReturnValue))
                    : originalReturnValue;
        }
        abstract T adjust(R originalReturnValue);
    }

    private static Class<?> hierarchicalPackageClass(String simpleClassName) {
        String fullClassName = Node.class.getName()
                .substring(0, Node.class.getName().lastIndexOf('.') + 1)
                + simpleClassName;
        try {
            return Class.forName(fullClassName, false, Node.class.getClassLoader());
        } catch (ClassNotFoundException ex) {
            throw new NoClassDefFoundError(ex.getMessage());
        }
    }

    public static class NodeTestTaskContextAdvice extends AdviceFor<Object> {

        private static final ReturnValueAdjustment<?,?> listenerReturnValueAdjustment =
                new ReturnValueAdjustment<EngineExecutionListener,ProvideJunitPlatformHierarchical>() {
            @Override
            ProvideJunitPlatformHierarchical adjust(EngineExecutionListener delegateeListener) {
                return new ProvideJunitPlatformHierarchical(delegateeListener);
            }
        };
        private static final ReturnValueAdjustment<?,?> executorServiceReturnValueAdjustment =
                new ReturnValueAdjustment<
                        HierarchicalTestExecutorService,
                        SameThreadHierarchicalTestExecutorService>() {
            @Override
            Object apply(Object originalReturnValue) {
                Object newReturnValue = super.apply(originalReturnValue);
                return newReturnValue != originalReturnValue
                        && isOngoingLazyParamsRepetition()
                        ? newReturnValue : originalReturnValue;
            }
            @Override
            SameThreadHierarchicalTestExecutorService adjust(HierarchicalTestExecutorService discarded) {
                return new SameThreadHierarchicalTestExecutorService();
            }
        };

        NodeTestTaskContextAdvice() {
            super(hierarchicalPackageClass("NodeTestTaskContext"));
            for (Method m : getDeclaredMethods()) {
                if (false == Modifier.isPrivate(m.getModifiers())) {
                    if (executorServiceReturnValueAdjustment.concernsReturnTypeOf(m)
                            || listenerReturnValueAdjustment.concernsReturnTypeOf(m)) {
                        on(m);
                    }
                }
            }
        }

        /**
         * Used by {@link #executorServiceReturnValueAdjustment} when deciding
         * whether to force {@link SameThreadHierarchicalTestExecutorService}
         * during LazyParams repetition.
         * TODO: Need to test and verify preserved concurrent execution of
         * dynamic tests when outside of LazyParams context!
         */
        private static boolean isOngoingLazyParamsRepetition() {
            Predicate<StackTraceElement> isRepeaterClass = new Predicate<StackTraceElement>() {
                final String repeaterClassName =
                        ProvideJunitPlatformHierarchical.class.getName();
                @Override public boolean test(StackTraceElement t) {
                    /*
                     * TODO: This is probably not sufficient.
                     * A more careful examination of execution state is needed.
                     */
                    return repeaterClassName.equals(t.getClassName());
                }
            };
            return Arrays.asList(Thread.currentThread().getStackTrace())
                    .stream().anyMatch(isRepeaterClass);
        }

        public static Object adjustReturnValue(Object returnValue) {
            return executorServiceReturnValueAdjustment.apply(
                    listenerReturnValueAdjustment.apply(returnValue));
        }

        @Advice.OnMethodExit(inline = true)
        @SuppressWarnings("UnusedAssignment"/*because of how Byte Buddy works!*/)
        private static void onReturn(
                @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC)
                        Object returnValue) {
            returnValue = adjustReturnValue(returnValue);
        }
    }

    public static class NodeTestTaskAdvice
    extends AdviceFor<HierarchicalTestExecutorService.TestTask> {
        static final Class<? extends HierarchicalTestExecutorService.TestTask>
                targetClass = hierarchicalPackageClass("NodeTestTask")
                        .asSubclass(HierarchicalTestExecutorService.TestTask.class);

        private enum Loop {
            throwableCollectors(false, targetClass, ThrowableCollector.class),
            contexts          (true, targetClass, EngineExecutionContext.class),
            nodes            (true, targetClass, Node.class),
            throwables      (false, ThrowableCollector.class, Throwable.class);

            final Collection<Field> fields;

            Loop(boolean includeFinalFields, Class<?> targetClass, Class<?> fieldType) {
                List<Field> targetFields = new ArrayList<Field>(2);
                for (Class c = targetClass; Object.class != c; c = c.getSuperclass()) {
                    for (Field f : c.getDeclaredFields()) {
                        int mods = f.getModifiers();
                        if (false == Modifier.isStatic(mods)
                                && f.getType() == fieldType
                                && (includeFinalFields
                                        || false == Modifier.isFinal(mods))) {
                            targetFields.add(f);
                        }
                    }
                }
                if (targetFields.isEmpty()) {
                    throw new Error(name() + " fields are not found on " + targetClass);
                }
                this.fields = 1 == targetFields.size()
                        ? Collections.singleton(targetFields.get(0))
                        : Collections.unmodifiableCollection(Arrays
                                .asList(targetFields.toArray(new Field[0])));
            }

            <T> UnaryOperator<T> switcherOn(final Object o) {
                return new UnaryOperator<T>() {
                    @Override public T apply(T replacement) {
                        List<T> originals = on(o);
                        if (2 <= originals.size()) {
                            throw new IllegalStateException(
                                    "No unique " + name() + " identified on " + o);
                        }
                        for (Field f : fields) {
                            try {
                                f.set(o, replacement);
                            } catch (Exception mustNeverHappen) {
                                throw new Error(mustNeverHappen);
                            }
                        }
                        return originals.isEmpty() ? null : originals.get(0);
                    }
                };
            }

            <T> List<T> on(Object o, T... type) {
                Class<?> desiredType = type.getClass().getComponentType();
                List<T> ts = new ArrayList<T>(fields.size());
                for (Field f : fields) {
                    try {
                        f.setAccessible(true);
                        T value = (T) f.get(o);
                        if (false == ts.contains(value) && desiredType.isInstance(value)) {
                            ts.add(value);
                        }
                    } catch (Exception mustNeverHappen) {
                        throw new Error(mustNeverHappen);
                    }
                }
                return ts;
            }
        }

        private static final Constructor<? extends Node.DynamicTestExecutor>
                dynamicExecutorConstructor = resolveDynamicExecutorConstructor();

        NodeTestTaskAdvice() throws NoSuchMethodException {
            super(targetClass);
            on(targetClass.getDeclaredMethod("reportCompletion"));
            on(targetClass.getDeclaredMethod("cleanUp"));
        }

        private static Constructor<? extends Node.DynamicTestExecutor>
                resolveDynamicExecutorConstructor() {
            for (Class<?> declaredClass : targetClass.getDeclaredClasses()) {
                if (Node.DynamicTestExecutor.class.isAssignableFrom(declaredClass)) {
                    try {
                        Constructor<? extends Node.DynamicTestExecutor> candidate =
                                declaredClass.asSubclass(Node.DynamicTestExecutor.class)
                                        .getDeclaredConstructor(targetClass);
                        candidate.setAccessible(true);
                        return candidate;
                    } catch (Exception ex) {
                        System.err.println(ex.getMessage());
                    }
                }
            }
            System.err.println("Unable to resolve constructor for desired"
                    + " DynamicTestExecutor class!");
            return null;
        }

        @Advice.OnMethodEnter(inline = false)
        public static void tweaksAtEndOfTask(
                @Advice.This HierarchicalTestExecutorService.TestTask thisTask,
                @Advice.Origin("#m"/*to capture method-name!*/) String methodName) {
            delayedExecutorResolution(thisTask);
            if ("cleanUp".equals(methodName)) {
                suspendCleanUp(thisTask);
            }
        }

        private static void delayedExecutorResolution(
                final HierarchicalTestExecutorService.TestTask thisTask) {

            for (TestDescriptor taskDescriptor : Loop.nodes.<TestDescriptor>on(thisTask)) {
                if (taskDescriptor instanceof DescriptorContextGuard
                        && null != ((DescriptorContextGuard)taskDescriptor).dynamicExecutor
                        || pendingContextGuards.containsKey(taskDescriptor)
                        && null != pendingContextGuards.get(taskDescriptor).dynamicExecutor) {
                    /*
                     * All is fine and already taken care of by
                     * DynamicTestExecutorAdvice#afterAwaitFinished(...) !
                     */
                    return;
                }
            }

//            System.out.println("Reconciliation of descriptor-guard and dynamic executor: ");
            try {
                DynamicTestExecutorAdvice.afterAwaitFinished(
                        dynamicExecutorConstructor.newInstance(thisTask),
                        thisTask, null);
            } catch (InvocationTargetException ex) {
                throwUnchecked(ex.getTargetException());
            } catch (Exception ex) {
                throwUnchecked(ex);
            }
        }

        private static <TD extends TestDescriptor & Node<CTX>,CTX extends EngineExecutionContext>
                void suspendCleanUp(HierarchicalTestExecutorService.TestTask thisTask) {
            final TD testDescriptor = Loop.nodes.<TD>on(thisTask)
                    .stream().findAny().orElse(null);
            if (false == testDescriptor instanceof TestDescriptor
                    || ((TestDescriptor)testDescriptor).isRoot()) {
                /*Assume no actual cleanup will happen here: */ return;
            }
            final List<CTX> contexts = Loop.contexts.<CTX>on(thisTask);
            ThrowableCollectorInterception.setup(
                    Loop.throwableCollectors.<ThrowableCollector>switcherOn(thisTask),
                    new UnaryOperator<ThrowableCollector.Executable>() {
                @Override
                public ThrowableCollector.Executable apply(
                        ThrowableCollector.Executable coreCleanUpExecution) {
                    return new ThrowableCollector.Executable() {
                        @Override public void execute() throws Throwable {
                            storeCleanUpOnDescriptorContextGuards();
                            throwPendingThrowable();
                        }

                        void storeCleanUpOnDescriptorContextGuards() {
                            DescriptorContextGuard<?,?> guard =
                                    DescriptorContextGuard.of(testDescriptor, true);
                            final WeakReference<DescriptorContextGuard<?,?>>
                                    weakGuard = new WeakReference(guard);
                            guard.suspendedCleanups.put(testDescriptor.getUniqueId(),
                                    new ThrowableCollector.Executable() {
                                @Override public void execute() throws Throwable {
                                    DescriptorContextGuard<?,?> discardedGuard = weakGuard.get();
                                    if (null != discardedGuard) {
                                        weakGuard.clear();
                                        discardedGuard.preservedState = null;
                                        discardedGuard.suspendedCleanups.clear();
                                    }
                                    for (CTX eachCtx : contexts) {
                                        testDescriptor.cleanUp(eachCtx);
                                    }
                                }
                            });
                        }
                    };
                }
                void throwPendingThrowable() throws Throwable {
                    List<ThrowableCollector> collectors =
                            new ArrayList<ThrowableCollector>();
                    for (CTX ctx : contexts) {
                        collectCollectors(ctx, collectors);
                    }
                    List<Throwable> throwables =
                            new ArrayList<Throwable>(collectors.size());
                    for (ThrowableCollector eachCollector : collectors) {
                        throwables.add(Loop.throwables
                                .<Throwable>switcherOn(eachCollector)
                                .apply(null));
                    }
                    for (Throwable thrown : throwables) {
                        if (null != thrown) {
                            throw thrown;
                        }
                    }
                }
                void collectCollectors(CTX ctx, List<ThrowableCollector> clc) {
                    List<Throwable> collectorIssues = new ArrayList<Throwable>();
                    for (Method m : ctx.getClass().getMethods()) {
                        if (0 == m.getParameterCount()
                                && ThrowableCollector.class == m.getReturnType()) {
                            try {
                                ThrowableCollector collector =
                                        (ThrowableCollector) m.invoke(ctx);
                                if (null != collector) {
                                    clc.add(collector);
                                }
                            } catch (InvocationTargetException collectorIssue) {
                                Throwable coreIssue = collectorIssue.getTargetException();
                                collectorIssues.add(null != coreIssue
                                        ? coreIssue : collectorIssue);
                            } catch (Exception mustNeverHappen) {
                                throw new Error(mustNeverHappen);
                            }
                        }
                    }
                    if (false == collectorIssues.isEmpty()) {
                        ThrowableCollector lateFake =
                                new ThrowableCollector(new Predicate<Object>() {
                            @Override public boolean test(Object t) { return false; }
                        });
                        for (final Throwable collectorIssue : collectorIssues) {
                            lateFake.execute(new ThrowableCollector.Executable() {
                                @Override public void execute() throws Throwable {
                                    throw collectorIssue;
                                }
                            });
                        }
                        clc.add(lateFake);
                    }
                }
            });
        }
    }

    public static class DynamicTestExecutorAdvice
    extends AdviceFor<Node.DynamicTestExecutor> {

        DynamicTestExecutorAdvice() throws InterruptedException {
            on().awaitFinished();
            on().execute(null, null);
        }

        @Override
        protected ElementMatcher.Junction<? super TypeDescription> definitionsToInstrument() {
            return super.definitionsToInstrument()
                    .and(ElementMatchers.not(ElementMatchers.isStatic()))
                    .and(ElementMatchers.isDeclaredBy(NodeTestTaskAdvice.targetClass));
        }

        @Advice.OnMethodEnter(inline = false)
        public static void beforeExecute(
                @Advice.FieldValue(typing = Assigner.Typing.DYNAMIC, value="this$0")
                final HierarchicalTestExecutorService.TestTask enclosingTask,
                @Advice.Argument(value=0, optional=true) TestDescriptor dynamicTest) {

            if (null == dynamicTest) {
                return;/*Seems like method is #awaitFinished()!*/
            }

            if (dynamicTest instanceof TestDescriptor
                    && false == dynamicTest instanceof DescriptorContextGuard) {
                for (TestDescriptor taskDescriptor : testDescriptorNodesOn(enclosingTask)) {
                    dynamicTest.setParent(taskDescriptor);
                    DescriptorContextGuard.setupAsParent(
                            dynamicTest, dynamicTest.getChildren());
                    return;
                }
            }
        }

        @Advice.OnMethodExit(inline = false)
        public static void afterAwaitFinished(
                @Advice.This Node.DynamicTestExecutor executorInstance,
                @Advice.FieldValue(typing = Assigner.Typing.DYNAMIC, value = "this$0")
                        final HierarchicalTestExecutorService.TestTask enclosingTask,
                @Advice.AllArguments Object[] argsThatPinpointsExecuteMethod) {
            if (null != argsThatPinpointsExecuteMethod
                    && 0 < argsThatPinpointsExecuteMethod.length) {
                /* This "OnMethodExit" is not for method #execute(...): */
                return;
            }

            for (TestDescriptor descriptor : testDescriptorNodesOn(enclosingTask)) {
                DescriptorContextGuard
                        .of(descriptor, true)
                        .preserveContextForRepeat(executorInstance);
            }
        }

        private static Iterable<TestDescriptor> testDescriptorNodesOn(
                HierarchicalTestExecutorService.TestTask enclosingNodeTask) {
            return NodeTestTaskAdvice.Loop.nodes.on(enclosingNodeTask);
        }
    }

    public static abstract class DescriptorContextGuard<
            D extends TestDescriptor & Node<C>, C extends EngineExecutionContext>
    implements TestDescriptor, Node<C> {
        private static Predicate<TestDescriptor> isDescriptorContextGuard =
                new Predicate<TestDescriptor>() {
            @Override public boolean test(TestDescriptor td) {
                return td instanceof DescriptorContextGuard;
            }
        };

        private static final PartialInterceptionFactory<DescriptorContextGuard> factory =
                new PartialInterceptionFactory(
                        DescriptorContextGuard.class,
                        new Function<DescriptorContextGuard, Object>() {
            @Override
            public Object apply(DescriptorContextGuard instance) {
                return instance.nodeTestDescriptorProxy;
            }
        }, TestDescriptor.class);

        private final D nodeTestDescriptorProxy = (D) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[] {TestDescriptor.class, Node.class},
                new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] arguments)
            throws Throwable {
                TestDescriptor guarded = guardedTestDescriptor.get();
                try {
                    return method.invoke(guarded instanceof Node
                            || Node.class != method.getDeclaringClass()
                            ? guarded
                            : /*Supported - but rare: */ new Node() {},
                            arguments);
                } catch (InvocationTargetException ex) {
                    throw ex.getTargetException();
                }
            }
        });

        private static final AtomicLong guardCount = new AtomicLong();
        private static final Lock pendingContextGuardsCreationMonitor = new ReentrantLock();

        private TestDescriptor finalParent;
        private boolean scopeOnGuardedDescriptor;
        private final WeakReference<TestDescriptor> guardedTestDescriptor;
        private final UniqueId uniqueId;
        private final AtomicBoolean startupIsPending = new AtomicBoolean(true);

        private String finalizedDisplayAppendix = null;

        private Boolean hasPendingRepetitions;
        private ContextLifecycleProviderFacade.MaxRepeatCount maxReached;
        private Throwable result;
        private Node.DynamicTestExecutor dynamicExecutor;
        private InternalState<TestDescriptor> preservedState;

        private final Map<UniqueId,ThrowableCollector.Executable> suspendedCleanups =
                new LinkedHashMap<UniqueId,ThrowableCollector.Executable>();
        private final AtomicReference<Consumer<EngineExecutionListener>>
                pendingNotifications = new AtomicReference(noopListenerConsumer);
        final EngineExecutionListener delayingListener = (EngineExecutionListener)
                Proxy.newProxyInstance(
                        getClass().getClassLoader(),
                        new Class[] {EngineExecutionListener.class},
                        new InvocationHandler() {
            @Override
            public Object invoke(Object proxy,
                    final Method listenerMethod, final Object[] arguments)
            throws Throwable {
                if ("toString".equals(listenerMethod.getName())) {
                    /** To cut toString() in this manner is useful during debugging! */
                    return toString();
                }
                pendingNotifications.updateAndGet(
                        new UnaryOperator<Consumer<EngineExecutionListener>>() {
                    @Override
                    public Consumer<EngineExecutionListener> apply(
                            Consumer<EngineExecutionListener> current) {
                        return current.andThen(new Consumer<EngineExecutionListener>() {
                            @Override
                            public void accept(EngineExecutionListener listener) {
                                try {
                                    listenerMethod.invoke(listener, arguments);
                                } catch (InvocationTargetException ex) {
                                    throwUnchecked(ex.getTargetException());
                                } catch (Exception ex) {
                                    throwUnchecked(ex);
                                }
                            }
                            /** For debug purposes: */ @Override
                            public String toString() {
                                return listenerMethod.getName() + " ON " + (
                                        null == arguments || 0 == arguments.length
                                        ? "_void_"
                                        : arguments[0] instanceof DescriptorContextGuard
                                        ? ((DescriptorContextGuard)arguments[0]).getDisplayName()
                                        : arguments[0].getClass().getSimpleName());
                            }
                        });
                    }
                });
                return null;
            }

            /** For debug purposes: */ @Override
            public String toString() {
                return getCurrentAppendix() + " DELAYING " + guardedTestDescriptor.get();
            }
        });

        /** For debug purposes: */ @Override
        public String toString() {
            return getCurrentAppendix() + " " + guardedTestDescriptor.get();
        }

        protected DescriptorContextGuard(TestDescriptor guardedTestDescriptor) {
            this.guardedTestDescriptor =
                    new WeakReference<TestDescriptor>(guardedTestDescriptor);
            this.uniqueId = guardedTestDescriptor.getUniqueId().append(
                    "LazyParams", "" + guardCount.incrementAndGet());
        }

        static DescriptorContextGuard<?,?> asGuardOf(TestDescriptor descriptor) {
            DescriptorContextGuard<?,?> newGuard = factory.newInstance(descriptor);
            pendingContextGuardsCreationMonitor.lock();
            try {
                DescriptorContextGuard<?,?> concurrentGuard =
                        pendingContextGuards.get(descriptor);
                return null != concurrentGuard ? concurrentGuard : newGuard;
            } finally {
                pendingContextGuardsCreationMonitor.unlock();
            }
        }

        public static DescriptorContextGuard<?,?> of(
                TestDescriptor descriptor, boolean scopeAlreadyInitialized) {
            if (descriptor instanceof DescriptorContextGuard) {
                return (DescriptorContextGuard<?, ?>) descriptor;
            }
            DescriptorContextGuard<?,?> pendingGuard = pendingContextGuards.get(descriptor);
            if (null == pendingGuard) {
                pendingGuard = asGuardOf(descriptor);
                pendingGuard.scopeOnGuardedDescriptor = scopeAlreadyInitialized;
                if (scopeAlreadyInitialized) {
                    pendingContextGuardsCreationMonitor.lock();
                    try {
                        DescriptorContextGuard<?,?> concurrentGuard =
                                pendingContextGuards.get(descriptor);
                        if (null != concurrentGuard) {
                            pendingGuard = concurrentGuard;
                        } else {
                            pendingContextGuards.put(descriptor, pendingGuard);
                        }
                    } finally {
                        pendingContextGuardsCreationMonitor.unlock();
                    }
                }
            }
            return pendingGuard;
        }

        void preserveContextForRepeat(Node.DynamicTestExecutor dynamicExecutor) {
            this.dynamicExecutor = dynamicExecutor;
            this.preservedState = InternalState.of(guardedTestDescriptor.get());
        }

        boolean closeScope() {
            if (null == hasPendingRepetitions) {
                TestDescriptor scopeIdentifier = scopeOnGuardedDescriptor
                        ? guardedTestDescriptor.get() : this;
                try {
                    hasPendingRepetitions = false == lifecycleFacade.closeExecutionScope(
                            scopeIdentifier, result);
                } catch (ContextLifecycleProviderFacade.MaxRepeatCount maxReached) {
                    hasPendingRepetitions = false;
                    this.maxReached = maxReached;
                }
                CharSequence finalAppendix = lifecycleFacade
                        .resolveDisplayAppendixForScope(scopeIdentifier);
                this.finalizedDisplayAppendix = null == finalAppendix
                        ? "" : finalAppendix.toString();
//                System.out.println("Closed scope " + finalAppendix);
            }
            return hasPendingRepetitions.equals(false);
        }

        void ensureStarted() {
            if (startupIsPending.getAndSet(false)) {
                delayingListener.dynamicTestRegistered(this);
//                System.out.println("Registered " + getUniqueId());
//                System.out.println("Displayed as " + getDisplayName());
                delayingListener.executionStarted(this);
            }
        }

        boolean finishAndCloseScope(TestExecutionResult testExecutionResult) {
            result = testExecutionResult.getThrowable().orElse(null);
            ensureStarted();
            delayingListener.executionFinished(this, testExecutionResult);
            return closeScope();
        }

        void finalizeParent(TestDescriptor finalParent) {
            this.finalParent = finalParent;
        }

        CharSequence getCurrentAppendix() {
            if (null != finalizedDisplayAppendix) {
                return finalizedDisplayAppendix;
            } else {
                return lifecycleFacade.resolveDisplayAppendixForScope(
                        scopeOnGuardedDescriptor ? guardedTestDescriptor.get() : this);
            }
        }

        /** Equality by identity */
        @Override public boolean equals(Object other) {
            return this == other;
        }
        /** HashCode by identity */
        @Override public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override
        public UniqueId getUniqueId() {
            return uniqueId;
        }
        @Override
        public String getDisplayName() {
            TestDescriptor guarded = guardedTestDescriptor.get();
            String coreDisplayName = guarded.getDisplayName();
            final String problemEnd = "()";
            if (null != coreDisplayName && coreDisplayName.endsWith(problemEnd)) {
                /* Adjust - as some IDEs don't like appendix after () ...*/
                TestSource source = guarded.getSource().orElse(null);
                if (source instanceof MethodSource && coreDisplayName.equals(
                        ((MethodSource)source).getMethodName() + "()")) {
                    coreDisplayName = coreDisplayName.substring(0,
                            coreDisplayName.length() - problemEnd.length());
                }
            }
            return coreDisplayName + getCurrentAppendix();
        }
        @Override
        public String getLegacyReportingName() {
            return guardedTestDescriptor.get().getLegacyReportingName() + getCurrentAppendix();
        }

        /**
         * No support for {@link TestSource} as of now but this will perhaps
         * change in the future.
         * @return Empty optional
         */
        @Override public Optional<TestSource> getSource() { return Optional.empty(); }
        @Override
        public Optional<TestDescriptor> getParent() {
            if (null != finalParent) {
                return Optional.of(finalParent);
            } else {
                return guardedTestDescriptor.get().getParent();
            }
        }
        TestDescriptor getSourceParentOrItsGuard() {
            TestDescriptor parent = getParent().orElse(null);
            return null != parent && parent == guardedTestDescriptor.get()
                    || parent instanceof DescriptorContextGuard && guardedTestDescriptor.get()
                    == ((DescriptorContextGuard)parent).guardedTestDescriptor.get()
                    ? parent.getParent().orElse(null)
                    : parent;
        }
        @Override public void setParent(TestDescriptor parent) {}
        @Override
        public Set<? extends TestDescriptor> getChildren() {
            Set<? extends TestDescriptor> childs =
                    guardedTestDescriptor.get().getChildren();
            setupAsParent(this, childs);
            return childs;
        }
        static void setupAsParent(
                TestDescriptor newParent,
                Iterable<? extends TestDescriptor> children) {
            for (TestDescriptor eachChild : children) {
                TestDescriptor oldParent = eachChild.getParent().orElse(null);
                if (newParent != oldParent) {
                    eachChild.setParent(newParent);
                    if (oldParent instanceof DescriptorContextGuard) {
                        setupAsParent(eachChild, eachChild.getChildren());
                    }
                }
            }
        }
        @Override public boolean isTest() { return true; }
        @Override public boolean mayRegisterTests() { return true; }

        /**
         * Bypasses the regular {@link Node#prepare(EngineExecutionContext)} invocation
         * and instead restores descriptor state that was preserved with
         * {@link #preserveContextForRepeat(Node.DynamicTestExecutor)}.
         *
         * @return Argument context without modifications
         */
        @Override public C prepare(C context) {
            TestDescriptor testDescriptor = guardedTestDescriptor.get();
            if (null == testDescriptor) {
                throw new IllegalStateException("TestDescriptor has been garbage-collected.");
            }
            preservedState.restoreOn(testDescriptor);
            return context;
        }
        @Override
        public SkipResult shouldBeSkipped(C context) {
            return SkipResult.doNotSkip();
        }
        @Override public ExecutionMode getExecutionMode() { return ExecutionMode.SAME_THREAD; }
        @Override public Set getExclusiveResources() { return Collections.emptySet(); }
    }
}
