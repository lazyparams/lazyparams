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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.platform.engine.CancellationToken;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.EngineExecutionContext;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;
import org.junit.platform.engine.support.hierarchical.ThrowableCollector;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.model.Statement;

import org.mockito.invocation.InvocationOnMock;
import org.lazyparams.LazyParams;
import org.lazyparams.core.Lazer;
import org.lazyparams.demo.ParameterizedTree;
import org.lazyparams.showcase.ToList;

import static java.lang.Character.*;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.*;

/**
 * Craft low-level scenarios on {@link ProvideJunitPlatformHierarchical}
 * @author Henrik Kaipe
 */
@RunWith(Parameterized.class)
public class HierarchicalLifecycleTest {

    enum ListenerEventType {dynamicTestRegistered, executionStarted, executionFinished;
        static final List<ListenerEventType> toStartDynamicTest = Collections
                .unmodifiableList(Arrays.asList(dynamicTestRegistered,executionStarted));
        TestExecutionResult verifyNext(
                String expectedDisplayName, ListIterator<ListenerEvent> feed) {
            String expected = this + " on " + expectedDisplayName;
            int nextIndex = feed.nextIndex();
            assertTrue(expected + " demands event #" + nextIndex, feed.hasNext());
            ListenerEvent event = feed.next();
            assertEquals("Invocation #" + nextIndex, (Object)expected,
                    event.eventType + " on " + event.displayName);
            return event.result;
        }
    }
    static class ListenerEvent {
        private final ListenerEventType eventType;
        private final String displayName;
        private final TestExecutionResult result;

        ListenerEvent(InvocationOnMock event) {
            this.eventType = ListenerEventType.valueOf(event.getMethod().getName());
            this.displayName = event.<TestDescriptor>getArgument(0).getDisplayName();
            this.result = ListenerEventType.executionFinished == this.eventType
                    ? event.getArgument(1) : null;
        }

        @Override
        public String toString() {
            return eventType + " on " + displayName;
        }
    }

    static final UniqueId rootId = UniqueId.root(HierarchicalLifecycleTest.class.getName(),
            ProvideJunitPlatformHierarchical.class.getSimpleName());
    final NodeDescriptor rootDescriptor = newNodeDescriptor("ROOT");

    final List<ListenerEvent> coreListenerEvents = new ArrayList<ListenerEvent>(64);
    final EngineExecutionListener coreListener = mock(
            EngineExecutionListener.class,
            withSettings().stubOnly().defaultAnswer(inv -> {
        try {
            coreListenerEvents.add(new ListenerEvent(inv));
            return null;
        } catch(Throwable x) {
            System.out.println(x.getMessage());
            throw newNodeDescriptor("bad invocation")
                    .new InconsistencyCheck(x.getMessage());
        }
    }));

    @Rule public final TestRule prependHierarchyOnFailure;

    public HierarchicalLifecycleTest(
            CharSequence displayName,
            List<NodeDescriptor> rootChildren,
            final CharSequence hierarchy,
            CharSequence withParametersThatAreAlreadyPrepared,
            CharSequence withFailuresThatAreAlreadyPrepared) {
        rootChildren.forEach(rootDescriptor::addChild);
        this.prependHierarchyOnFailure = (stmt,desc) -> new Statement() {
            @Override public void evaluate() throws Throwable {
                try {
                    stmt.evaluate();
                } catch (Error core) {
                    AssertionError prepended = new AssertionError(
                            hierarchy + "\n" + core.getMessage());
                    prepended.setStackTrace(core.getStackTrace());
                    throw prepended;
                }
            }
        };
    }

    void executeOnHierarchicalEngine() {
        ExecutionRequest engineExecRequest =
                mock(ExecutionRequest.class, RETURNS_MOCKS);
        when(engineExecRequest.getRootTestDescriptor()).thenReturn(rootDescriptor);
        when(engineExecRequest.getEngineExecutionListener()).thenReturn(coreListener);
        try {
            /* JUnit-6 introduces a new property "cancellationToken",
             * which needs to be stubbed: */
            when(engineExecRequest.getCancellationToken())
                    .thenReturn(CancellationToken.create());
        } catch (Error isExpectedOnTestProfileThatVerifiesSupportForJunit5) {}
        new HierarchicalTestEngine<EngineExecutionContext>() {
            @Override
            protected ThrowableCollector.Factory createThrowableCollectorFactory(
                    ExecutionRequest request) {
                return NodeDescriptor.THROWABLE_COLLECTOR_FACTORY;
            }
            @Override
            protected EngineExecutionContext createExecutionContext(ExecutionRequest request) {
                return new EngineExecutionContext() {};
            }
            @Override
            public String getId() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            @Override
            public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }.execute(engineExecRequest);
    }

