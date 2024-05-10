/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.showcase;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.lazyparams.LazyParams;

/**
 * @author Henrik Kaipe
 */
public class InAnyOrderCases {

    /**
     * Specify from parent scope, because it affects how many parameters are
     * introduced during test execution.
     * Purpose is to better uncover poor distribution of parameter values that
     * especially occurs when nbrOfTasks=4. It highlights some room for
     * improvement on the way permutations are combined for
     * {@link InAnyOrder}.
     */
    int nbrOfTasks = LazyParams.pickValue(i -> i + " tasks", 0, 1, 2, 3, 4);
    {
        /*
         * Help preserve order-of-execution in the test report by better
         * avoidance of duplication of test display names:
         */
        Timing.displayFromNow();        
    }

    String task1() {
        InAnyOrder.runQuietly(
            () -> LazyParams.pickValue("abORc", "A","B","c"),
            () -> LazyParams.pickValue("12or3", 1, 2, 3));
        return "Task1";
    }
    String task2() {return "TaskTwo";}
    String task3() throws IOException {
        if (FalseOrTrue.pickBoolean("on3fail","on3pass")) {
            throw new IOException("an xeption");
        } else {
            return "task3";
        }
    }
    String task1double() {
        return task1() + "double";
    }

    @Test void test() throws Exception {
        LazyParams.currentScopeConfiguration().setMaxFailureCount(12);
        LazyParams.<Invoker>pickValue().invoke(Stream.<Callable<String>>
                of(this::task1, this::task2, this::task3, this::task1double)
                .limit(nbrOfTasks)
                .toArray(Callable[]::new));
    }

    enum Invoker {
        verbose, qronicly, quietly;

        void invoke(Callable<String>[] tasks) throws Exception {
            switch (this) {
                case verbose:
                    InAnyOrder.runVerbosly(tasks);
                    break;
                case qronicly:
                    InAnyOrder.runQronicly(tasks);
                    break;
                case quietly:
                    InAnyOrder.runQuietly(asRunnables(tasks));
            }
        }

        private Runnable[] asRunnables(Callable<String>[] tasks) {
            return Stream.of(tasks).map(task -> new Runnable() {
                @Override
                public void run() {
                    this.<RuntimeException>call();
                }
                <E extends Exception> void call() throws E {
                    try {
                        task.call();
                    } catch (Exception ex) {
                        throw (E)ex;
                    }
                }
            }).toArray(Runnable[]::new);
        }
    }
}
