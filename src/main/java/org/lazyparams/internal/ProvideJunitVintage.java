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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.ClassRoadie;
import org.junit.internal.runners.TestClass;
import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;
import org.junit.runners.ParentRunner;

import org.lazyparams.LazyParams;

/**
 * @author Henrik Kaipe
 */
public enum ProvideJunitVintage {
    fireTestSuiteFinished {
        void reportUnsupportedStaticScope(RunNotifier notifier, Description suiteCloser) {
            String staticDisplay = lifecycleFacade
                    .resolveDisplayAppendixForScope(suiteCloser)
                    .toString();
            if (false == staticDisplay.isEmpty()) {
                Description msgDesc = Description.createTestDescription(
                        RepeatDescription.classOf(suiteCloser),
                        "No static parameterization support on JUnit-3/4, i.e. only first values picked:"
                        + staticDisplay,
                        RepeatDescription.INSTANCE);
                notifier.fireTestStarted(msgDesc);
                String message = "Parameterization support in static scope requires JUnit-5! \n"
                        + "I.e. no repetition in static scope for resolving parameter values other than (" + staticDisplay + " )";
                try {
                    notifier.fireTestAssumptionFailed(new Failure(msgDesc,
                            new AssumptionViolatedException(message)));
                } catch (Error possiblyAnOldVersionOfJUnit) {
                    notifier.fireTestFailure(
                            new Failure(msgDesc, new RuntimeException(message)));
                }
                notifier.fireTestFinished(msgDesc);

                /*
                 * Open and close faked scope to clear LazerContext
                 * from pending combinations:
                 */
                lifecycleFacade.openExecutionScope(suiteCloser);
                LazyParams.currentScopeConfiguration().setMaxTotalCount(1);
                try {
                    lifecycleFacade.closeExecutionScope(suiteCloser, null);
                } catch (Throwable ignore) {}
            }
        }
        @Override
        boolean handleInterception(RunNotifier notifier, Description base, Failure ___) {
            if (RunNotifier.class == notifier.getClass()) {
                try {
                    Description suiteCloser = base.childlessCopy();
                    if (false == lifecycleFacade.closeExecutionScope(suiteCloser, null)) {
                        reportUnsupportedStaticScope(notifier, suiteCloser);
                    }
                } catch (ContextLifecycleProviderFacade.MaxRepeatCount noProblem) {}
            }
            return true;
        }
    },
    fireTestStarted {
        @Override
        boolean handleInterception(RunNotifier ___, Description desc, Failure ____) {
            if (null == desc.getAnnotation(RepeatDescription.class)) {
                lifecycleFacade.openExecutionScope(desc);
            }
            return true;
        }
    },
    fireTestAssumptionFailed,
    fireTestFailure,
    fireTestFinished {
        /**
         * @return result from first {@link #fireTestFailure} event!
         */
        Throwable recordCounts(Map<Throwable,ProvideJunitVintage> failureEvents,
            AtomicInteger failCount, AtomicInteger skipCount) {
            for (Map.Entry<Throwable,?> failEntry : failureEvents.entrySet()) {
                if (fireTestFailure == failEntry.getValue()) {
                    failCount.incrementAndGet();
                    return failEntry.getKey();
                }
            }
            if (failureEvents.values().contains(fireTestAssumptionFailed)) {
                skipCount.incrementAndGet();
            }
            return null;
        }

        @Override
        boolean handleInterception(
                RunNotifier notifier, Description base, Failure ___) {
            testIdentifierBackup.remove();/*no longer needed after test has ended*/

            Map<Throwable,ProvideJunitVintage> pendingRepeatFailures =
                    pendingFailuresToReport.remove(base);
            if (null == pendingRepeatFailures) {
                try {
                    if (lifecycleFacade.closeExecutionScope(base, null)) {
                        /* No lazy parameters were introduced on this test ... */
                        return true;
                    }
                } catch (ContextLifecycleProviderFacade.MaxRepeatCount butShouldAlreadyHaveBeenHandled) {
                    butShouldAlreadyHaveBeenHandled.printStackTrace();
                    /* This indicates that lazy parameterization was deactivated by
                     * setting max total count to 1 - so dont report as failure: */
                    return true;
                }
                pendingRepeatFailures = new LinkedHashMap<Throwable, ProvideJunitVintage>();
            }

            AtomicInteger
                    failCount = new AtomicInteger(0),
                    skipCount = new AtomicInteger(0);
            recordCounts(pendingRepeatFailures, failCount, skipCount);
            List<Description> reportedRepetitions = new ArrayList<Description>();
            try {
                reportedRepetitions.add(relayRepetitionEvents(
                        notifier, base, pendingRepeatFailures));    
            } catch (Exception justReportOriginalFailureInstead___) {
                return true;
            }
            Throwable repeatProblem = null;
            EnumSet<ProvideJunitVintage> recordedEvents =
                    EnumSet.noneOf(ProvideJunitVintage.class);
            try {
                Request repeatRequest = Request.aClass(RepeatDescription.classOf(base))
                        .filterWith(base);
                Runner repeatRunner = repeatRequest.getRunner();
                if (false == repeatRunner instanceof Filterable) {
                    FilteredRunnerAdvice.record(repeatRunner,
                            Filter.matchMethodDescription(base));
                }

                for (boolean allRepetitionsCompleted = false; false == allRepetitionsCompleted;) {
                    pendingRepeatFailures.clear();
                    RepeatNotifier repeatNotifier = newRepeatNotifier(
                            base, pendingRepeatFailures, recordedEvents);
                    repeatRunner.run(repeatNotifier);

                    if (recordedEvents.isEmpty()) {
                        Iterator<Throwable> iterRepeatProblems =
                                pendingRepeatFailures.keySet().iterator();
                        throw iterRepeatProblems.hasNext()
                                ? iterRepeatProblems.next()
                                : new IllegalStateException(
                                "LazyParams repeat-failure on " + base);
                    }
                    try {
                        Throwable repeatResult = recordCounts(
                                pendingRepeatFailures, failCount, skipCount);
                        allRepetitionsCompleted = lifecycleFacade
                                .closeExecutionScope(base, repeatResult);
                    } finally {
                        recordedEvents.clear();
                        reportedRepetitions.add(relayRepetitionEvents(
                                notifier, base, pendingRepeatFailures));
                    }
                    /* Try grab a new inner-most runner,
                     * in order to possibly bypass some heavy setup execution ... */
                    Runner innerRunner = FilteredRunnerAdvice
                            .repeatLeafRunner.get(repeatNotifier);
                    if (null != innerRunner) {
                        repeatRunner = innerRunner;
                    }
                }
            } catch (ContextLifecycleProviderFacade.MaxRepeatCount maxReached) {
                repeatProblem = maxReached;
            } catch (VirtualMachineError dontHandleThis) {
                throw dontHandleThis;
            } catch (Throwable unknownRepeatProblem) {
                repeatProblem = unknownRepeatProblem;
                if (false == recordedEvents.isEmpty()) {
                    /*Close again - because this seems to originate from repeater runner:*/
                    try {
                        lifecycleFacade.closeExecutionScope(base, null);
                    } catch (ContextLifecycleProviderFacade.MaxRepeatCount _ignore_) {}
                }
            }
            if (null == repeatProblem && 1 <= failCount.intValue()) {
                repeatProblem = new Summary(
                        failCount, skipCount, reportedRepetitions.size());
            }
            if (null != repeatProblem) {
                notifier.fireTestFailure(new Failure(base, repeatProblem));
            } else if (1 <= skipCount.intValue()) {
                notifier.fireTestAssumptionFailed(new Failure(base, new Summary(
                        failCount, skipCount, reportedRepetitions.size())));
            }
            return true;
        }

        RepeatNotifier newRepeatNotifier(final Description base,
                final Map<Throwable,ProvideJunitVintage> repeatFailures,
                final Collection<ProvideJunitVintage> eventsRecord) {
            return new RepeatNotifier(RepeatDescription.classOf(base)) {
                void recordFailure(Failure failure, ProvideJunitVintage eventType) {
                    if (false == eventsRecord.isEmpty()
                            && base.equals(failure.getDescription())) {
                        eventsRecord.add(eventType);
                        repeatFailures.put(failure.getException(), eventType);
                    }
                }

                @Override public void fireTestStarted(Description description)
                throws StoppedByUserException {
                    if (base.equals(description)) {
                        if (eventsRecord.add(fireTestStarted)) {
                            lifecycleFacade.openExecutionScope(base);
                        } else if (eventsRecord.contains(fireTestFinished)) {
                            throw new StoppedByUserException();
                        }
                    }
                }
                @Override public void fireTestAssumptionFailed(Failure failure) {
                    recordFailure(failure, fireTestAssumptionFailed);
                }
                @Override public void fireTestFailure(Failure failure) {
                    recordFailure(failure, fireTestFailure);
                }
                @Override public void fireTestFinished(Description description) {
                    if (false == eventsRecord.isEmpty() && base.equals(description)) {
                        eventsRecord.add(fireTestFinished);
                    }
                }

                @Override public void addFirstListener(RunListener listener) {/*Disabled*/}
                @Override public void fireTestIgnored(Description description) {/*Disabled*/}
                @Override public void fireTestSuiteFinished(Description description) {/*Disabled*/}
                @Override public void fireTestSuiteStarted(Description description) {/*Disabled*/}
                @Override public void fireTestRunFinished(Result result) {/*Disabled*/}
                @Override public void fireTestRunStarted(Description description) {/*Disabled*/}
                @Override public void removeListener(RunListener listener) {/*Disabled*/}
                @Override public void addListener(RunListener listener) {/*Disabled*/}
            };
        }
    };

