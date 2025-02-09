/*
 * Copyright 2024-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams;

import org.lazyparams.config.Configuration;
import org.lazyparams.internal.ConfigurationContext;
import org.lazyparams.internal.Instrument;
import org.lazyparams.showcase.ScopedLazyParameter;

/**
 * This class provides the static <code>pickValue(...)</code> methods that serve as
 * the intuitive introduction to this framework by covering most of what traditional
 * parametrization solutions can offer - but with the additional benefit of
 * refactor-friendly type safety and implicit all-pairs testing out-of-the-box.
 * <br><br>
 * On top of these <code>pickValue(...)</code> methods there are
 * additional APIs on lazy parametrization available in the
 * {@link org.lazyparams.showcase showcase package}. An intention with these
 * APIs is to demonstrate how lazy parametrization allows the developer to
 * innovate new kinds of parametrization that collaborates nicely with the
 * overall testing framework (e.g. JUnit). Some examples:<ul>
 * <li>Use {@link org.lazyparams.showcase.FalseOrTrue#pickBoolean(CharSequence)}
 * to introduce a parameter with values <code>false</code> and <code>true</code>.
 * This very simple parametrization is a powerful tool to easily make a test
 * cover some of those trivial but disturbing corner-cases, for which we don't
 * want to create separate tests:
 * <pre><code>&commat;Test void userInput() {
 *     String textInput = LazyParams.pickValue("text", "foo","bar");
 *     if (FalseOrTrue.pickBoolean("with_noisy_whitespace")) {
 *         textInput = " " + textInput + " ";
 *     }
 *     // ... test continues ...
 * }
 * // With ConsoleLauncher the test could produce these results:
 * // └─ userInput() ✔
 * //    ├─ userInput text=foo ✔
 * //    ├─ userInput text=bar with_noisy_whitespace ✔
 * //    ├─ userInput text=foo with_noisy_whitespace ✔
 * //    └─ userInput text=bar ✔</code></pre>
 * </li>
 * <li>{@link org.lazyparams.showcase.FullyCombined} offers static
 * <code>pickFullyCombined(...)</code> methods for navigating all parameter
 * value combinations. A test will be repeated until all combinations are
 * executed, instead of stopping after just covering pairwise combinations.
 * These fully combined parameters can be part of a test that also facilitates
 * parameters that are combined in the default pairwise manner(!) and therewith
 * form a pocket where all parameter value combinations are evaluated - but with
 * each pocket combination being pairwise combined with other parameter values.
 * <br><br>
 * </li>
 * <li>For combinatorial parametrization of lists there is an abstract class
 * {@link org.lazyparams.showcase.ToList}, which
 * {@link org.lazyparams.showcase.ToList#pickList(String, Object...) pickList(...)}
 * methods present high-level parametrization abstractions that achieve their
 * magic with multiple technically primitive (int) parameters under-the-hood.
 * This powerful abstraction has very few (or none?) equivalents in
 * traditional parametrization solutions - but it is almost a no-brainer from
 * a lazy parametrization perspective.
 * <br>
 * This abstraction allows for many different list parametrization solutions
 * but a few particularly useful implementations are recognized and have
 * been carefully crafted to combine well with other parameters.
 * They are offered by these static factory methods:<ul>
 * <li>{@link org.lazyparams.showcase.ToList#combineOneOrTwo()}</li>
 * <li>{@link org.lazyparams.showcase.ToList#combineOneOrPermutationOfTwo()}</li>
 * <li>{@link org.lazyparams.showcase.ToList#combineElementsIndividually()}</li>
 * <li>{@link org.lazyparams.showcase.ToList#combinePermutation()}</li>
 * </ul>
 * The above implementations' ability to "combine well with other parameter"
 * make them suitable baselines for other parametrization abstractions,
 * such as this function ...
 * <pre><code>static &lt;T&gt; List&lt;T&gt; pickAtMostTwo(String listName, T... sourceValues) {
 *     return FalseOrTrue.pickBoolean("empty_" + listName)
 *             ? new ArrayList()
 *             : ToList.combineOneOrTwo().pickList(listName, sourceValues);
 * }</code></pre>
 * ... that harnesses a special empty list corner-case and otherwise relies on
 * {@link org.lazyparams.showcase.ToList#combineOneOrTwo()}.
 * </li>
 * </ul>
 *
 * @author Henrik Kaipe
 * @see <a target="_top" href="https://github.com/lazyparams/lazyparams#lazyparams">LazyParams Documentation</a>
 * @see org.lazyparams.showcase
 */
public class LazyParams {
    private LazyParams() {}

    /**
     * Used by {@link #pickValue(Enum...) pickValue(E... values)}
     */
    private static final ToDisplayFunction<Object> stringValueOf =
            new ToDisplayFunction<Object>() {
        @Override public CharSequence apply(Object value) {
            return String.valueOf(value);
        }
    };

    /**
     * @see <a target="_top" href="https://github.com/lazyparams/lazyparams#lazyparams">LazyParams Documentation</a>
     */
    public static <T> T pickValue(
            ToDisplayFunction<? super T> toDisplay,
            T[] possibleParameterValues) {
        return ScopedLazyParameter
                .from(possibleParameterValues)
                .withExtraIdDetails(ExtraIdDetail.array_displayed)
                .asParameter(toDisplay).pickValue();
    }