    void verifyCoreListenerEvents(ListIterator<ListenerEvent> events,
            NodeDescriptor node, String coreName) {
        ListenerEventType.executionStarted.verifyNext(coreName, events);
        int parentParamsStart = coreName.indexOf(' ');
        String lateSlashSuffix = 0 < parentParamsStart
                && NodeDescriptor.EventExecutionPhase.NONE != node.parameterIntroduction
                ? " /" : "";
        TestExecutionResult.Status expectedResult =
                NodeDescriptor.EventExecutionPhase.NONE == node.failure
                ? TestExecutionResult.Status.SUCCESSFUL
                : TestExecutionResult.Status.FAILED;
        for (String paramizedName : node
                .expectedDisplayNamesWithParamValues(coreName + lateSlashSuffix)) {
            if (null != paramizedName) {
                ListenerEventType.toStartDynamicTest.forEach(
                        eventType -> eventType.verifyNext(paramizedName, events));
            }
            node.forEachChild(child -> {
                String childCoreName = child.getDisplayName();
                switch (node.parameterIntroduction) {
                    case BEFORE: case DURING:
                        childCoreName += paramizedName
                                .substring(paramizedName.indexOf(' '));
                    break;
                    default:
                        if (0 < parentParamsStart) {
                            childCoreName += coreName
                                    .substring(parentParamsStart);
                        }
                    break;
                }
                switch (child.parameterIntroduction) {
                    case NONE:
                        if (0 < childCoreName.indexOf(' ')) {
                            childCoreName += " /";
                        }
                    break;
                    default:/*NOOP*/
                }
                if (false == childCoreName.equals(child.getDisplayName())
                        || false == node.getChildren().contains(child)) {
                    ListenerEventType.dynamicTestRegistered
                            .verifyNext(childCoreName, events);
                }
                verifyCoreListenerEvents(events, child, childCoreName);
            });
            if (null != paramizedName) {
                assertSame(paramizedName + " test result", expectedResult, ListenerEventType
                        .executionFinished.verifyNext(paramizedName, events).getStatus());
            }
        }
        assertSame(coreName.substring(0,coreName.length() - lateSlashSuffix.length())
                + " test result", expectedResult,
                ListenerEventType.executionFinished
                        .verifyNext(coreName, events).getStatus());
    }

    /**
     * There are some situations, for which the prepare/cleanUp lifecycle events
     * dont behave as desired. These are corner-case issues that should be fixed.
     * But until fixes are implemented their scenarios will instead be skipped.
     */
    @Before
    public void skipHierarchiesThatDontSupportFullLifecycle() {
        assumeNoParentIntroducesParametersAfter(rootDescriptor);
        assumeNoChildHasTwoParameterizedAncestors(rootDescriptor, 0);
    }
    private void assumeNoChildrenOn(NodeDescriptor node, String childrenDesc) {
        Collection<Object> allChildren = new ArrayList<Object>();
        node.forEachChild(allChildren::add);
        assumeThat(childrenDesc, allChildren, empty());
    }
    private void assumeNoParentIntroducesParametersAfter(NodeDescriptor node) {
        switch (node.parameterIntroduction) {
            default:
                /* Parameter introduction BEFORE or DURING - all good: */
                return;
            case NONE:
                /*Inconclusive - must expand navigation to children ...*/
                node.forEachChild(this::assumeNoParentIntroducesParametersAfter);
                return;
            case AFTER:
                assumeNoChildrenOn(node, "Children of " + node
                        + " that introduces parameterization AFTER");
        }
    }
    private void assumeNoChildHasTwoParameterizedAncestors(
            NodeDescriptor node, int parameterizedAncestorsCount) {
        switch (node.parameterIntroduction) {
            default:
                if (2 <= ++parameterizedAncestorsCount) {
                    assumeNoChildrenOn(node, "Childrean of " + node
                            + " that is second parameterized ancestor");
                    return;
                } else {
                    /* Execution continues below ... */
                }
            case NONE:
                final int finalCount = parameterizedAncestorsCount;
                node.forEachChild(child -> assumeNoChildHasTwoParameterizedAncestors(
                        child, finalCount));
        }
    }