    private static final ContextLifecycleProviderFacade<Description> lifecycleFacade =
            new ContextLifecycleProviderFacade<Description>();

    private static final Map<Description,Map<Throwable,ProvideJunitVintage>>
            pendingFailuresToReport = new WeakHashMap<Description, Map<Throwable, ProvideJunitVintage>>();

    /**
     * There are JUnit runners that don't hold on to the {@link Description} of
     * a test during its execution but instead creates a new {@link Description}
     * instance for each event being fired. LazyParams identifies a test execution
     * scope by its {@link Description} instance, so it will not work to
     * locate an existing scope with a new {@link Description} instance.
     * Also the LazyParams scope context is referenced by a weakly referenced
     * Description, in order to not prevent garbage collection if test goes
     * wrong somehow. Therefore the {@link Description} that is used to reference
     * LazyParams scope is kept on this thread-local for safe-keeping until
     * a fireTestFinished event is fired, so we don't risk the scope reference
     * {@link Description} and its LazyParams scope context being garbage
     * collected in midair as the JUnit runner might drop its {@link Description}
     * instance between events from an ongoing test-execution.
     * This safe-keeping of the test-identifier {@link Description} is also
     * what allows us to use a {@link WeakHashMap} for
     * {@link #pendingFailuresToReport}.
     *
     * @see #pendingFailuresToReport
     * @see #lifecycleFacade
     * @see #fireTestFinished
     */
    private static final ThreadLocal<Description> testIdentifierBackup =
            new ThreadLocal<Description>();

