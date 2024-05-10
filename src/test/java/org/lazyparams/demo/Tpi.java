/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.demo;

import com.google.testing.junit.testparameterinjector.junit5.TestParameter;
import com.google.testing.junit.testparameterinjector.junit5.TestParameterInjectorTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lazyparams.LazyParams;
import org.lazyparams.showcase.TestInstance_Lifecycle_PER_METHOD_REPETITION;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verify compatibility with 3rd-party Jupiter extension TestParameterInjector,
 * which brings parametrization on its own.
 *
 * @author Henrik Kaipe
 */
@TestInstance_Lifecycle_PER_METHOD_REPETITION
@ExtendWith(Tpi.MonitorTestInstanceLifecycle.class)
public class Tpi {

    @TestParameter boolean fTpi;
    final boolean fLazy = LazyParams.pickValue("fLazy", false, true);

    private static Object testInstanceCache = null;

    @BeforeEach void verifyNewTestInstance() {
        assertNotNull(testInstanceCache, "Expect something cached");
        assertNotSame(testInstanceCache, this,
                TestInstance_Lifecycle_PER_METHOD_REPETITION.class
                + " should prevent test-instance recycling");
    }
    @AfterEach void cacheInstanceToVerifyItsNotReusedOnNextTest() {
        testInstanceCache = this;
    }

    @TestParameterInjectorTest void fieldsOnly() {}
    @Test void lazyFieldOnly() {}

    @TestParameterInjectorTest void allParams(
            @TestParameter boolean pTpi1, @TestParameter boolean pTpi2) {
        assertEquals(
                false, mLazy() && fTpi && fLazy && pTpi1 && pTpi2,
                "Fail on all true");
        assertEquals(
                true, mLazy() || fLazy || fTpi || pTpi2 || pTpi1,
                "Fail on all false");
        LazyParams.pickValue("conditional_lazy", false, true);
    }

    boolean mLazy() {
        return LazyParams.pickValue("mLazy", false, true);
    }

    static class MonitorTestInstanceLifecycle
    implements BeforeAllCallback, AfterAllCallback {
        @Override public void beforeAll(ExtensionContext context) {
            assertNull(testInstanceCache,
                    "No test-instance created before test");
            /* Create dummy instance: */
            testInstanceCache = new Object();
        }
        @Override public void afterAll(ExtensionContext context) {
            assertThat(testInstanceCache)
                    .isInstanceOf(Tpi.class)
                    .as("Expected creation of real test-instance");
            testInstanceCache = null;
        }
    }
}
