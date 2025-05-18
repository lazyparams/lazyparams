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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.hierarchical.EngineExecutionContext;
import org.junit.platform.engine.support.hierarchical.Node;
import org.junit.platform.engine.support.hierarchical.ThrowableCollector;
import org.lazyparams.LazyParams;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * @see HierarchicalLifecycleTest
 * @author Henrik Kaipe
 */
public class NodeDescriptor extends AbstractTestDescriptor
implements Node<EngineExecutionContext> {

    enum EventExecutionPhase {
        NONE, BEFORE, DURING, AFTER;
        StringBuilder capitalize() {
            return new StringBuilder()
                    .append(name().charAt(0))
                    .append(name().substring(1).toLowerCase());
        }
        @Override public String toString() { return " " + name() + " execution"; }
    }

    class InconsistencyCheck extends Error { InconsistencyCheck(String issue) {
        super("On " + getDisplayName() + " " + issue);
        if (null != issue) { throw this; }
    }}

    static final ThrowableCollector.Factory THROWABLE_COLLECTOR_FACTORY =
            () -> new ThrowableCollector(t -> false) {
        @Override public void execute(ThrowableCollector.Executable executable) {
            try {
                executable.execute();
            } catch (InconsistencyCheck inconsistency) {
                throw inconsistency;
            } catch (Throwable failure) {
                super.execute(() -> { throw failure; });
            }
        }
    };

    EventExecutionPhase parameterIntroduction, failure;
    { parameterIntroduction = failure = EventExecutionPhase.NONE; }
    private int prepareCount = 0, cleanUpCount = 0;
    private boolean pendingParameterValues = false;
    private boolean parentParameterized = false;
    boolean outsideExecutionWindow = true;

    private final Set<NodeDescriptor> dynamics = new LinkedHashSet<>();

    String[] expectedDisplayNamesWithParamValues(String coreName) {
        switch(parameterIntroduction) {
            case NONE: return new String[] {null};
            default: return new String[] {
                coreName + ' ' + getDisplayName() + "=37",
                coreName + ' ' + getDisplayName() + "=38"
            };
        }
    }

    public NodeDescriptor(UniqueId uniqueId, String displayName) {
        super(uniqueId, displayName);
    }

    void addDynamic(NodeDescriptor dynamicChild) {
        assertTrue(dynamics.add(dynamicChild));
    }

    private EventExecutionPhase pickExemode(IntUnaryOperator seeds) {
        switch (seeds.applyAsInt(3)) {
            case 0: return EventExecutionPhase.BEFORE;
            case 1: return EventExecutionPhase.DURING;
            case 2: return EventExecutionPhase.AFTER;
            default: throw new Error("Must never happen!");
        }
    }

    CharSequence paramize(IntUnaryOperator seeds) {
        return (parameterIntroduction = pickExemode(seeds))
                .capitalize().append(getDisplayName());
    }
    CharSequence fail(IntUnaryOperator seeds) {
        failure = (false == getChildren().isEmpty()
                || false == dynamics.isEmpty())
                ? EventExecutionPhase.AFTER
                : (EventExecutionPhase.DURING == parameterIntroduction
                || 0 == seeds.applyAsInt(2))
                ? EventExecutionPhase.DURING
                : EventExecutionPhase.BEFORE;
        return failure.capitalize().append(getDisplayName());
    }

    void forEachChild(Consumer<NodeDescriptor> apply) {
        dynamics.forEach(apply);
        getChildren().stream().map(NodeDescriptor.class::cast).forEach(apply);
    }

    @Override public Type getType() {
        return isRoot() ? Type.CONTAINER
                : children.isEmpty() ? Type.TEST
                : Type.CONTAINER_AND_TEST;
    }

    void doEventsOn(final EventExecutionPhase currentPhase) {
        new InconsistencyCheck(prepareCount < 1 ? "not prepared" + currentPhase
                : outsideExecutionWindow
                        ? "but not cleared for execution by parent " + getParent()
                : null);
        if (currentPhase == parameterIntroduction) {
            pendingParameterValues =
                    37 == LazyParams.pickValue(getDisplayName(), 37,38);
        }
        assertNotSame(getDisplayName(), currentPhase, failure);
    }

    @Override public EngineExecutionContext prepare(EngineExecutionContext context) {
        new InconsistencyCheck(cleanUpCount < prepareCount
                ? "was prepared multiple times without being cleaned up between"
                : null);
        ++prepareCount;
        doEventsOn(null);
        return context;
    }
    @Override public EngineExecutionContext before(EngineExecutionContext context) {
        doEventsOn(EventExecutionPhase.BEFORE);
        return context;
    }
    @Override public EngineExecutionContext execute(
            EngineExecutionContext context,
            DynamicTestExecutor dynamicTestExecutor) {
        doEventsOn(EventExecutionPhase.DURING);
        forEachChild(child -> {
            child.outsideExecutionWindow = false;
            child.parentParameterized |=
                    pendingParameterValues || parentParameterized;
        });
        dynamics.forEach(dynamicTestExecutor::execute);
        return context;
    }
    @Override public void after(EngineExecutionContext context) {
        List<String> childrenIssues = new ArrayList<String>() {
            @Override public boolean contains(Object o) {
                NodeDescriptor child = (NodeDescriptor) o;
                if (false == child.outsideExecutionWindow) {
                    add(child + " was not executed!");
                } else if (child.pendingParameterValues) {
                    add(child + " repetitions not completed!");
                } else if (child.parentParameterized) {
                    if (child.prepareCount <= child.cleanUpCount) {
                        add(child + " should not have been cleaned up,"
                                + " because of pending parent paramerization");
                    }
                } else if (child.cleanUpCount < child.prepareCount) {
                    add(child + " should have been cleaned up");
                }
                return false == children.isEmpty();
            }
        };
        forEachChild(childrenIssues::contains);
        new InconsistencyCheck(childrenIssues.stream()
                .map(line -> line + "\n")
                .reduce(String::concat).orElse(null));
        if (pendingParameterValues) {
            /*
             * This is a tricky situation, for which cleanup has been
             * suspended on children. But what about prepare??
             * In an ideal world it would make sense to make sure
             * child#prepare is not repeated when execution returns to
             * each child as part of parent node repetition. But for now
             * this has not been implemented and therefore this test
             * will assume repeated preparation when parent repetition
             * makes execution return to child.
             */
            forEachChild(new Consumer<NodeDescriptor>() {
                @Override public void accept(NodeDescriptor eachChild) {
                    eachChild.cleanUpCount = prepareCount;
                    eachChild.forEachChild(this);
                }
            });
        } else if (false == parentParameterized) {
            new Consumer<NodeDescriptor>() {
                @Override public void accept(NodeDescriptor nodeInHierarchy) {
                    nodeInHierarchy.parentParameterized = false;
                    nodeInHierarchy.forEachChild(this);
                }
            }.accept(this);
        }
        try {
            doEventsOn(EventExecutionPhase.AFTER);
        } finally {
            outsideExecutionWindow = false == pendingParameterValues;
        }
    }
    @Override public void cleanUp(EngineExecutionContext context) {
        new InconsistencyCheck(pendingParameterValues
                ? "must not close when having pending param values!"
                : parentParameterized
                ? "must not close when there are pending param values from parent!"
                : prepareCount <= 0 ? "has not been prepared!"
                : outsideExecutionWindow ? null
                : "has not had its execution window closed!");
        forEachChild(child -> new InconsistencyCheck(
                child.cleanUpCount < child.prepareCount
                ? " child " + child + " there was no cleanup!"
                : null));
        cleanUpCount = prepareCount;
    }

    @Override public String toString() { return getDisplayName(); }
}
