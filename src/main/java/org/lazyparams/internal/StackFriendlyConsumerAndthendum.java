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

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Is used by
 * {@link ProvideJunitPlatformHierarchical.DescriptorContextGuard#delayingListener} to line up
 * {@link org.junit.platform.engine.EngineExecutionListener EngineExecutionListener} invocations
 * until all test-properties (such as display-name) have been finalized in order to have
 * {@link org.junit.platform.engine.EngineExecutionListener EngineExecutionListener} event
 * methods invoked with properly finalized argument details.
 * <br><br>
 * This used to be achieved in a more ~best practice~ manner by having each
 * invocation postponed as a <code>Consumer&lt;EngineExecutionListener&gt;</code>
 * instance and use {@link Consumer#andThen(Consumer)} to have them all lined up
 * as a single compound <code>Consumer&lt;EngineExecutionListener&gt;</code>,
 * which {@link Consumer#accept(Object) accept(EngineExecutionListener)} method
 * would delegate its argument <code>EngineExecutionListener</code> to each of
 * its lined-up consumers. However, because of how the default
 * {@link Consumer#andThen(Consumer)} is implemented, this practice could
 * result in {@link StackOverflowError} ...<br>
 * <a href="https://bugs.openjdk.org/browse/JDK-8156610">https://bugs.openjdk.org/browse/JDK-8156610</a>
 * <br>... when there are many test repetitions (thousands).
 * <br><br>
 * The intention with this abstract class is to workaround the
 * {@link StackOverflowError} problem without breaking any {@link Consumer}
 * semantics. If {@link #andThenAccept(Object)} of this abstract class is
 * implemented instead of {@link Consumer#accept(Object)} then this pattern ...
 * <pre><code>
 * Consumer&lt;T&gt; compoundConsumer = beforeConsumer.andThen(new Consumer&lt;T&gt;() {
 *   {@literal @}Override
 *   public void accept(T t) {
 *     // Consumer functionality here!
 *   }
 * });
 * </code></pre> ... will instead be coded in this manner ... <pre><code>
 * Consumer&lt;T&gt; compoundConsumer = new StackFriendlyConsumerAndthendum&lt;T&gt;() {
 *   {@literal @}Override
 *   void andThenAccept(T t) {
 *     // Consumer functionality here!
 *   }
 * }.apply(beforeConsumer);
 * </code></pre>
 * ... with the advantage that the acquired <code>compoundConsumer</code> will
 * adhere to regular {@link Consumer} semantics without causing
 * {@link StackOverflowError}.
 *
 * @see ProvideJunitPlatformHierarchical.DescriptorContextGuard#delayingListener
 * @see <a href="https://bugs.openjdk.org/browse/JDK-8156610">Stack overflow error with Consumer interface</a>
 * @author Henrik Kaipe
 */
abstract class StackFriendlyConsumerAndthendum<T>
implements UnaryOperator<Consumer<T>>, Consumer<T> {

    private Consumer<T> before;
    private int prependumLength;

    abstract void andThenAccept(T t);

    @Override
    public final Consumer<T> apply(Consumer<T> before) {
        this.before = before;
        prependumLength = before instanceof StackFriendlyConsumerAndthendum
                ? ((StackFriendlyConsumerAndthendum)before).prependumLength + 1
                : 0;
        return this;
    }

    @Override
    public final void accept(T t) {
        if (null == this.before) {
            throw new IllegalStateException("Unary operation not applied!");
        }

        StackFriendlyConsumerAndthendum<T>[] unstackedAndthendumChain =
                new StackFriendlyConsumerAndthendum[prependumLength];
        StackFriendlyConsumerAndthendum<T> unstacked = this;
        for (int i = prependumLength; 0 <= --i;) {
            unstackedAndthendumChain[i] = unstacked;
            unstacked = (StackFriendlyConsumerAndthendum<T>) unstacked.before;
        }
        unstacked.before.accept(t);
        unstacked.andThenAccept(t);
        for (StackFriendlyConsumerAndthendum<T> eachUnstacked : unstackedAndthendumChain) {
            eachUnstacked.andThenAccept(t);
        }
    }

    /**
     * Not implemented!
     * It would make sense to override the default
     * {@link Consumer#andThen(Consumer)} and have it implemented in accordance
     * with the {@link UnaryOperator} nature of this class - but it's not used
     * by {@link ProvideJunitPlatformHierarchical} so just making sure this
     * function is not used!
     */
    @Override
    public final Consumer<T> andThen(final Consumer<? super T> after) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "#andThen(...) is not implemented!");
//        return new StackFriendlyConsumerAndthendum<T>() {
//            @Override void andThenAccept(T t) {
//                after.accept(t);
//            }
//        }.apply(this);
    }
}
