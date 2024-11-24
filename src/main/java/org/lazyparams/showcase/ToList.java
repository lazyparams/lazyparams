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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.lazyparams.ToDisplayFunction;
import org.lazyparams.showcase.ScopedLazyParameter.CombiningCollector;
import org.lazyparams.showcase.ScopedLazyParameter.FactoryRoot;
import org.lazyparams.showcase.ScopedLazyParameter.Identifier;

/**
 * Abstract superclass for {@link ScopedLazyParameter.CombiningCollector} implementations
 * that achieve parametrization of lists.
 * The purpose is to have an implementation as argument to
 * {@link ScopedLazyParameter.BasicFactory#asParameter(String,CombiningCollector)} (or
 * {@link ScopedLazyParameter.BasicFactory#asParameter(ToDisplayFunction,CombiningCollector)})
 * in order to choose resulting list elements from the parameter values
 * specified when fluent parametrization was rooted with
 * {@link ScopedLazyParameter#from(Object[]) ScopedLazyParameter.from(T[])} (or
 * {@link ScopedLazyParameter#from(Object,Object...)  ScopedLazyParameter.from(T, T...)}.
 * Class offers instance methods
 * {@link #pickList(String,Object...) pickList(String,T...)} (and
 * {@link #pickList(ToDisplayFunction,Object...) pickList(ToDisplayFunction,T...)})
 * for using itself in this manner with a best practice combine strategy that
 * is applied by
 * {@link #applyRecommendedDeviationFromDefaultPairwiseCombining(ScopedLazyParameter.FactoryRoot)}.
 * <br><br>
 * Static factory methods {@link #combineOneOrTwo()},
 * {@link #combineOneOrPermutationOfTwo()}, {@link #combinePermutation()}
 * and {@link #combineElementsIndividually()}
 * are crafted to provide a menu of default implementations that cooperate well
 * with the default {@link ScopedLazyParameter.CombiningCollector.Seeds#next(int)}
 * mechanism to combine well within themselves and with other parameters.
 *
 * @author Henrik Kaipe
 */
public abstract class ToList<T> implements CombiningCollector<T,List<T>> {

    /**
     * Produces a list that contains one or two of the parameter input values.
     * If the result list has two values then their individual order will be
     * the same as how they were ordered in the original array of parameter
     * input-values.
     */
    public static <T> ToList<T> combineOneOrTwo() {
        return new ToList<T>() {
            /**
             * Implementation is quite tricky but it does preserve a strong
             * correlation between two seeded ints and array indexes for
             * elements in the resulting combined list. This strong correlation
             * is intended to help combining with other parameters, so that core
             * {@link org.lazyparams.core.Lazer} will have a chance to track
             * relations between values of different parameters.
             */
            @Override
            public List<T> applyOn(List<? extends T> inputParameterValues, Seeds combinedSeeds) {

                final int halfLength = (inputParameterValues.size() + 1) / 2;
                final int firstSinglesRoof = Math
                        .max((inputParameterValues.size() + 2) / 4, 1);

                final int major = combinedSeeds.next(
                        (inputParameterValues.size() / 2) * 2 + 1);
                final int minor = combinedSeeds.next(halfLength);

                /* Evaluate singleton list scenarios: */
                if (inputParameterValues.size() == major) {
                    return asArrayList(inputParameterValues,
                            firstSinglesRoof <= minor ? minor : minor + halfLength);
                } else if (major < firstSinglesRoof
                        || firstSinglesRoof + halfLength <= major
                        ? major % halfLength == minor
                        : halfLength + minor == inputParameterValues.size()) {
                    return asArrayList(inputParameterValues, major);
                }

                /* Two elements to list: */
                return asArrayList(inputParameterValues, major,
                        halfLength + minor == inputParameterValues.size()
                        || (halfLength + major == inputParameterValues.size()
                                && inputParameterValues.size() < 2 * halfLength
                                ? firstSinglesRoof <= minor
                                : minor < major && major - minor <= halfLength)
                        ? minor : minor + halfLength);
            }

            private List<T> asArrayList(List<? extends T> values, int... indexPicks) {
                Arrays.sort(indexPicks);
                List<T> list = new ArrayList<T>();
                for (int eachIndex : indexPicks) {
                    list.add(values.get(eachIndex));
                }
                return list;
            }
        };
    }