    /**
     * @see <a target="_top" href="https://github.com/lazyparams/lazyparams#lazyparams">LazyParams Documentation</a>
     */
    public static <T> T pickValue(
            final String parameterName, T[] possibleParamValues) {
        return ScopedLazyParameter
                .from(possibleParamValues)
                .withExtraIdDetails(ExtraIdDetail.array_named)
                .asParameter(parameterName).pickValue();
    }

    /**
     * @see <a target="_top" href="https://github.com/lazyparams/lazyparams#lazyparams">LazyParams Documentation</a>
     */
    public static <T> T pickValue(
            ToDisplayFunction<? super T> toDisplay,
            T primaryValue, T... otherValues) {
        return ScopedLazyParameter
                .from(primaryValue, otherValues)
                .withExtraIdDetails(ExtraIdDetail.varargs_displayed)
                .asParameter(toDisplay).pickValue();
    }

    /**
     * @see <a target="_top" href="https://github.com/lazyparams/lazyparams#lazyparams">LazyParams Documentation</a>
     */
    public static <T> T pickValue(
            String parameterName, T primaryValue, T... otherValues) {
        return ScopedLazyParameter
                .from(primaryValue, otherValues)
                .withExtraIdDetails(ExtraIdDetail.varargs_named)
                .asParameter(parameterName).pickValue();
    }

    /**
     * Special function to promote lazy enum parameters with minimal boiler plating.
     * To pick a parameter value from the constants of enum type <code>MyEnum</code>
     * it is sufficient to use statements like these:<pre><code>
     *   LazyParams.&lt;MyEnum&gt;pickValue()
     *   MyEnum value = LazyParams.pickValue()
     * </code></pre>
     * Parameter value toString() result will be displayed. (Default
     * {@link Enum#toString()} implementation returns constant name.)
     */
    public static <E extends Enum<E>> E pickValue(E... values) {
        E[] frozenValues = values.clone();
        Class<E> enumType = (Class<E>) values.getClass().getComponentType();
        StringBuilder sb = new StringBuilder(enumType.getName());
        for (Enum e : frozenValues) {
            sb.append('\n').append(e.ordinal()).append(e.getClass().getName());
        }
        final String idToString = sb.toString();
        final Object explicitId = new Object() {
            @Override
            public String toString() {
                return idToString;
            }
            @Override
            public boolean equals(Object obj) {
                return this.getClass().getName().equals(obj.getClass().getName())
                        && toString().equals(obj.toString());
            }
            @Override
            public int hashCode() {
                return 203 + toString().hashCode();
            }
        };
        if (0 == frozenValues.length && false == enumType.isEnum()) {
            throw new IllegalArgumentException("Not an enum type: " + enumType);
        }
        return ScopedLazyParameter
                .from(0 == frozenValues.length ? (E[])enumType.getEnumConstants() : frozenValues)
                .withExplicitParameterId(explicitId)
                .asParameter(stringValueOf).pickValue();
    }

    /**
     * To explicitly install LazyParams with this method is not strictly
     * necessary, because it will be implicitly installed anyway on initial
     * usage as soon as the very first parameter value is picked.
     * But separate installation with this method has a couple of benefits:<ul>
     * <li>
     * Installation requires a small but noticeable amount of time. If test
     * execution times are examined then isolated installation with this method
     * during static initialization of test-class is perhaps a good idea.
     * </li>
     * <li>
     * LazyParams can support multi-threaded test executions if the concurrency
     * does not effect the order by which the parameter values are picked.
     * But this only works as long as LazyParams can successfully associate a
     * parameter value pick on a child thread with its main test-execution thread.
     * If this method is used to make sure LazyParams is installed before the
     * main thread starts the test (e.g. during static
     * initialization of test-class) then there is a greater chance to
     * successfully associate the main test-execution thread with parameter
     * values that are picked from child threads.<br>
     * A situation of concern is when test is executed with a timeout that fails
     * the test if it takes too long, because this is usually supported
     * by having the actual test execute on a child thread, while the main
     * test-execution thread sits waiting for the specified amount of time.
     * </li>
     * </ul>
     * @see #uninstall()
     */
    public static void install() {
        currentScopeConfiguration();
    }

    /**
     * Temporarily uninstalls LazyParams but it will be reinstalled again on
     * next usage.
     * This function should be of no concern as long as everything works but
     * there is a small risk for the deep installation of LazyParams to end up in
     * technical conflict with some other framework. Invoking this function
     * during initialization of test-class that deals with the conflicting
     * framework could then be a way to workaround the problem.
     *
     * @see #install()
     */
    public static void uninstall() {
        Instrument.uninstall();
    }

    /**
     * Returns current test scope configuration.
     * Typically the configuration gets out of scope when current test
     * (or test-suite) execution is completed.
     * If method is used outside of any detectable test then a thread scoped
     * configuration is retrieved.
     */
    public static Configuration currentScopeConfiguration() {
        return ConfigurationContext.currentTestConfiguration();
    }

    private enum ExtraIdDetail {
        varargs_named, varargs_displayed, array_named, array_displayed;
    }
}
