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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.EngineExecutionContext;
import org.junit.platform.engine.support.hierarchical.Node;
import org.mockito.MockMakers;
import org.mockito.MockSettings;
import org.mockito.stubbing.Answer;
import org.lazyparams.LazyParams;
import org.lazyparams.internal.ProvideJunitPlatformHierarchical.DescriptorContextGuard;
import org.lazyparams.showcase.ScopedLazyParameter;
import org.lazyparams.showcase.Timing;
import org.lazyparams.showcase.ToPick;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author Henrik Kaipe
 */
public class DescriptorContextGuardCreationChecks {

    private static <T> Predicate<T> in(Collection<T> collection) {
        return collection::contains;
    }
    private static <T> Predicate<T> in(T[] array) {
        return in(asList(array));
    }

    private static List<Method> methodsImplemented() {
        return Stream.of(DescriptorContextGuard.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .collect(Collectors.toList());
    }

    private static Method pickProxyMethod() {
        return MethodsSorted.pick(Stream
                .of(DescriptorContextGuard.class.getMethods())
                .filter(in(methodsImplemented()).negate())
                .filter(in(Node.class.getMethods())
                        .or(in(TestDescriptor.class.getMethods())))
                .collect(Collectors.toList()));
    }

    private static boolean initialized() {
        return LazyParams.pickValue("initialized", false, true);
    }

    @Test
    public void needIdentityEqualityAndHashCode() {
        Class<? extends TestDescriptor> targetClass = LazyParams.pickValue(
                c -> "target=" + c.getSimpleName(),
                TestDescriptor.class, DescriptorNode.class);
        TestDescriptor target = mock(targetClass, inv -> {
            return UniqueId.class == inv.getMethod().getReturnType()
                    ? UniqueId.forEngine("test")
                    : RETURNS_DEFAULTS.answer(inv);
        });
        DescriptorContextGuard guard2test = DescriptorContextGuard.of(target, false);
        DescriptorContextGuard otherGuardWithSameData = DescriptorContextGuard
                .of(target, initialized());
        assert guard2test != otherGuardWithSameData :
                "Factory can reuse instance when both are created with initalized=true";
        assertFalse("Equality with other",
                guard2test.equals(otherGuardWithSameData));
        assertTrue("Equality with self", guard2test.equals(guard2test));
        int hashCodeOnGuard2test = guard2test.hashCode();
        int hashCodeOnOther = otherGuardWithSameData.hashCode();
        assertFalse("Hashcode equal with other",
                hashCodeOnGuard2test == hashCodeOnOther);
        assertEquals("Hashcode consistency",
                hashCodeOnOther, otherGuardWithSameData.hashCode());
        assertEquals("Hashcode consistency",
                hashCodeOnGuard2test, guard2test.hashCode());
    }

    @Test
    public void topOfNode() throws Exception {
        Timing.displayFromNow();
        LazyParams.currentScopeConfiguration().setMaxTotalCount(250);
        LazyParams.currentScopeConfiguration().setMaxFailureCount(20);
        List<Method> guardInteractions = new ArrayList<>();
        Answer<?> monitoredAnswer = inv -> {
            guardInteractions.add(inv.getMethod());
            return RETURNS_MOCKS.answer(inv);
        };
        List<Method> invocationsToReachTarget = new ArrayList<>();
        List<Object> argsForInvocationAt1 = new ArrayList<>();
        AtomicReference<Object> recordMethodExchange = new AtomicReference<>();
        Function<Class<?>,Object> mockMonitor = type -> {
            if (Boolean.class == type || boolean.class == type) {
                return true;
            } else if (String.class == type) {
                return new Object().toString();
            } else if (type.isEnum()) {
                return LazyParams.pickValue(
                        "returns", type.getEnumConstants());
            } else if (Optional.class == type) {
                return Optional.of(new Object());
            } else if (false == type.isPrimitive()) {
                MockSettings settings = withSettings();
                if (Modifier.isFinal(type.getModifiers())) {
                    settings = settings.mockMaker(MockMakers.INLINE);
                }
                return mock(type, settings.defaultAnswer(monitoredAnswer));
            } else if (void.class == type) {
                return null;
            } else {
                return 0;
            }
        };
        DescriptorNode target = mock(DescriptorNode.class, inv -> {
            Method m = inv.getMethod();
            if (m.getName().equals("toString")) {
                return "Mocked " + inv.getMethod().getDeclaringClass().getSimpleName();
            }
            invocationsToReachTarget.add(m);
            switch (invocationsToReachTarget.size()) {
                case 1:
                    assertThat("Return-type first invocation",
                            m.getReturnType(),
                            sameInstance(UniqueId.class));
                    return UniqueId.forEngine("unit-test mocking");
                case 2:
                    assertNotNull(
                            "Only reach here when target interaction is expected",
                            recordMethodExchange.get());
                    Collections.addAll(argsForInvocationAt1, inv.getArguments());
                    Object returnValue =
                            mockMonitor.apply(inv.getMethod().getReturnType());
                    recordMethodExchange.updateAndGet(pendingExchange -> pendingExchange
                            .toString().startsWith(inv.getMethod().getName() + " ")
                            ? returnValue : pendingExchange);
                    return returnValue;
                default:
                    return RETURNS_DEFAULTS.answer(inv);
            }
        });
        DescriptorContextGuard guard = DescriptorContextGuard
                .of(target, initialized());
        verify(target).getUniqueId();//Expected during guard-creation!
        Method m;
        Object[] monitoredArgs;
        Object result;
        switch (MethodKind.pick()) {
            case proxy:
                m = pickProxyMethod();
                recordMethodExchange.set(m.getName() + " to be invoked");
                monitoredArgs = Stream.of(m.getParameterTypes())
                        .map(mockMonitor).toArray();
                result = m.invoke(guard, monitoredArgs);
                assertThat("Value returned from guarded target", result,
                        m.getReturnType().isPrimitive()
                        ? equalTo(recordMethodExchange.get())
                        : sameInstance(recordMethodExchange.get()));
                if (false == guardInteractions.isEmpty()) {
                    throw new AssertionError(
                            "Unexpected interaction with argument or return-value: "
                            + guardInteractions);
                }
                m.invoke(verify(target), Stream.of(monitoredArgs)
                        .map(eachGuardArgument -> same(eachGuardArgument))
                        .toArray());
                verifyNoMoreInteractions(target);
                return;
            case impl:
                m = MethodsSorted.pick(methodsImplemented());
                recordMethodExchange.set(m.getName() + " to be invoked");
                monitoredArgs = Stream.of(m.getParameterTypes())
                        .map(mockMonitor).toArray();
                result = m.invoke(guard, monitoredArgs);
                if (false == Objects.equals(result, recordMethodExchange.get())
                        || false == guardInteractions.isEmpty()
                        || 2 != invocationsToReachTarget.size()
                        || monitoredArgs.length != argsForInvocationAt1.size()) {
                    return;
                }
                for (int i = 0; i < monitoredArgs.length; ++i) {
                    if (monitoredArgs[i] != argsForInvocationAt1.get(i)) {
                        return;
                    }
                }
                throw new AssertionError("No implementation detected for " + m);

            default:
                throw new Error("Unknown MethodKind: " + MethodKind.pick());
        }   
    }

    enum MethodKind {
        impl,
        proxy;

        static MethodKind pick() { return LazyParams.pickValue(); }
    }

    enum MethodsSorted {
        BY_TO_DISPLAY {
            @Override
            Method makePick(Collection<Method> methodOptions) {
                return methodOptions.stream()
                        .sorted(Comparator.comparing(Method::getName))
                        .collect(ToPick.from())
                        .withExplicitParameterId(MethodsSorted.class)
                        .asParameter(m
                        -> m.getDeclaringClass().getSimpleName() + "::" + m.getName())
                        .pickValue();
            }
        },
        BY_TO_STRING {
            @Override
            Method makePick(Collection<Method> methodOptions) {
                return methodOptions.stream()
                        .sorted(Comparator.comparing(Method::toString))
                        .collect(ToPick.from())
                        .withExplicitParameterId(MethodsSorted.class)
                        .asParameter(m -> "method=" + m.getName())
                        .pickValue();
            }
        },
        BY_NAME_KEY {
            @Override
            Method makePick(Collection<Method> methodOptions) {
                Map<String,Method> nameMapped = methodOptions.stream()
                        .collect(Collectors.toMap(Method::getName, m -> m));
                return nameMapped.entrySet().stream()
                        .map(Map.Entry::getKey)
                        .sorted()
                        .collect(ToPick.from())
                        .withExplicitParameterId(MethodsSorted.class)
                        .asParameter(m -> "name=" + m.getName(), (keys,seeds)
                                -> nameMapped.get(keys.get(seeds.next(keys.size()))))
                        .pickValue();
            }
        };

        abstract Method makePick(Collection<Method> methodOptions);

        static Method pick(Collection<Method> methodOptions) {
            return ScopedLazyParameter.from(values())
                    .notCombined()
                    .qronicly()
                    .asParameter(Enum::name).pickValue()
                    .makePick(methodOptions);
        }
    }

    interface DescriptorNode extends TestDescriptor, Node<EngineExecutionContext> {}

    @Test
    public void pickMethodsSorted() {
        ScopedLazyParameter.from(1, 2, 3).notCombined()
                        .asParameter(i -> "exec#" + i).pickValue();
        assertThat("Constant of MethodSorted",
                ScopedLazyParameter
                        .from(MethodsSorted.values())
                        .qronicly()
                        .asParameter(Enum::name).pickValue(),
                sameInstance(MethodsSorted.BY_TO_STRING));
                
    }
}
