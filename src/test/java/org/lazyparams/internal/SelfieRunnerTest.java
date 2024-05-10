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

import com.google.common.base.Predicates;
import java.util.stream.Stream;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.notification.RunNotifier;
import org.lazyparams.LazyParams;

/**
 * Selfierunner that represents the very minimum of what is required for
 * a JUnit-4 runner to be compatible with LazyParams. I.e. it needs to
 * implement {@link Filterable} in a reliable manner!
 *
 * @author Henrik Kaipe
 */
@RunWith(SelfieRunnerTest.class)
public class SelfieRunnerTest extends Runner implements Filterable {
    public SelfieRunnerTest(Class<SelfieRunnerTest> c) {}

    private Filter filter;

    /**
     * Filter support is what makes a runner compatible with LazyParams!
     */
    @Override
    public void filter(Filter filter) {
        this.filter = filter;
    }

    @Override
    public Description getDescription() {
        Description desc = Description.createSuiteDescription(SelfieRunnerTest.class);
        /*
         * Seems necessary to have children added here already or test-execution
         * may be cancelled because of "suite descriptor without tests (aka children)"
         */
        Stream.of(SelfieRunnerVerifyTest.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(org.junit.Test.class))
                .map(m -> m.getName().replace('_', ' '))
//        Stream.of("first test","next test 2 run","three times a charm")
                .map(s -> Description.createTestDescription(SelfieRunnerTest.class, s))
                .forEach(desc::addChild);
        return desc;
    }

    @Override
    public void run(RunNotifier notifier) {
        getDescription().getChildren().stream()
                .filter(null == filter ? Predicates.alwaysTrue() : filter::shouldRun)
                .forEach(d -> {
            notifier.fireTestStarted(d);
            LazyParams.pickValue("dummy_param", "foo", "bar");
            notifier.fireTestFinished(d);
        });
    }
}
