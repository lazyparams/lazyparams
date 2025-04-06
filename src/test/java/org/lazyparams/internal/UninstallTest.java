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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;
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
import org.junit.jupiter.engine.descriptor.TestTemplateInvocationTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestTemplateTestDescriptor;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.Node;
import org.lazyparams.LazyParams;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.engine.descriptor.JupiterTestDescriptor;
import org.junit.jupiter.engine.descriptor.MethodBasedTestDescriptor;
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

    @SuppressWarnings("ThrowableResultIgnored")
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

        /* But it should be a different story after uninstalling LazyParams: */
        LazyParams.uninstall();
        assertThrows(NullPointerException.class,
                this::applyAfterAndThenPrepareTemplateInvocation);
        assertThrows(NullPointerException.class,
                this::applyAfterAndThenExecuteDynamic);
    }

    /**
     * This should normally result in NullPointerException but LazyParams
     * will prevent it after full installation.
     */
    private void applyAfterAndThenExecuteDynamic() throws Exception {
        JupiterTestDescriptor dynamicDescriptor =
                captureExecutorDescriptorArgOn(TestFactoryTestDescriptor.class);
        JupiterEngineExecutionContext stubbedContext = deeplyStubbedContext();
        dynamicDescriptor.after(stubbedContext);
        dynamicDescriptor.execute(stubbedContext,
                mock(Node.DynamicTestExecutor.class, withSettings().stubOnly()));
    }

    /**
     * This should normally result in NullPointerException but LazyParams
     * will prevent it after full installation.
     */
    private void applyAfterAndThenPrepareTemplateInvocation() throws Exception {
        JupiterEngineExecutionContext stubbedContext = deeplyStubbedContext();
        MutableExtensionRegistry dummyRegistry = MutableExtensionRegistry
                .createRegistryWithDefaultExtensions(stubbedContext.getConfiguration());
        when(stubbedContext.getExtensionRegistry()).thenReturn(dummyRegistry);
        when(stubbedContext.getExtensionContext()).thenReturn(null);
        TestTemplateInvocationTestDescriptor invDesc =
                captureExecutorDescriptorArgOn(TestTemplateTestDescriptor.class);
        invDesc.after(stubbedContext);
        invDesc.prepare(stubbedContext);
    }

    private <D extends JupiterTestDescriptor> D captureExecutorDescriptorArgOn(
            Class<? extends MethodBasedTestDescriptor> descriptorClass)
    throws Exception {
        Object testInstance = new Object() {
            @DisplayName("dummy") Stream<DynamicNode> dummy() {
                return Stream.of(DynamicTest.dynamicTest("dummy", () -> {}));
            }
        };
        JupiterEngineExecutionContext stubbedContext = stubContextAround(testInstance);

        Constructor descriptorConstr = Stream
                .of(descriptorClass.getConstructors())
                .filter(c -> 4 <= c.getParameterCount())
                .peek(c -> c.setAccessible(true))
                .findAny()
                .orElseThrow(NoSuchElementException::new);
        List<Object> constrArgs = new ArrayList<Object>();
        constrArgs.add(UniqueId.root(descriptorClass.getSimpleName(), "foobar"));
        constrArgs.add(testInstance.getClass());
        constrArgs.add(testInstance.getClass().getDeclaredMethod("dummy"));
        if (5 <= descriptorClass.getConstructors()[0].getParameterCount()) {
            constrArgs.add((Supplier)Collections::emptyList);
        }
        constrArgs.add(stubbedContext.getConfiguration());
        MethodBasedTestDescriptor descriptor = (MethodBasedTestDescriptor)
                descriptorConstr.newInstance(constrArgs.toArray());

        Node.DynamicTestExecutor executor = mock(Node.DynamicTestExecutor.class,
                withSettings().name(descriptorClass.getSimpleName() +  " Executor"));
        descriptor.execute(stubbedContext, executor);

        ArgumentCaptor<JupiterTestDescriptor> descriptorArg =
                ArgumentCaptor.forClass(JupiterTestDescriptor.class);
        verify(executor).execute(descriptorArg.capture());
        return (D) descriptorArg.getValue();
    }

    private JupiterEngineExecutionContext stubContextAround(Object testInstance) {
        JupiterEngineExecutionContext stubbedContext = deeplyStubbedContext();
        when(stubbedContext.getExtensionContext().getTestInstance())
                .thenReturn(Optional.of(testInstance));
        when(stubbedContext.getExtensionContext().getRequiredTestInstance())
                .thenCallRealMethod();
        when(stubbedContext.getExtensionRegistry().stream(any()))
                .thenReturn(Stream.of(new TestTemplateInvocationContextProvider() {
            @Override
            public boolean supportsTestTemplate(ExtensionContext context) {
                return true;
            }
            @Override
            public Stream<TestTemplateInvocationContext>
                    provideTestTemplateInvocationContexts(ExtensionContext context) {
                TestTemplateInvocationContext invCtx = mock(
                        TestTemplateInvocationContext.class,
                        withSettings().stubOnly());
                when(invCtx.getDisplayName(anyInt()))
                        .thenReturn("dummy");
                when(invCtx.getAdditionalExtensions())
                        .thenReturn(Collections.emptyList());
                return Stream.of(invCtx);
            }
        }));
        when(stubbedContext.getThrowableCollector())
                .thenReturn(new ThrowableCollector(x -> false));
        return stubbedContext;
    }

    private JupiterEngineExecutionContext deeplyStubbedContext() {
        return mock(JupiterEngineExecutionContext.class,
                withSettings().stubOnly().defaultAnswer(RETURNS_DEEP_STUBS));
    }
}
