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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.lazyparams.ToDisplayFunction;
import org.lazyparams.showcase.ScopedLazyParameter.FactoryRoot;

/**
 * Provides static method {@link #from()} as a mean to help create parameters
 * from streams.
 * <br/>
 * Here are also other methods that provide collector implementations
 * that achieve elegant parametrization from streams with even less syntax.
 * But these other methods are deprecated, because their reliability need
 * certain stream properties to be guaranteed on repeated stream creations.
 * These demands on special stream properties are pitfalls that can be very
 * confusing if developer lacks knowledge on how these things work, so usage
 * of those methods have been discouraged by having them deprecated.
 *
 * @see #from()
 *
 * @author Henrik Kaipe
 */
public class ToPick {
    private ToPick() {}

    /**
     * Collector implementation that accumulates values from stream to a list,
     * which finisher is specified as constructor argument.
     * All {@link ToPick} factory methods returns an instance of this class.
     */
    private static class CollectorImpl<T,R> implements Collector<T,List<T>,R> {

        private static final Supplier SUPPLIER = new Supplier<List<Object>>() {
            @Override
            public List<Object> get() {
                return new ArrayList<Object>();
            }
        };
        private static final BiConsumer ACCUMULATOR = new BiConsumer<List<Object>,Object>() {
            @Override
            public void accept(List<Object> accumulation, Object nextValue) {
                accumulation.add(nextValue);
            }
        };

        private final Function<List<T>,R> finisher;

        CollectorImpl(Function<List<T>,R> finisher) {
            this.finisher = finisher;
        }

        @Override
        public Supplier<List<T>>        supplier() { return SUPPLIER; }
        @Override
        public BiConsumer<List<T>,T> accumulator() { return ACCUMULATOR; }
        @Override
        public BinaryOperator<List<T>>  combiner() { return null; }
        @Override
        public Function<List<T>,R>      finisher() { return finisher; }
        @Override
        public Set<Collector.Characteristics> characteristics() { return Collections.emptySet(); }
    }

    /**
     * Common {@link CollectorImpl#finisher()} implementation super-class
     * for collectors that interact with {@link ScopedLazyParameter.FactoryRoot}.
     *
     * @see #from()
     * @see #as(String)
     * @see #as(ToDisplayFunction)
     */
    private static abstract class ParameterFactoryFinisher<T,R>
    implements Function<List<T>,R> {
        @Override public R apply(List<T> values) {
            FactoryRoot<T> paramFactory = new ScopedLazyParameter.FactoryHandler<T>(
                    (T[])values.toArray(), ExtraIdDetail.factory_root)
                    .newProxy();
            return finish(paramFactory);
        }
        abstract R finish(FactoryRoot<T> parameterFactory);
    }

    /**
     * Recommended practice when parameter values are collected from a stream
     * is to use this collector, which initiates a progressive factory for
     * creating parameter that is abstracted as an instance of
     * {@link ScopedLazyParameter}, which can be stored in a static field (or
     * other context that is not reset on each repetition) so that its method
     * {@link ScopedLazyParameter#pickValue()} can be used for test
     * parametrization.
     */
    public static <T> Collector<T,?,FactoryRoot<T>> from() {
        return new CollectorImpl(new ParameterFactoryFinisher<T, FactoryRoot<T>>() {
            /**
             * This is tested by<br/>
             * {@link org.lazyparams.showcase.ToListLessCombined#oneOrTwo()}<br/>
             * {@link org.lazyparams.showcase.ToListLessCombined#individuallyCombined()}<br/>
             * {@link org.lazyparams.showcase.ToListDefaultPairwise#permutations()}<br/>
             * {@link org.lazyparams.internal.DescriptorContextGuardCreationChecks#topOfNode()}
             */
            @Override
            FactoryRoot<T> finish(FactoryRoot<T> parameterFactory) {
                return parameterFactory;
            }
        });
    }

