/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.lazyparams.showcase.FalseOrTrue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.lazyparams.LazyParams.pickValue;

/**
 * Test that seeds {@link TestFactory @TestFactory test} with some additional
 * parametrization of the lazy kind.
 * Also a little check on ability to install - or not - from static scope of
 * {@link BeforeAll @BeforeAll}.
 *
 * @author Henrik Kaipe
 */
public class Factoring {

    static boolean uninstallOn1stRun = true;

    @BeforeAll
    static void forceUninstallOn1stRun() {
        if (uninstallOn1stRun) {
            LazyParams.uninstall();
        }
        uninstallOn1stRun = FalseOrTrue.pickBoolean("already installed", "force uninstall");
    }

    public Stream<DynamicNode> scopeBridge() {
        int toFailOn = pickValue("failer", 901, 73, 3, 8);
        return parameterixeDynamicNode(toFailOn);
    }

    Stream<DynamicNode> parameterixeDynamicNode(int failValue) {
        return Stream
                .of(DynamicTest.dynamicTest("halo", () -> {
            assertThat("Value dynamic",
                    pickValue("dynamic", 8, 3, 4),
                    not(equalTo(failValue)));
        }), DynamicTest.dynamicTest("no73", () -> {
            assertThat("failer value", failValue,
                    not(equalTo(73)));
        }));
    }

    @TestFactory Stream<? extends DynamicNode> tests() {
        String selection = FalseOrTrue.pickBoolean("filter on")
                ? pickValue(""::concat, "normal", "Param") : null;
        Stream<DynamicTest> testStream = Stream
                .of(LeafParameterizedJupiter.class.getDeclaredMethods())
                .sorted(Comparator.comparing(Method::getName))
                .filter(m -> false == Modifier.isPrivate(m.getModifiers())
                        && 1 <= m.getAnnotations().length
                        && void.class == m.getReturnType())
                .filter(m -> null == selection || m.getName().contains(selection))
                .map(m -> DynamicTest.dynamicTest(m.getName(), () -> {
                    try {
                        m.invoke(new LeafParameterizedJupiter());
                    } catch (InvocationTargetException x) {
                        throw x.getTargetException();
                    }        
                }));
        if (FalseOrTrue.pickBoolean("containerized")) {
            return Stream.of(
                    DynamicTest.dynamicTest("scoped bridge", this::scopeBridge),
                    DynamicContainer.dynamicContainer("container", testStream));
        }
        return testStream;
    }
}