    private static Description relayRepetitionEvents(RunNotifier notifier,
            Description baseDescription, Map<Throwable,ProvideJunitVintage> failures)
    throws StoppedByUserException {
        Description repeatDescription =
                RepeatDescription.forRepetitionOf(baseDescription);
        notifier.fireTestStarted(repeatDescription);
        for (Map.Entry<Throwable,ProvideJunitVintage> eachFail : failures.entrySet()) {
            Failure failure = new Failure(repeatDescription, eachFail.getKey());
            switch (eachFail.getValue()) {
                case fireTestAssumptionFailed:
                    notifier.fireTestAssumptionFailed(failure);
                    break;
                case fireTestFailure:
                    notifier.fireTestFailure(failure);
                    break;
                default:
                    throw new IllegalStateException(
                            "Unexpected failure event-type: " + eachFail.getValue());
            }
        }
        notifier.fireTestFinished(repeatDescription);
        return repeatDescription;
    }

    /**
     * Default implementation requires a non-null {@link Failure} argument!
     * This method is overidden by the event-types that don't have any
     * {@link Failure} parameter.
     */
    boolean handleInterception(
            RunNotifier notifier, Description base, Failure failure) {

        Throwable thrown = failure.getException();
        if (thrown instanceof ContextLifecycleProviderFacade.MaxRepeatCount
                || thrown instanceof Summary
                || thrown instanceof StoppedByUserException
                || thrown instanceof VirtualMachineError) {
            return true;
        }

        Map<Throwable,ProvideJunitVintage> pendingRepeatFailures =
                pendingFailuresToReport.get(base);
        if (null == pendingRepeatFailures) {
            try {
                if (lifecycleFacade.closeExecutionScope(base,
                        fireTestAssumptionFailed == this ? null : thrown)) {
                    return true;
                }
            } catch (ContextLifecycleProviderFacade.MaxRepeatCount maxReached) {
                try {
                    relayRepetitionEvents(notifier, base,
                            Collections.singletonMap(thrown, this));
                } catch (Exception justReportOriginalFailureInstead___) {
                    return true;
                }
                notifier.fireTestFailure(new Failure(base, maxReached));
                return false;
            }
            pendingRepeatFailures = new LinkedHashMap<Throwable,ProvideJunitVintage>();
            pendingFailuresToReport.put(base, pendingRepeatFailures);
        }
        pendingRepeatFailures.put(thrown, this);
        return false;
    }