    /**
     * @deprecated
     * For this to work it is necessary to collect from a stream that is
     * intrinsically ordered. If stream source does not define a particular
     * encounter order then it is necessary to apply
     * {@link java.util.stream.Stream#sorted(java.util.Comparator)} or
     * {@link java.util.stream.Stream#sorted()} on stream before parameter
     * value is picked by this collector.
     * <br/>
     * But don't these demands on having stream values repeatedly recreated
     * (with same encounter order) challenge the primary incentives for picking
     * parameter values from a stream? E.g. to stream a limited number of
     * more-or-less randomly selected values from a large pool of available
     * test-data.
     * <br/>
     * For such and similar situations it is probably better to stream values
     * only once, before test is started (e.g. during @BeforeAll), and have
     * them stored as a {@link ScopedLazyParameter} that can be discarded after
     * all test repetitions have completed (e.g. during @AfterAll).
     * A {@link ScopedLazyParameter} can be created from a stream by using the
     * collector of {@link #from()}, which creates a progressive factory
     * that allows some additional customizations of the manufactured parameter.
     */
    @Deprecated
    public static <T> Collector<T,?,T> as(final String parameterName) {
        return new CollectorImpl(new ParameterFactoryFinisher<T,T>() {
            @Override
            T finish(FactoryRoot<T> parameterFactory) {
                return parameterFactory
                        .withExtraIdDetails(ExtraIdDetail.named)
                        .asParameter(parameterName).pickValue();
            }
        });
    }

    /**
     * @deprecated
     * For this to work it is necessary to collect from a stream that is
     * intrinsically ordered. If stream source does not define a particular
     * encounter order then it is necessary to apply
     * {@link java.util.stream.Stream#sorted(java.util.Comparator)} or
     * {@link java.util.stream.Stream#sorted()} on stream before parameter
     * value is picked by this collector.
     * <br/>
     * But don't these demands on having stream values repeatedly recreated
     * (with same encounter order) challenge the primary incentives for picking
     * parameter values from a stream? E.g. to stream a limited number of
     * more-or-less randomly selected values from a large pool of available
     * test-data.
     * <br/>
     * For such and similar situations it is probably better to stream values
     * only once, before test is started (e.g. during @BeforeAll), and have
     * them stored as a {@link ScopedLazyParameter} that can be discarded after
     * all test repetitions have completed (e.g. during @AfterAll).
     * A {@link ScopedLazyParameter} can be created from a stream by using the
     * collector of {@link #from()}, which creates a progressive factory
     * that allows some additional customizations of the manufactured parameter.
     */
    @Deprecated
    public static <T> Collector<T,?,T> as(final ToDisplayFunction<? super T> toDisplay) {
        return new CollectorImpl(new ParameterFactoryFinisher<T,T>() {
            @Override
            T finish(FactoryRoot<T> parameterFactory) {
                return parameterFactory
                        .withExtraIdDetails(ExtraIdDetail.displayed)
                        .asParameter(toDisplay).pickValue();
            }
        });
    }

    /**
     * @see ToList#pickList( String, Object...)
     * @see Qronicly#pickValue( String, Object[])
     * @see FullyCombined#pickFullyCombined( String, Object[])
     **
     * @deprecated
     * For this to work it is necessary to collect from a stream that is
     * intrinsically ordered. If stream source does not define a particular
     * encounter order then it is necessary to apply
     * {@link java.util.stream.Stream#sorted(java.util.Comparator)} or
     * {@link java.util.stream.Stream#sorted()} on stream before parameter
     * value is picked by this collector.
     * <br/>
     * But don't these demands on having stream values repeatedly recreated
     * (with same encounter order) challenge the primary incentives for picking
     * parameter values from a stream? E.g. to stream a limited number of
     * more-or-less randomly selected values from a large pool of available
     * test-data.
     * <br/>
     * For such and similar situations it is probably better to stream values
     * only once, before test is started (e.g. during @BeforeAll), and have
     * them stored as a {@link ScopedLazyParameter} that can be discarded after
     * all test repetitions have completed (e.g. during @AfterAll).
     * A {@link ScopedLazyParameter} can be created from a stream by using the
     * collector of {@link #from()}, which creates a progressive factory
     * that allows some additional customizations of the manufactured parameter.
     */
    @Deprecated
    public static <T,R> Collector<T,?,R> as(
            final String parameterName,
            final BiFunction<String,? super T[],R> pickValueFunction,
            final T... templateArray_normallyIgnoredButEmptyStreamDefaultValueIsPossible) {
        return new CollectorImpl<T,R>(new PickValueFinisher(parameterName, pickValueFunction,
                templateArray_normallyIgnoredButEmptyStreamDefaultValueIsPossible));
    }

