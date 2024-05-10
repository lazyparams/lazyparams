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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runner.OrderWith;
import org.junit.runner.manipulation.Ordering;
import org.lazyparams.LazyParams;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Henrik Kaipe
 */
@OrderWith(InAnyOrderCasesVintage.class)
public class InAnyOrderCasesVintage implements Ordering.Factory {
    /** This is a suitable test for checking fresh install ... */
    static { LazyParams.uninstall(); }

    static final String testMethodToGo1st = "simpler9";

    @Rule
    public final TestRule timeout2and9 = (stmt,desc) -> {
        switch (desc.getMethodName()) {
            default:
                return stmt;
            case "simple2": case testMethodToGo1st:
//                System.out.println("Timeout on " + desc.getMethodName());
                return Timeout.seconds(1).apply(stmt, desc);
        }
    };

    @Override
    public Ordering create(Ordering.Context context) {
        return new Ordering() {
            /**
             * Make sure test-method "simpler9" runs first!
             */
            @Override
            protected List<Description> orderItems(Collection<Description> descriptions) {
                final List<Description> inOrder = new ArrayList<>();
                descriptions.forEach(desc -> {
                    if (testMethodToGo1st.equals(desc.getMethodName())) {
                        inOrder.add(0, desc);
                    } else {
                        inOrder.add(desc);
                    }
                });
                assertThat(inOrder).as("Test-method descritions in execution order")
                        .contains(descriptions.toArray(new Description[0]));
                assertThat(inOrder.get(0).getMethodName())
                        .as("Name of first test method to execute")
                        .isEqualTo(testMethodToGo1st);
                return inOrder;
            }
        };
    }

    /**
     * Test-class has been prepared to run first test with a timeout in order
     * to have lazy parametrization introduced on a child thread.
     * For such situations it is necessary to have LazyParams explicitly
     * installed before the timeout child-thread introduces its parameters.
     * This method ensures the installation happens and is properly completed
     * before the first timeout child-thread is started!
     * Removal of this method should break lazy parametrization for the first
     * test-method executed.
     */
    @BeforeClass public static void ensureInstalled() {
        LazyParams.install();
    }

    @Test public void complex() throws Exception {new InAnyOrderCases().test();}

    @Test public void simple0() throws Exception {
        SimpleInvocation.invoke(0);
    }
    @Test public void simple1() throws Exception {
        SimpleInvocation.invoke(1);
    }
    @Test public void simple2() throws Exception {
        SimpleInvocation.invoke(2);
    }
    @Test(timeout = 1000)
    public void simple3() throws Exception {
        SimpleInvocation.invoke(3);
    }
    @Test public void simple4() throws Exception {
        SimpleInvocation.invoke(4);
    }
    @Test public void simple5() throws Exception {
        SimpleInvocation.invoke(5);
    }
    @Test(timeout = 1000)
    public void simple6() throws Exception {
        SimpleInvocation.invoke(6);
    }
    @Test
    public void simpler9() throws Exception {
        InAnyOrder.runQronicly(SimpleInvocation.values());
    }

    enum SimpleInvocation implements Callable<String> {
        INT_1, INV_TWO, INV_3, INV_FOUR, INV_5, INV_SIX, INV_7, IN8, IN9;

        @Override public String call() {
            LazyParams.pickValue(this);
            return this.name();
        }

        static void invoke(int limit) throws Exception {
            LazyParams.<InAnyOrderCases.Invoker>pickValue().invoke(Stream
                    .of(values()).limit(limit).toArray(SimpleInvocation[]::new));
        }
    }
}
