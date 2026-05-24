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

import java.util.EnumSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.lazyparams.internal.PowerMockRunnerLight;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import static org.lazyparams.demo.CloseableLifecycledResource.*;

/**
 * @author Henrik Kaipe
 */
@RunWith(PowerMockRunnerLight.class)
@PowerMockRunnerDelegate(
        /**
         * Closing of static scope during
         * {@link org.junit.runner.notification.RunNotifier#fireTestSuiteFinished(org.junit.runner.Description)}
         * following @AfterClass is only supported by PowerMock when using the
         * rather new feature {@link org.powermock.modules.junit4.PowerMockRunnerDelegate},
         * whereas default legacy (without this annotation) relies on JUnit-4's old
         * notification lifecycle, for which the "fireTestSuiteFinished" was never
         * introduced. For now the static scope closure on JUnit-4 is only
         * supported when "fireTestSuiteFinished" is fired.
         */
        JUnit4.class)
@PowerMockIgnore("org.lazyparams.demo.CloseableLifecycledResource")
public class OnScopeClosingPowermockedTest {

    /** @see #after() */
    @Rule
    public TestName testName = new TestName();

    @BeforeClass public static void beforeAll() {
        OnScopeClosing.attach(BEFORE_ALL).open();
        CloseableLifecycledResource.assertOpenOnesAreJust(BEFORE_ALL);
    }

    @Before public void before() {
        CloseableLifecycledResource.assertOpenOnesAreJust(BEFORE_ALL);
        OnScopeClosing.attach(BEFORE_EACH_TEST).open();
    }

    @Test @PrepareForTest(FoolParams.class/*To force new classloader if run with Powermock*/)
    public void one() {
        CloseableLifecycledResource.assertOpenOnesWhenTestMethodStarts();
        OnScopeClosing.attach(DURING_TEST_ONE).open();
    }

    @Test @PrepareForTest(ManyByMany.class/*To force new classloader if run with Powermock*/)
    public void two() {
        CloseableLifecycledResource.assertOpenOnesWhenTestMethodStarts();
        OnScopeClosing.attach(DURING_TEST_TWO).open();
        OnScopeClosing.attach(DURING_TEST_TWO_EXTRA).open();
    }

    @After public void after() {
        EnumSet<CloseableLifecycledResource> expectedOpen =
                EnumSet.of(BEFORE_ALL, BEFORE_EACH_TEST);

        switch (testName.getMethodName()) {
            case "one":
                expectedOpen.add(DURING_TEST_ONE);
                break;
            case "two":
                expectedOpen.add(DURING_TEST_TWO);
                expectedOpen.add(DURING_TEST_TWO_EXTRA);
                break;
            default:
                throw new AssertionError(
                        "Unexpected test-method " + testName.getMethodName());
        }

        CloseableLifecycledResource.assertOpenOnesAreJust(
                expectedOpen.toArray(new CloseableLifecycledResource[0]));

        OnScopeClosing.attach(AFTER_EACH_TEST).open();
    }

    @AfterClass public static void afterAll() {
        CloseableLifecycledResource.assertOpenOnesAreJust(BEFORE_ALL);
        OnScopeClosing.attach(AFTER_ALL).open();
    }
}