    /**
     * @see ToList#pickList( ToDisplayFunction, Object...)
     * @see Qronicly#pickValue( ToDisplayFunction, Object[])
     * @see FullyCombined#pickFullyCombined( ToDisplayFunction, Object[])
     **
     * @deprecated
     * For this to work it is necessary to collect from a stream that is
     * intrinsically ordered. If stream source does not define a particular
     * encounter order then it is necessary to apply
     * {@link java.util.stream.Stream#sorted(java.util.Comparator)} or
     * {@link java.util.stream.Stream#sorted()} on stream before parameter
     * value is picked by this collector.
     * <br/>
     * But don't these demands on having stream values repeatedly recreated
     * (with same encounter order) challenge the primary incentives for picking
     * parameter values from a stream? E.g. to stream a limited number of
     * more-or-less randomly selected values from a large pool of available
     * test-data.
     * <br/>
     * For such and similar situations it is probably better to stream values
     * only once, before test is started (e.g. during @BeforeAll), and have
     * them stored as a {@link ScopedLazyParameter} that can be discarded after
     * all test repetitions have completed (e.g. during @AfterAll).
     * A {@link ScopedLazyParameter} can be created from a stream by using the
     * collector of {@link #from()}, which creates a progressive factory
     * that allows some additional customizations of the manufactured parameter.
     */
    @Deprecated
    public static <T,R> Collector<T,?,R> as(
            final ToDisplayFunction<? super R> toDisplay,
            final BiFunction<ToDisplayFunction<? super R>,? super T[],R> pickValueFunction,
            final T... templateArray_normallyIgnoredButEmptyStreamDefaultValueIsPossible) {
        return new CollectorImpl<T,R>(new PickValueFinisher(toDisplay, pickValueFunction,
                templateArray_normallyIgnoredButEmptyStreamDefaultValueIsPossible));
    }

    /**
     * Functionality of this class, when being ...
     * ... used within {@link #as(String, BiFunction, Object...)} is
     *     tested by {@link org.lazyparams.showcase.ToListDefaultPairwise#oneOrPermutationOfTwo()}
     * <br/>
     * ... used within {@link #as(ToDisplayFunction, BiFunction, Object...)} is
     *     tested by {@link org.lazyparams.showcase.ToListLessCombined#permutations()}
     */
    private static class PickValueFinisher<A,T,R> implements Function<List<T>,R> {
        private final A pickValue1stArg;
        private final BiFunction<A,? super T[],R> pickValueFunction;
        private final T[] arrayTemplate;

        PickValueFinisher(A pickValue1stArg,
                BiFunction<A, ? super T[], R> pickValueFunction,
                T[] arrayTemplate) {
            this.pickValue1stArg = pickValue1stArg;
            this.pickValueFunction = pickValueFunction;
            this.arrayTemplate = null == arrayTemplate ? (T[]) new Object[1] : arrayTemplate;
        }

        @Override
        public R apply(List<T> values) {
            return pickValueFunction.apply(pickValue1stArg,
                    values.toArray(arrayTemplate.clone()));
        }
    }

    private enum ExtraIdDetail {
        factory_root, named, displayed;
    }
}
