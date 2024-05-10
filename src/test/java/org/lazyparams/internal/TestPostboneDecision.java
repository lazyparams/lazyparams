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

import java.lang.reflect.Field;
import java.util.Map;
import java.lang.reflect.Modifier;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.BindingPriority;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.JupiterTestDescriptor;
import org.junit.jupiter.engine.descriptor.MethodBasedTestDescriptor;
import org.junit.jupiter.engine.descriptor.NestedClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestFactoryTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestTemplateInvocationTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestTemplateTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.hierarchical.EngineExecutionContext;
import org.junit.platform.engine.support.hierarchical.Node;
import org.lazyparams.LazyParams;
import org.lazyparams.showcase.Ensembles;
import org.mockito.MockMakers;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

/**
 * Verifies whether {@link ProvideJunitPlatformHierarchical.AdviceRepeatableNode}
 * will properly postbone execution of {@link Node#after(EngineExecutionContext)}
 * on test-descriptor instances that also implement
 * {@link Node#execute(EngineExecutionContext,Node.DynamicTestExecutor)}.
 *
 * @see ProvideJunitPlatformHierarchical.AdviceRepeatableNode
 * @author Henrik Kaipe
 */
public class TestPostboneDecision {

    static final Map<UniqueId,?> postbonedAfterInvocations = new Object() {
        Map<UniqueId, ?> locatePostbonedAfters() {
            try {
                Field f = ProvideJunitPlatformHierarchical.AdviceRepeatableNode.class
                        .getDeclaredField("postbonedAfterInvocations");
                f.setAccessible(true);
                return (Map<UniqueId, ?>) f.get(null);
            } catch (Exception ex) {
                throw new Error(ex);
            }
        }
    }.locatePostbonedAfters();

    private TestDescriptor coreDescriptor;
    private Boolean expectPostbone;

    void setup(Class<? extends TestDescriptor> descriptorClass, boolean expect) {
        if (Modifier.isAbstract(descriptorClass.getModifiers())) {
            Assume.assumeTrue(
                    "Abstract classes are only testable if they are public",
                    Modifier.isPublic(descriptorClass.getModifiers()));
            descriptorClass = destract(descriptorClass);
        }

        coreDescriptor = mock(descriptorClass);
        Mockito.doReturn(UniqueId.root("whatever", "works")).when(coreDescriptor).getUniqueId();
        expectPostbone = expect;
    }

    @BeforeClass
    public static void ensureMockito_4_8_0_atLeast_orElseSkipThisTest() {
        try {
            Mockito.withSettings().mockMaker(MockMakers.INLINE);
        } catch (Throwable notGoodMockitoVersion) {
            Assume.assumeNoException(
                    "Mockito version too old", notGoodMockitoVersion);
        }
    }

    @Before
    public void pickParameterizedValues() throws Exception {

        Ensembles.<    Class<? extends TestDescriptor>,      Boolean>
//                         V                                     V   
                use( AbstractTestDescriptor.class,            false)
                .or(    JupiterTestDescriptor.class,             false)
                .or(    ClassBasedTestDescriptor.class,          false)
                .or(    ClassTestDescriptor.class,               false)
                .or(    MethodBasedTestDescriptor.class,         false)
                .or(    TestMethodTestDescriptor.class,          false)
                .or(   TestTemplateTestDescriptor.class,         false)
                .or( TestTemplateInvocationTestDescriptor.class, true)
                .or(    TestFactoryTestDescriptor.class,         false)
                .or(notPublic("DynamicNodeTestDescriptor"),      false)
                .or(notPublic("DynamicTestTestDescriptor"),      true)
                .or(notPublic("DynamicContainerTestDescriptor"), false)
                .or(    NestedClassTestDescriptor.class,         false)

                .asLazyDuo( (descriptorClass, expectPostboned) -> "Node#after(...) "
                        + (expectPostboned ? "is" : "is NOT") + " postboned on "
                        + descriptorClass.getSimpleName())
                .execute(this::setup);
    }

    Object postbonedContext() throws Exception {
        return postbonedAfterInvocations.remove(coreDescriptor.getUniqueId());
    }

    @Test
    public void verify() throws Exception {
        TestDescriptor descriptor2test = LazyParams.pickValue("when guarded", false, true)
                ? ProvideJunitPlatformHierarchical.DescriptorContextGuard.asGuardOf(coreDescriptor)
                : coreDescriptor;
        if (descriptor2test instanceof Node) {
            EngineExecutionContext context = Mockito.mock(
                    JupiterEngineExecutionContext.class,
                    Mockito.withSettings().name("Context on " + coreDescriptor.getClass().getSimpleName()));
            try {
                ((Node)descriptor2test).after(context);
            } catch (Throwable expected) {}
            assertEquals("Postboned context",
                    expectPostbone ? context : null,
                    postbonedContext());
        } else {
            assertEquals(false, ProvideJunitPlatformHierarchical
                    .AdviceRepeatableNode
                    .isAfterPostbonedOn(descriptor2test.getClass()));
        }
    }

    static Class<? extends TestDescriptor> notPublic(String descriptorName)
    throws ClassNotFoundException {
        return Class.forName(
                "org.junit.jupiter.engine.descriptor." + descriptorName,
                true, TestDescriptor.class.getClassLoader())
                .asSubclass(TestDescriptor.class);
    }
    <T extends TestDescriptor> Class<? extends T> destract(Class<T> abstractClass) {
        assert Modifier.isAbstract(abstractClass.getModifiers())
                : "De[ab]stractation only applies to abstract classes!";
        return new ByteBuddy()
                .subclass(abstractClass)
                .method(ElementMatchers.not(ElementMatchers.isDefaultMethod())
                        .and(ElementMatchers.isAbstract()))
                .intercept(MethodDelegation.to(this/*i.e. method #dummy()*/))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .asSubclass(abstractClass);
    }

    @BindingPriority(Integer.MAX_VALUE) @RuntimeType
    public Object dummy() { return null; }

    <T> T mock(Class<T> c) {
        return Mockito.mock(c, Mockito.withSettings()
                .mockMaker(MockMakers.INLINE)
                .defaultAnswer(Mockito.CALLS_REAL_METHODS));
    }

    @AfterClass
    public static void clearInlineMocking() {
        try {
            Mockito.clearAllCaches();/*to restore clean Descriptor-class functionality*/
        } catch (NoSuchMethodError probablyNotGoodMockitoVersion) {
            System.err.println(probablyNotGoodMockitoVersion.getMessage());
        }
    }
}