    /**
     * Avoid confusion about potential parameter leakage from the underlying
     * JUnit5 test scenario to this actual core test (which is implemented on JUnit-4).
     */
    @After
    public void withHardwiredDeactivationOfLazyParamsOnThisTest() {
        /*
         * On the other hand, this leakage might provide some clues on
         * test failures, so consider uncommenting the line below
         * as part of debugging efforts.
         */
        LazyParams.currentScopeConfiguration().setMaxTotalCount(1);
    }

    @Test public void test() {
        rootDescriptor.outsideExecutionWindow = false;
        executeOnHierarchicalEngine();
        verifyCoreListenerEvents(
                coreListenerEvents.listIterator(), rootDescriptor, "ROOT");
    }

    private static NodeDescriptor newNodeDescriptor(String name) {
        return new NodeDescriptor(rootId.append("name", name),name);
    }

    /*******************************************************************
     * A selection of {@link NodeDescriptor} tree as test-data is composed below.
     * Each individual {@link NodeDescriptor} tree is tested above!
     *******************************************************************/
    @Parameterized.Parameters(name = "{0} {3} {4}")
    public static List<Object[]> hierarchies() throws Lazer.ExpectedParameterRepetition {
        List<Object[]> paramValues = new ArrayList<>();
        for (int nbrOfNodes : new int[] {1,2,3,4,8}) {
            combineNodeDescriptors(paramValues, nbrOfNodes);
        }
        /*
         * Uninstall here - to avoid having context from ParameterizedTree
         * leak to the actual test-execution ...
         */
        LazyParams.uninstall();
        return paramValues;
    }

    private static void combineNodeDescriptors(
            List<Object[]> paramValues, int nbrOfNodes)
    throws Lazer.ExpectedParameterRepetition {
        List<String> displayNames = Arrays.asList("A","B","C","D","E","F","G","H");
        final Lazer lazer = new Lazer();
        do {
            lazer.startNew();
            int paramize1stValue = lazer.pick("parameterized1st", true, 3);
            boolean thereWillBeFailures = 1 == lazer.pick("willThereBeFailures", true, 2);
            List<NodeDescriptor> rootChildren =
                    new ArrayList<NodeDescriptor>(Math.max(5, nbrOfNodes));
            List<StringBuilder> hierarchy =
                            new ParameterizedTree<NodeDescriptor>('A', displayNames
                    .subList(0, nbrOfNodes).stream()
                    .<NodeDescriptor>map(name -> newNodeDescriptor(name))
                    .toArray(NodeDescriptor[]::new)) {
                @Override
                protected boolean fallbackOnPreviousNode(Object paramId) {
                    return 1 == lazer.pick(paramId, true, 2);
                }
                @Override
                public boolean connect(NodeDescriptor parent, NodeDescriptor child) {
                    parent.addChild(child);
                    int hierarchyCountDown = 4;
                    while (0 < --hierarchyCountDown) {
                        if (parent.getParent()
                                .filter(NodeDescriptor.class::isInstance)
                                .isPresent()) {
                            parent = (NodeDescriptor) parent.getParent().get();
                        } else {
                            return true;
                        }
                    }
                    return false;
                }
            }.populate(nodeOnRoot -> {
                rootChildren.add(nodeOnRoot);
                return rootChildren.size() < 5;
            });
            rootChildren.forEach(new Consumer<NodeDescriptor>() {
                int dynamicCount = 0;
                @Override public void accept(NodeDescriptor node) {
                    NodeDescriptor[] nodeChildren = node.getChildren()
                            .stream().toArray(NodeDescriptor[]::new);
                    for (NodeDescriptor nodeChild : nodeChildren) {
                        accept(nodeChild);
                    }
                    for (NodeDescriptor nodeChild : nodeChildren) {
                        if (4 <= dynamicCount) return;
                        if (1 == lazer.pick("dynChild" + ++dynamicCount,
                                dynamicCount <= 3 + 1,
                                2)) {
                            node.removeChild(nodeChild);
                            node.addDynamic(nodeChild);
                            for (StringBuilder line : hierarchy) {
                                int index = line.indexOf(nodeChild.getDisplayName());
                                if (0 < index) {
                                    line.setCharAt(index, toLowerCase(line.charAt(index)));
                                    break;
                                }
                            }
                        } else {
                            break;
                        }
                    }                    
                }
            });
            hierarchy.get(0).setCharAt(0, (char)('0' + nbrOfNodes));
            final NodeDescriptor[] allNodes = new NodeDescriptor[nbrOfNodes];
            rootChildren.forEach(new Consumer<NodeDescriptor>() {
                int index = 0;
                @Override public void accept(NodeDescriptor node) {
                    try {
                    allNodes[index++] = node;
                    } catch (RuntimeException x) {
                        throw x;
                    }
                    node.forEachChild(this);
                }
            });
            paramValues.add(new Object[] {
                mergeToSingleLine(hierarchy),
                rootChildren,
                hierarchy.stream().collect(Collectors.joining("\n")),
                setParameterizedFor1or2(paramize1stValue, lazer, allNodes),
                thereWillBeFailures ?  setFailOn1or2(lazer, allNodes)
                        : "all_pass"
            });
        } while (lazer.pendingCombinations());
    }