    public static class NotifierAdvice extends AdviceFor<RunNotifier> {
        public NotifierAdvice() {
            on().fireTestStarted(null);
            on().fireTestFailure(null);
            on().fireTestFinished(null);
            try {
                on().fireTestSuiteFinished(null);
                on().fireTestAssumptionFailed(null);
            } catch (Error perhapsAnEarlyVersionOfJunit) {}
        }

        public static Description resolveScopeReferenceFor(
                Description currentEventDescription) {
            Description currentTestIdentifier = testIdentifierBackup.get();
            if (currentEventDescription.equals(currentTestIdentifier)) {
                return/*established scope reference*/currentTestIdentifier;
            } else {
                testIdentifierBackup.set(currentEventDescription);
                return/*pending scope reference*/currentEventDescription;
            }
        }

        public static boolean isRepeatDescription(Description d) {
            return null != d.getAnnotation(RepeatDescription.class);
        }

        @Advice.OnMethodEnter(inline = true, skipOn = Advice.OnDefaultValue.class)
        @SuppressWarnings({"unused","UnusedAssignment"/*because of how Byte Buddy works!*/})
        private static boolean fireNotifierEvent(
                @Advice.Origin("#m"/*to capture method-name!*/) String notifyEvent,
                @Advice.This RunNotifier notifier,
                @Advice.Argument(value = 0, readOnly = false, typing = Assigner.Typing.DYNAMIC)
                        Object notifyData) {
//            System.out.println("INTERCEPTED " + notifyEvent + " on " + notifyData);
            ProvideJunitVintage operation;
            try {
                operation = ProvideJunitVintage.valueOf(notifyEvent);
            } catch (Exception ex) {
                ex.printStackTrace();
                System.err.println("Intercepted an event without "
                        + ProvideJunitVintage.class.getSimpleName() + " constant!");
                return true;
            }
            Failure failure = notifyData instanceof Failure
                    ? (Failure)notifyData      : null;
            Description description = null != failure
                    ? failure.getDescription() : (Description) notifyData;
            if (null == description || isRepeatDescription(description)) {
                return true;
            }

            description = resolveScopeReferenceFor(description);
            /* Make sure description is reused on an individual test: */
            if (null == failure) {
                notifyData = description;
            } else if (failure.getDescription() != description) {
                notifyData = failure = new Failure(
                        description, failure.getException());
            }

            return handleInterception(operation, notifier, description, failure);
        }

        public static boolean handleInterception(ProvideJunitVintage operation,
                RunNotifier notifier, Description description, Failure failure) {
            return operation.handleInterception(notifier, description, failure);
        }
    }

