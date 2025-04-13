/*
 * Copyright 2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.internal;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.hierarchical.ThrowableCollector;

/**
 * Used by
 * {@link ProvideJunitPlatformHierarchical.NodeTestTaskAdvice#suspendCleanUp(HierarchicalTestExecutorService.TestTask) }
 * to intercept clean-up {@link Executable } during
 * {@link org.junit.platform.engine.support.hierarchical.NodeTestTask#cleanUp() }
 * to have the execution postponed until repetitions have completed.
 *
 * @see ProvideJunitPlatformHierarchical.NodeTestTaskAdvice#suspendCleanUp(HierarchicalTestExecutorService.TestTask)
 * @see org.junit.platform.engine.support.hierarchical.NodeTestTask#cleanUp()
 */
class ThrowableCollectorInterception extends ThrowableCollector {

    private static final Predicate DUMMY_PREDICATE_TO_PLEASE_SUPERCLASS_CONSTRUCTOR =
            new Predicate() { @Override public boolean test(Object o) { return false; }};

    private UnaryOperator<ThrowableCollector> collectorSwitcher;
    private UnaryOperator<Executable> interception;

    private final ThrowableCollector coreCollector;

    private ThrowableCollectorInterception(
            UnaryOperator<ThrowableCollector> collectorSwitcher,
            UnaryOperator<Executable> interception) {
        super(DUMMY_PREDICATE_TO_PLEASE_SUPERCLASS_CONSTRUCTOR);
        this.collectorSwitcher = collectorSwitcher;
        this.interception = interception;

        coreCollector = collectorSwitcher.apply(this);
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    static void setup(
            UnaryOperator<ThrowableCollector> collectorSwitcher,
            UnaryOperator<Executable> interception) {
        new ThrowableCollectorInterception(collectorSwitcher, interception);
    }

    @Override
    public void execute(Executable executable) {
        UnaryOperator<Executable> interceptionGhost = this.interception;
        this.interception = null;
        if (null != interceptionGhost) {
            executable = interceptionGhost.apply(executable);
        }

        UnaryOperator<ThrowableCollector> collectorSwitcherGhost = this.collectorSwitcher;
        if (null != collectorSwitcherGhost) {
            collectorSwitcherGhost.apply(coreCollector);
            this.collectorSwitcher = null;
        }

        coreCollector.execute(executable);
    }

    @Override public TestExecutionResult toTestExecutionResult()
            { return coreCollector.toTestExecutionResult(); }
    @Override public      void assertEmpty() { coreCollector.assertEmpty(); }
    @Override public   boolean isNotEmpty()  { return coreCollector.isNotEmpty(); }
    @Override public   boolean isEmpty()     { return coreCollector.isEmpty(); }
    @Override public Throwable getThrowable() { return coreCollector.getThrowable(); }
}