    /**
     * Like {@link #combineOneOrTwo()} except order is not necessarily
     * preserved on a result list with two elements. Instead a permutation of
     * the two is combined. Whether result list will keep elements as in their
     * individual order from parameter input values array or reversed is
     * determined by using an extra combine seed whenever a result list
     * [of {@link #combineOneOrTwo()}] has two elements. It will be the third
     * parameter seed (i.e. second trailing seed) and therefore will be
     * pairwise combined if number of parameter input values is greater than 3,
     * even if parameter itself is set to be fully combined on some
     * {@link CartesianProductHub}. (And with 20 or more parameter input values
     * this permutation seed will even be effectively picked as
     * {@link ScopedLazyParameter.Combiner#notCombined()}, being a victim of the
     * combine degrading that is in effect for trailing seeds as more parameter
     * input values causes higher bounds on initial two seeds, which will
     * cut away pending allowance for pairwise combining.)
     * <br>
     * If default pairwise combined with ...
     * <ul>
     * <li>... 2 or 3 element input-values then all permutations
     * will be evaluated
     * </li>
     * <li>... at least 4 and less then 20 element input-values can be expected
     * to evaluate much like {@link #combineOneOrTwo()} (but a few more value
     * combinations because of combine algorithm defects when an extra parameter
     * (in this case extra seed) is conditioned. I.e. 3rd seed only enabled when
     * result list has two elements.
     * <br>
     * Each element will occur at least once in a two-element result list that
     * has elements ordered as in parameter input-values array and at least
     * once in reversed order. Roughly half of the
     * two-element result lists will have their elements in reverse order.
     * </li>
     * <li>... 20 or more element input-values will not be pairwise combined
     * (because of seed combine degrading) and it cannot be said for certain
     * whether an individual element will occur in both a reversed and an
     * originally ordered result lists.
     * </li>
     * </ul>
     */
    public static <T> ToList<T> combineOneOrPermutationOfTwo() {
        return new ToList<T>() {
            @Override
            public List<T> applyOn(List<? extends T> inputParameterValues, Seeds combinedSeeds) {
                List<T> result = ToList.<T>combineOneOrTwo()
                        .applyOn(inputParameterValues, combinedSeeds);
                if (2 <= result.size() && 1 <= combinedSeeds.next(2)) {
                    result.add(result.remove(0));
                }
                return result;
            }
        };
    }

    /**
     * To have elements combined individually is like introducing a parameter
     * with values <code>false</code> and <code>true</code> for each
     * input parameter-value, which will be included in the result list if its
     * introduced parameter evaluates as <code>true</code>.
     */
    public static <T> ToList<T> combineElementsIndividually() {
        return new ToList<T>() {
            @Override
            public List<T> applyOn(List<? extends T> inputParameterValues, Seeds combinedSeeds) {
                List<T> result = new ArrayList<T>(inputParameterValues.size());
                for (T individualInput : inputParameterValues) {
                    if (1 == combinedSeeds.next(2)) {
                        result.add(individualInput);
                    }
                }
                return result;
            }
        };
    }