    public static class ParentRunnerAdvice extends AdviceFor<Filterable> {
        private static final Method childrenInvoker = findNotifierMethod("childrenInvoker");

        ParentRunnerAdvice() throws NoSuchMethodException {
            super(ParentRunner.class);
            if (null != childrenInvoker) {
                on(ParentRunner.class.getDeclaredMethod("classBlock", RunNotifier.class));
                try {
                    on().filter(null);
                } catch (NoTestsRemainException _ignore_) {}
            } else {
                throw new NoSuchMethodException("Cannot instrument ParentRunner,"
                        + " because #childrenInvoker(RunNotifier) was not found!");
            }
        }

        private static Method findNotifierMethod(String methodName) {
            try {
                return ParentRunner.class
                        .getDeclaredMethod(methodName, RunNotifier.class);
            } catch (Throwable noSuchMethod) {
                System.err.println(noSuchMethod.getMessage());
                return null;
            }
        }

        @Advice.OnMethodEnter(inline = false, skipOn = Object.class)
        public static Object overrideClassBlockIfRepeatRunNotifierOnThisClass(
                @Advice.This ParentRunner thisRunner,
                @Advice.Argument(value = 0,typing = Assigner.Typing.DYNAMIC)
                        Object runNotifierOrFilter)
        throws Throwable {
            FilteredRunnerAdvice.record(thisRunner, runNotifierOrFilter);
            if (false == runNotifierOrFilter instanceof RepeatNotifier) {
                return null; // Dont override!
            }
            RepeatNotifier repeatNotifier = (RepeatNotifier) runNotifierOrFilter;
            if (repeatNotifier.isOnRepeat(thisRunner.getTestClass().getJavaClass())) {
                childrenInvoker.setAccessible(true);
                try {
                    return childrenInvoker.invoke(thisRunner, repeatNotifier);
                } catch (InvocationTargetException ex) {
                    throw ex.getTargetException();
                }
            } else {
                return null;//Unusual but possible if runner operates with other classloader
            }
        }
        @Advice.OnMethodExit(inline = true)
        private static void setReturnValue(
                @Advice.Return(typing = Assigner.Typing.DYNAMIC, readOnly = false)
                Object finalReturnValue,
                @Advice.Enter Object replacementReturnValue) {
            if (null != replacementReturnValue) {
//                System.out.println("CHANGING RETURN VALUE from " + finalReturnValue);
                finalReturnValue = replacementReturnValue;
            } else {
//                System.out.println("Keep original return value!");
            }
        }
    }

    public static class ClassRoadieAdvice extends AdviceFor<Object> {
        ClassRoadieAdvice() throws NoSuchMethodException {
            super(ClassRoadie.class);
            on(ClassRoadie.class.getDeclaredMethod("runProtected"));
        }

        @Advice.OnMethodEnter(inline = false, skipOn = Advice.OnDefaultValue.class)
        public static boolean runProtected(@Advice.This ClassRoadie thisRoadie) {
            RepeatNotifier repeatNotifier = null;
            TestClass testClass = null;
            Runnable runnable = null;
            for (Field field : ClassRoadie.class.getDeclaredFields()) {
                field.setAccessible(true);
                Object fieldValue;
                try {
                    fieldValue = field.get(thisRoadie);
                } catch (Exception ex) {
                    System.err.println(ex.getMessage());
                    continue;
                }
                if (fieldValue instanceof RepeatNotifier) {
                    repeatNotifier = (RepeatNotifier) fieldValue;
                } else if (fieldValue instanceof TestClass) {
                    testClass = (TestClass) fieldValue;
                } else if (fieldValue instanceof Runnable) {
                    runnable = (Runnable) fieldValue;
                }
            }
            if (null == repeatNotifier || null == runnable || null == testClass
                    || false == repeatNotifier.isOnRepeat(testClass.getJavaClass())) {
                return true;
            } else {
                runnable.run();
                return false;
            }
        }
    }

