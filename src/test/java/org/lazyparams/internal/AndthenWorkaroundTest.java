/*
 * Copyright 2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.internal;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.lazyparams.LazyParams;
import org.lazyparams.LazyParamsCoreUtil;
import org.lazyparams.showcase.Timing;

import static org.junit.Assert.assertEquals;

/**
 * Verifies that ... <pre><code>
 * Consumer&lt;T&gt; compoundConsumer = beforeConsumer.andThen(new Consumer&lt;T&gt;() {
 *     {@literal @}Override
 *     public void accept(T t) {
 *         // Consumer functionality here!
 *     }
 * });
 * </code></pre> ... can be replaced by ... <pre><code>
 * Consumer&lt;T&gt; compoundConsumer = new StackFriendlyConsumerAndthendum&lt;T&gt;() {
 *   {@literal @}Override
 *   void andThenAccept(T t) {
 *     // Consumer functionality here!
 *   }
 * }.apply(beforeConsumer);
 * </code></pre>
 * ... with functionality preserved and {@link StackOverflowError} avoided!
 *
 * @author Henrik Kaipe
 */
public class AndthenWorkaroundTest {
    {
        Timing.displayFromNow(); // To display it before parameters
    }

    AndthenFunction andthenner = LazyParams.pickValue();
    int applyCount = LazyParams.pickValue(
            count -> "applied " + count + " times", 50, 50000);

    @Rule
    public final ExpectedException stackoverFlowExpectation = ExpectedException.none();

    @Before
    public void setupStackOverflowErrorExpectation() {
        if (10000 < applyCount && AndthenFunction.REGULAR == andthenner) {
            Class<? extends Throwable> expectation = StackOverflowError.class;
            String displayNameAppendix = " expecting " + expectation.getSimpleName();
            LazyParamsCoreUtil.displayOnSuccess(
                    displayNameAppendix, displayNameAppendix);
            stackoverFlowExpectation.expect(expectation);
        }
    }

    @Test
    public void test() {
        Timing.displayFromNow(); // To measure time from here!
        final int initialNoiseDelta = 42;
        AtomicInteger toBeIncreased = new AtomicInteger( - initialNoiseDelta);
        IntStream.range(0, applyCount)
                .<Consumer<AtomicInteger>>mapToObj(i -> AtomicInteger::incrementAndGet)
                .reduce(atomicInt -> atomicInt.getAndAdd(initialNoiseDelta), andthenner.operator())
                .accept(toBeIncreased);
        assertEquals("Final int value", applyCount, toBeIncreased.get());
    }

    enum AndthenFunction { REGULAR, WORKAROUND;
        BinaryOperator<Consumer<AtomicInteger>> operator() {
            switch (this) {
                default: throw new IllegalArgumentException("Bad constant: " + this);

                case REGULAR: return Consumer::andThen;
                case WORKAROUND: return (aggregate,appendum)
                        -> new StackFriendlyConsumerAndthendum<AtomicInteger>() {
                    @Override void andThenAccept(AtomicInteger value) {
                        appendum.accept(value);
                    }
                }.apply(aggregate);
            }
        }
    }
}
