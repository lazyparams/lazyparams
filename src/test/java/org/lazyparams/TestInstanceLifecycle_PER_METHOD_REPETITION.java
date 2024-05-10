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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.lazyparams.showcase.TestInstance_Lifecycle_PER_METHOD_REPETITION;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * To verify extensions don't break the experimental lifecycle of
 * {@link TestInstance_Lifecycle_PER_METHOD_REPETITION}.
 *
 * @see TestInstance_Lifecycle_PER_METHOD_REPETITION
 * @author Henrik Kaipe
 */
@TestInstance_Lifecycle_PER_METHOD_REPETITION
@ExtendWith(TestInstanceLifecycle_PER_METHOD_REPETITION.InstanceFactory.class)
public class TestInstanceLifecycle_PER_METHOD_REPETITION {

    static class InstanceFactory
    implements TestInstanceFactory, TestInstancePostProcessor, BeforeEachCallback {
        @Override
        public Object createTestInstance(
                TestInstanceFactoryContext factoryContext,
                ExtensionContext extensionContext) {
            return new TestInstanceLifecycle_PER_METHOD_REPETITION();
        }
        @Override
        public void beforeEach(ExtensionContext context) {
            TestInstanceLifecycle_PER_METHOD_REPETITION testInstance =
                    (TestInstanceLifecycle_PER_METHOD_REPETITION)
                    context.getTestInstance().get();
            assert false == testInstance.touchedByBeforeEachCallback
                    : "This must be first test-instance encounter!";
            testInstance.touchedByBeforeEachCallback = true;
        }
        @Override
        public void postProcessTestInstance(
                Object testInstance, ExtensionContext context) {
            TestInstanceLifecycle_PER_METHOD_REPETITION test =
                    (TestInstanceLifecycle_PER_METHOD_REPETITION) testInstance;
            assertNull(test.touchedByBeforeEachCallback,
                    "No value should have been initiated for #touchedByBeforeEachCallback");
            test.touchedByBeforeEachCallback = false;
        }
    }

    private static TestInstanceLifecycle_PER_METHOD_REPETITION latestTestInstance;

    private Boolean touchedByBeforeEachCallback;

    @Test void run3times() {
        LazyParams.pickValue(i -> "#" + i, 1,2,3);
        assertNotSame(latestTestInstance, this,
                "New test-instance expected for each repetition");
        latestTestInstance = this;
        assert touchedByBeforeEachCallback
                : "Test instance should have been available during before-each callback";
    }
}