    public static class FilteredRunnerAdvice extends AdviceFor<Object> {
        static final Method recordOnJunitClassloader = new Object() {
            Method locateRecordOnRightClassloader() {
                Class<?> constructorAdviceClassOnJunitClassloader;
                try {
                    constructorAdviceClassOnJunitClassloader = Class.forName(
                            FilteredRunnerAdvice.class.getName(),
                            false,
                            Runner.class.getClassLoader());
                } catch (ClassNotFoundException shouldNeverHappen) {
                    /*Pretend we are on the right classloader anyway: */
                    shouldNeverHappen.printStackTrace();
                    return null;
                }
                if (FilteredRunnerAdvice.class
                        == constructorAdviceClassOnJunitClassloader) {
                    /* We are on the right classloader! */
                    return null;

                } else {
                    try {
                        /* This is the wrong classloader!
                        * Locate class instance for this class: */
                        Class<?> thisClassOnRightClassLoader = Class.forName(
                                FilteredRunnerAdvice.class.getName(), false,
                                Runner.class.getClassLoader());
                        return thisClassOnRightClassLoader.getMethod(
                                "record", Runner.class, Object.class);
                    } catch (Exception shouldNeverHappen) {
                        shouldNeverHappen.printStackTrace();
                        return null;
                    }
                }
            }
        }.locateRecordOnRightClassloader();
        static final WeakIdentityHashMap<Runner,Filter> filterings = newWeakMap();
        static final WeakIdentityHashMap<RunNotifier,Runner> repeatLeafRunner = newWeakMap();

        private static <K,V> WeakIdentityHashMap<K,V> newWeakMap() {
            return null == recordOnJunitClassloader ? new WeakIdentityHashMap<K,V>() : null;
        }

        FilteredRunnerAdvice() throws NoSuchMethodException {
            on(Runner.class.getMethod("run", RunNotifier.class));
            try {
                on(Filterable.class.getMethod("filter", Filter.class));
            } catch (Exception probablyBrokenBecauseOfObsoleteJunitVersion_ButTryingAnyway) {
                System.err.println(
                        probablyBrokenBecauseOfObsoleteJunitVersion_ButTryingAnyway.getMessage());
            }
        }

        @Override
        protected ElementMatcher.Junction<? super TypeDescription> definitionsToInstrument() {
            return super.definitionsToInstrument()
                    .and(ElementMatchers.not(ElementMatchers.isSubTypeOf(ParentRunner.class)))
                    .and(ElementMatchers.isSubTypeOf(Runner.class));
        }

        @Advice.OnMethodEnter(inline = false)
        public static void record(
                @Advice.This Runner thisRunner,
                @Advice.Argument(value=0,typing=Assigner.Typing.DYNAMIC) Object arg)
        throws Throwable {
            if (null != recordOnJunitClassloader) {
                try {
                    recordOnJunitClassloader.invoke(null, thisRunner, arg);
                } catch (InvocationTargetException ex) {
                    throw ex.getTargetException();
                }
                return;

            } else if (arg instanceof Filter) {
                filterings.put(thisRunner, (Filter)arg);
 
            } else if (referencesRepeatNotifier(arg)) {
                RunNotifier notifier = (RunNotifier) arg;
                Runner outerRunner = repeatLeafRunner.get(notifier);
                if (outerRunner != thisRunner) {
                    repeatLeafRunner.put(notifier, thisRunner);
                    if (null != outerRunner && false == filterings.containsKey(thisRunner)) {
                        Filter filter = filterings.get(outerRunner);
                        if (null != filter) {
                            if (thisRunner instanceof Filterable) {
                                ((Filterable)thisRunner).filter(filter);
                            } else {
                                filterings.put(thisRunner, filter);
                            }
                        }
                    }
                }
            }
        }