    private static CharSequence setFailOn1or2(
            final Lazer lazer, final NodeDescriptor[] allNodes) {
        final StringBuilder result = new StringBuilder("failures=[");
        ToList.combineOneOrTwo().applyOn(new AbstractList<NodeDescriptor>() {
            @Override public NodeDescriptor get(int index) {
                NodeDescriptor node = allNodes[index];
                if (null != node) {
                    result.append(node.fail(bound
                            -> lazer.pick(getClass(), false, bound)))
                            .append(',');
                }
                return null;
            }
            @Override public int size() { return allNodes.length; }
        }, bound -> lazer.pick("failures", false, bound));
        return withEndBracket(result);
    }

    private static CharSequence withEndBracket(StringBuilder result) {
        if (',' == result.charAt(result.length() - 1)) {
            result.deleteCharAt(result.length() - 1);
        }
        return result.append(']');
    }

    private static CharSequence setParameterizedFor1or2(
            final int paramize1stValue, final Lazer lazer,
            final NodeDescriptor[] allNodes) {
        final StringBuilder result = new StringBuilder("withParams=[");
        ToList.combineOneOrTwo().applyOn(new AbstractList<NodeDescriptor>() {
            boolean firstIsPending = true;
            @Override public NodeDescriptor get(int index) {
                NodeDescriptor node = allNodes[index];
                if (null != node) {
                    result.append(node.paramize(bound
                            -> firstIsPending && paramize1stValue < bound
                               ? paramize1stValue
                               : lazer.pick(getClass(), false, bound)))
                            .append(',');
                }
                firstIsPending = false;
                return null;
            }
            @Override public int size() { return allNodes.length; }
        }, bound -> lazer.pick("params", false, bound));
        return withEndBracket(result);
    }

    private static StringBuilder mergeToSingleLine(
            List<? extends CharSequence> hierarchy) {
        StringBuilder merged = new StringBuilder(24);
        merged.append(hierarchy.get(0).charAt(0));

        int nestling = 0;
        for (int lineNbr = 0; lineNbr < hierarchy.size(); lineNbr += 2) {
            final CharSequence line = hierarchy.get(lineNbr);
            int i = 0;
            while (false == isJavaIdentifierPart(line.charAt(i += 4))) {}
            for (; i - 4 < 4 * nestling; --nestling) {
                merged.append(']');
            }
            merged.append('-').append(line.charAt(i));
            for (; (i += 4) < line.length(); ++nestling) {
                merged.append('[').append(line.charAt(i));
            }
        }
        while (0 <= --nestling) {
            merged.append(']');
        }
        return merged;
    }
}
