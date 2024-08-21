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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.TestTemplateInvocationTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestTemplateTestDescriptor;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.Node;
import org.lazyparams.LazyParams;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.engine.descriptor.JupiterTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestFactoryTestDescriptor;
import org.junit.jupiter.engine.extension.MutableExtensionRegistry;
import org.junit.platform.engine.support.hierarchical.ThrowableCollector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Henrik Kaipe
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UninstallTest {

    static String pendingRepeat;
    static List<String> completedPicksLog;

    @BeforeAll
    static void install() {
        LazyParams.install();        
    }
    @BeforeAll
    static void resetPickLog() {
        pendingRepeat = null;
        completedPicksLog = new ArrayList<>();
    }
    void valuePick(String paramName) {
        /* This shall force implicit install: */
        switch (LazyParams.pickValue(paramName, 1, 2)) {
            case 1:
                assertNull(pendingRepeat,
                        "as first pick made for " + paramName);
                pendingRepeat = paramName;
                return;
            case 2:
                try {
                    assertEquals(paramName, pendingRepeat,
                            "1st pick parameter name must match second");
                    completedPicksLog.add(paramName);
                } finally {
                    pendingRepeat = null;
                }
                return;
            default:
                throw new Error("Only 1 or 2 expected from pick");
        }
    }

    @Order(1)
    @RepeatedTest(2) void initialRepeat() { valuePick("nbr1"); }
    @Order(2)
    @TestFactory Stream<DynamicNode> initialDynamic() {
        return Stream.of(DynamicTest.dynamicTest( "dynamic",
                () -> valuePick("nbr2")));
    }

    @Order(3)
    @Test void verify1stUninstall() throws Exception {
        assertInstalledThenUninstall(
                "nbr1", "nbr1", "nbr2");
    }

    @Order(4)
    @TestFactory Stream<DynamicNode> dynamicAfterImplicitReinstall() {
        return initialDynamic();
    }
    @Order(5)
    @RepeatedTest(2) void repeatAfterImplicitReinstall() { valuePick("nbr5"); }

    @Order(6)
    @Test void verifyImplicitReinstalledAndForceExplicitReinstall() throws Exception {
        assertInstalledThenUninstall(
                "nbr2", "nbr5", "nbr5");
        LazyParams.install();
    }

    @Order(7)
    @RepeatedTest(2) void repeatAfterExplicitReinstall() { valuePick("nbr7"); }

    @Order(8)
    @TestFactory Stream<DynamicNode> dynamicAfterExplicitReinstall() {
        return initialDynamic();
    }

    @Order(9)
    @Test void verifyExplicitReinstall() throws Exception {
        assertInstalledThenUninstall(
                "nbr7", "nbr7", "nbr2");
    }

    private void assertInstalledThenUninstall(String... expectedPicksLog) throws Exception {
        try {
            assertThat("Completed picks log",
                    completedPicksLog, contains(expectedPicksLog));
            assertThat("Pending pick", pendingRepeat, nullValue());
        } finally {
            resetPickLog();
        }

        /* These operations are silent noop when LazyParams is installed:*/
        applyAfterAndThenPrepareTemplateInvocation();
        applyAfterAndThenExecuteDynamic();

        /* But should be a different story after uninstalling LazyParams: */
        LazyParams.uninstall();
        assertThrows(NullPointerException.class,
                this::applyAfterAndThenPrepareTemplateInvocation);
        assertThrows(NullPointerException.class,
                this::applyAfterAndThenExecuteDynamic);
    }

    private void applyAfterAndThenExecuteDynamic() throws Exception {
        JupiterTestDescriptor dynamicDescriptor = newDummyDynamicDescriptor();
        dynamicDescriptor.after(mock(JupiterEngineExecutionContext.class));
        dynamicDescriptor.execute(
                mock(JupiterEngineExecutionContext.class, RETURNS_DEEP_STUBS),
                mock(Node.DynamicTestExecutor.class));
    }

    private JupiterTestDescriptor newDummyDynamicDescriptor() throws Exception {
        Node.DynamicTestExecutor executor = mock(Node.DynamicTestExecutor.class);

        final Object testInstance = new Object() {
            @DisplayName("bar") Stream<DynamicNode> factoryMethod() {
                return Stream.of(DynamicTest.dynamicTest("dummy", () -> {}));
            }
        };
        new TestFactoryTestDescriptor(
                UniqueId.root("factory-dummy", "bar"),
                testInstance.getClass(),
                testInstance.getClass().getDeclaredMethod("factoryMethod"),
                mock(JupiterConfiguration.class)) {{
            JupiterEngineExecutionContext ctx = mock(
                    JupiterEngineExecutionContext.class,
                    RETURNS_DEEP_STUBS);
            when(ctx.getThrowableCollector())
                    .thenReturn(new ThrowableCollector(x -> false));
            when(ctx.getExtensionContext().getRequiredTestInstance())
                    .thenReturn(testInstance);
            invokeTestMethod(ctx, executor);
        }};

        ArgumentCaptor<JupiterTestDescriptor> descriptorArg =
                ArgumentCaptor.forClass(JupiterTestDescriptor.class);
        verify(executor).execute(descriptorArg.capture());
        return descriptorArg.getValue();
    }

    /**
     * This should normally result in NullPointerException but LazyParams
     * will prevent it after full installation.
     */
    private void applyAfterAndThenPrepareTemplateInvocation() throws Exception {
        JupiterEngineExecutionContext mockContext = mock(
                JupiterEngineExecutionContext.class, RETURNS_DEEP_STUBS);
        MutableExtensionRegistry dummyRegistry = MutableExtensionRegistry
                .createRegistryWithDefaultExtensions(mockContext.getConfiguration());
        when(mockContext.getExtensionRegistry()).thenReturn(dummyRegistry);
        when(mockContext.getExtensionContext()).thenReturn(null);
        TestTemplateInvocationTestDescriptor invDesc = newDummyInvocationDescriptor();
        invDesc.after(mockContext);
        invDesc.prepare(mockContext);
    }

    @DisplayName("foo")
    private TestTemplateInvocationTestDescriptor newDummyInvocationDescriptor()
    throws Exception {
        TestTemplateInvocationContextProvider provider = new TestTemplateInvocationContextProvider() {
            @Override
            public boolean supportsTestTemplate(ExtensionContext context) {
                return true;
            }
            @Override
            public Stream<TestTemplateInvocationContext>
                    provideTestTemplateInvocationContexts(ExtensionContext context) {
                TestTemplateInvocationContext invCtx = mock(
                        TestTemplateInvocationContext.class);
                when(invCtx.getDisplayName(anyInt()))
                        .thenReturn("bar");
                when(invCtx.getAdditionalExtensions())
                        .thenReturn(Collections.emptyList());
                return Stream.of(invCtx);
            }
        };

        JupiterEngineExecutionContext context = mock(
                JupiterEngineExecutionContext.class, RETURNS_DEEP_STUBS);
        when(context.getExtensionRegistry().stream(any()))
                .thenReturn(Stream.of(provider));
        Node.DynamicTestExecutor executor = mock(Node.DynamicTestExecutor.class);        
        try {
            new TestTemplateTestDescriptor(
                    UniqueId.root("template-dummy", "foo"),
                    getClass(),
                    getClass().getDeclaredMethod("newDummyInvocationDescriptor"),
                    mock(JupiterConfiguration.class))
                    .execute(context, executor);
        } catch (Exception ex) {
            throw new Error(ex);
        }

        ArgumentCaptor<TestDescriptor> descriptorArg =
                ArgumentCaptor.forClass(TestDescriptor.class);
        verify(executor).execute(descriptorArg.capture());
        return (TestTemplateInvocationTestDescriptor) descriptorArg.getValue();
    }
}