        private static boolean referencesRepeatNotifier(Object arg) {
            if (RunNotifier.class == arg.getClass()
                    || false == arg instanceof RunNotifier) {
                return false;
            } else if (arg instanceof RepeatNotifier
                    || repeatLeafRunner.containsKey((RunNotifier) arg)) {
                return true;
            }

            /* Special case - with tweaked notifier: */
            for (Class<?> c = arg.getClass();
                    RunNotifier.class != c; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (false == Modifier.isStatic(f.getModifiers())
                            && false == f.getType().isPrimitive()) {
                        f.setAccessible(true);
                        try {
                            Object v = f.get(arg);
                            if (v instanceof RunNotifier) {
                                Runner outer = repeatLeafRunner.get((RunNotifier)v);
                                if (null != outer) {
                                    repeatLeafRunner.put((RunNotifier) arg, outer);
                                    return true;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
            return false;
        }
    }

    private enum RepeatDescription implements Annotation {
        INSTANCE;

        @Override public Class<? extends Annotation> annotationType() {
            return RepeatDescription.class;
        }

        static Description forRepetitionOf(Description baseDescription) {
            CharSequence appendix = lifecycleFacade
                    .resolveDisplayAppendixForScope(baseDescription);
//            Description repeatDescription = Description.createTestDescription(
//                    classOf(baseDescription),
//                    methodNameOf(baseDescription) + (null == appendix ? "" : appendix),
//                    composeRepeatAnnotationsFor(baseDescription));
            /*
             * Because of what seems to be an issue on Maven Surefire, we here create
             * repeat description with method Description#createSuiteDescription(...)
             * instead of the out-commented alternative above, which uses the
             * seemingly more natural method Description#createTestDescription(...),
             * given that we do indeed have a test-class here - and we are about to
             * report result on a singular test-execution - not on a test suite with
             * multiple tests.
             */
            Description repeatDescription = Description.createSuiteDescription(
                    methodNameOf(baseDescription) + (null == appendix ? "" : appendix),
                    composeRepeatAnnotationsFor(baseDescription));
            baseDescription.addChild(repeatDescription);
            return repeatDescription;
        }

        private static Annotation[] composeRepeatAnnotationsFor(Description baseDesc) {
            try {
                Collection<Annotation> baseAnnotations = baseDesc.getAnnotations();
                Annotation[] repeatAnnotations = new Annotation[baseAnnotations.size() + 1];
                baseAnnotations.toArray(repeatAnnotations);
                repeatAnnotations[repeatAnnotations.length - 1] = INSTANCE;
                return repeatAnnotations;
            } catch (Exception justInCase_butShouldNeverHappen) {
                return new Annotation[] {INSTANCE};
            }
        }

        static Class<?> classOf(Description desc) {
            try {
                return desc.getTestClass();
            } catch (Error earlyJunitVersion) {
                String displayName = desc.getDisplayName();
                String className = displayName.substring(
                        displayName.lastIndexOf('(') + 1,
                        displayName.lastIndexOf(')'));
                try {
                    return Class.forName(className, false,
                            RepeatDescription.class.getClassLoader());
                } catch (ClassNotFoundException discard) {
                    System.err.println(discard.getMessage());
                    throw earlyJunitVersion;
                }
            }
        }

        private static String methodNameOf(Description desc) {
            try {
                return desc.getMethodName();
            } catch (Error earlyJunitVersion) {
                String displayName = desc.getDisplayName();
                return displayName.substring(0, displayName.lastIndexOf('('));
            }
        }
    }
    private static final class Summary extends AssertionError {
        Summary(Number failCount, Number skipCount, int totalCount) {
            super(summarize(failCount.intValue(), skipCount.intValue())
                    + " (total " + totalCount + ")");
        }

        static String summarize(int failCount, int skipCount) {
            if (2 <= failCount) {
                return failCount + " tests failed";
            } else if (1 == failCount) {
                return "1 test failed";
            } else if (2 <= skipCount) {
                return skipCount + " tests skipped";
            } else if (1 == skipCount) {
                return "1 test skipped";
            } else {
                return "";
            }
        }
    }
    public static abstract class RepeatNotifier extends RunNotifier {
        private final Class<?> testClassonRepeat;

        public RepeatNotifier(Class<?> testClassonRepeat) {
            this.testClassonRepeat = testClassonRepeat;
        }

        boolean isOnRepeat(Class<?> testClass) {
            return testClassonRepeat == testClass;
        }
    }
}