    /**
     * Unlike other ToList implementations (from {@link #combineOneOrTwo()},
     * {@link #combineOneOrPermutationOfTwo()} or 
     * {@link #combineElementsIndividually()}) this one will produce a list
     * that will always contain the exact same elements no matter how many times
     * the test is repeated. Only the element order will differ.
     * <br><br>
     * It is assumed unlikely for a permutation pick to cause failure because
     * of a certain combination with another parameter. This is a reason
     * to why default
     * {@link #applyRecommendedDeviationFromDefaultPairwiseCombining(ScopedLazyParameter.FactoryRoot)}
     * is overridden with {@link ScopedLazyParameter.Combiner#notCombined()}
     * for this {@link ToList} implementation. It will ensure each
     * element to occur once as first element in list and thereafter keep on
     * producing more permutations as long as test's other parameter
     * combinations continue to trigger repetitions. Therewith decent
     * permutation coverage is achieved but it's kept mostly independent of
     * other test parameters.
     *
     * @see InAnyOrder
     */
    public static <T> ToList<T> combinePermutation() {
        return new ToList<T>() {
            private List<T> permutateFirstOnes(List<T> firstOnes, Seeds combinedSeeds) {
                int permutedIndex = combinedSeeds.next(firstOnes.size());
                if (1 == firstOnes.size()) {
                    return new ArrayList<T>(firstOnes);
                } else {
                    List<T> result = permutateFirstOnes(
                            firstOnes.subList(1, firstOnes.size()), combinedSeeds);
                    result.add(permutedIndex, firstOnes.get(0));
                    return result;
                }
            }
            @Override
            public List<T> applyOn(final List<? extends T> inputParameterValues, Seeds combinedSeeds) {
                final int trailerIndexStart = 4;
                List<T> result = permutateFirstOnes(new AbstractList<T>() {
                    @Override
                    public T get(int index) { return inputParameterValues.get(index); }
                    @Override
                    public int size() {
                        return Math.min(trailerIndexStart, inputParameterValues.size());
                    }
                }, combinedSeeds);
                for (int trailer = trailerIndexStart;
                        trailer < inputParameterValues.size(); ++trailer) {
                    result.add(trailer - combinedSeeds.next(trailer + 1),
                            inputParameterValues.get(trailer));
                }
                return result;
            }
            @Override
            protected <V> Identifier<V,?> applyRecommendedDeviationFromDefaultPairwiseCombining(
                    FactoryRoot<V> factory) {
                return factory.notCombined();
            }
        };
    }

    @Override
    public abstract List<T> applyOn(List<? extends T> inputParameterValues, Seeds combinedSeeds);

    /**
     * Only used by {@link #pickList(String,Object...)} and
     * {@link #pickList(ToDisplayFunction,Object...)}
     * so that a recommended best practice combine strategy for this
     * {@link ToList} implementation will be applied.
     * Default implementation does nothing - and just returns its argument. I.e.
     * the default pairwise combine strategy will prevail unless this method is
     * overridden.
     * <br><br>
     * This default implementation is overridden by {@link #combinePermutation()},
     * which applies {@link ScopedLazyParameter.Combiner#notCombined()}.
     *
     * @see #combinePermutation()
     */
    protected <V> Identifier<V,?> applyRecommendedDeviationFromDefaultPairwiseCombining(
            FactoryRoot<V> factory) {
        return factory;
    }

    public <V> List<V> pickList(String parameterName, V... sourceOfListValues) {
        if (0 == sourceOfListValues.length) {
            return ScopedLazyParameter.from(new ArrayList<V>())
                    .withExtraIdDetails(ExtraIdDetail.named, getClass().getName())
                    .asParameter(parameterName).pickValue();
        } else {
            return applyRecommendedDeviationFromDefaultPairwiseCombining(
                    ScopedLazyParameter.from(sourceOfListValues))
                    .withExtraIdDetails(ExtraIdDetail.named)
                    .asParameter(parameterName, (ToList<V>)this).pickValue();
        }
    }

    public <V> List<V> pickList(
            ToDisplayFunction<? super List<V>> toDisplay, V... sourceOfListValues) {
        if (0 == sourceOfListValues.length) {
            return ScopedLazyParameter.from(new ArrayList<V>())
                    .withExtraIdDetails(ExtraIdDetail.displayed, getClass().getName())
                    .asParameter(toDisplay).pickValue();
        } else {
            return applyRecommendedDeviationFromDefaultPairwiseCombining(
                    ScopedLazyParameter.from(sourceOfListValues))
                    .withExtraIdDetails(ExtraIdDetail.displayed)
                    .asParameter(toDisplay, (ToList<V>)this).pickValue();
        }
    }

    private enum ExtraIdDetail { named, displayed; }
}
