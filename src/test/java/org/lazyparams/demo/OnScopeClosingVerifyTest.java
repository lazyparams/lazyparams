/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.RunWith;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runners.Parameterized;
import org.lazyparams.LazyParams;

import static org.junit.Assert.*;

/**
 * @author Henrik Kaipe
 */
@RunWith(Parameterized.class)
public class OnScopeClosingVerifyTest {

    final Class<?> class2test;

    public OnScopeClosingVerifyTest(String testName, Class<?> class2test) {
        this.class2test = class2test;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<?> classes2test() {
        return Stream
                .of(OnScopeClosingPowermockedTest.class,
                        OnScopeClosingVintageVanillaTest.class,
                        OnScopeClosingVintageDuplicatedTest.class)
                .map(class2test -> new Object[] {class2test.getSimpleName(), class2test})
                .collect(Collectors.toList());
    }

    @Before public void closeAll() {
        Stream.of(CloseableLifecycledResource.values())
                .forEach(CloseableLifecycledResource::close);
    }

    @Test public void LazyParams_preInstalled() { LazyParams.install(); }
    @Test public void LazyParams_unInstalled() { LazyParams.uninstall(); }

    @After
    public void verifyBoth() {
        CloseableLifecycledResource.assertOpenOnesAreJust();

        final List<Failure> failureLog = new ArrayList<>();//is expected to stay empty!
        final ListIterator<Description> started =
                new ArrayList<Description>().listIterator();
        final List<Description> finished = new ArrayList<Description>();

        JUnitCore core = new JUnitCore();
        core.addListener(new RunListener() {
            @Override
            public void testIgnored(Description description) {
                failureLog.add(new Failure(description, new Throwable("TEST IGNORED!")));
            }
            @Override
            public void testAssumptionFailure(Failure failure) {
                failureLog.add(failure);
            }
            @Override
            public void testFailure(Failure failure) {
                failureLog.add(failure);
            }
            @Override
            public void testFinished(Description actual) {
                if (false == started.hasPrevious()) {
                    failureLog.add(new Failure(actual, new Throwable("WAS NEVER STARTED!")));
                }
                try {
                    assertEquals("test finished", started.previous(), actual);
                    finished.add(actual);
                    started.remove();
                } catch (Throwable failure) {
                    failureLog.add(new Failure(actual, failure));
                }
            }
            @Override
            public void testStarted(Description description) {
                started.add(description);
            }
            @Override
            public void testSuiteFinished(Description description) {
                testFinished(description);
            }
            @Override
            public void testSuiteStarted(Description description) {
                testStarted(description);
            }
        });
        core.run(Request.aClass(class2test).filterWith(Filter.ALL));

        for (String method : new String[] {"one","two"}) {
            assertTrue(method + " finished", finished.remove(
                    Description.createTestDescription(class2test, method)));            
        }

        assertTrue("Unexpectedly finished: " + finished, finished.isEmpty()
                || Description.createSuiteDescription(class2test).equals(finished.remove(0)));

        CloseableLifecycledResource.assertOpenOnesAreJust();

        if (started.hasPrevious()) {
            fail(started.previous() + " was not finished");
        }
        if (false == failureLog.isEmpty()) {
            Failure f = failureLog.get(0);
            throw new Error(f.getDescription().getDisplayName(), f.getException());
        }
    }
}
