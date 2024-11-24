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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.lazyparams.LazyParams;
import org.lazyparams.LazyParamsCoreUtil;
import org.lazyparams.ToDisplayFunction;
import org.lazyparams.config.Configuration;

/**
 * Represents a lazy parameter that is introduced to current repetition scope
 * on first usage of method {@link #pickValue()} that will return a parameter
 * value that is held until scope repeats or execution returns to parent
 * scope. This means after a parameter value was retrieved from
 * {@link #pickValue()} then {@link #pickValue()} will continue to return the
 * same value for as long as execution stays in the same repetition scope.
 * <br>
 * A special feature is how {@link #pickValue()} will also return the same value
 * as execution runs through sub-scopes of the repetition scope that introduced
 * the parameter value. This is unlike
 * {@link LazyParams LazyParams.pickValue(...)} and other best-practice APIs
 * that will always produce separate parameters for each (sub-)scope, even if
 * parameters are otherwise identical. This may seem strange given that
 * {@link LazyParams} relies on ScopedLazyParameter under the hood - but it is all
 * because it does not hold on to its ScopedLazyParameter instance, so instead
 * {@link #pickValue()} is invoked on a new instance whenever a parameter
 * value-pick is made with {@link LazyParams LazyParams.pickValue(...)}.
 * <br><br>
 * An instance of ScopedLazyParameter is created by using a fluent progressive
 * factory that is initiated by method {@link #from(Object[]) from(T[] values)}
 * or {@link #from(Object,Object[]) from(T primaryValue, T... otherValues)}).
 * The factory supports fine-grained details for creation of full-featured lazy
 * parameters and it is used under-the-hood by other parametrization APIs - e.g.
 * {@link LazyParams}, {@link FullyCombined}, {@link ToList}, {@link InAnyOrder}
 * and {@link Ensembles}.
 *
 * @see #from(Object,Object...) from(T[] primaryValue, T... otherValues)
 * @see #from(Object[]) from(T[] values)
 * @see FactoryRoot
 *
 * @author Henrik Kaipe
 */
public abstract class ScopedLazyParameter<T> {
    ScopedLazyParameter() {}

    public static <T> FactoryRoot<T> from(T primaryValue, T... otherValues) {
        Object[] possibleParamValues = new Object[otherValues.length + 1];
        possibleParamValues[0] = primaryValue;
        for (int i = 0; i < otherValues.length; ++i) {
            possibleParamValues[i+1] = otherValues[i];
        }
        return new FactoryHandler<T>((T[])possibleParamValues).newProxy();
    }

    public static <T> FactoryRoot<T> from(T[] values) {
        T[] possibleParamValues = values.clone();
        return new FactoryHandler<T>(
                possibleParamValues,
                new ToStringKey(values.getClass().getName()) {})
                .newProxy();
    }

    public abstract T pickValue();

    public interface CombiningCollector<T,C> {

        public interface Seeds { int next(int bound); }

        C applyOn(List<? extends T> parameterValues, Seeds combinedSeeds);
    }

    private static class CombiningCollectorSeedsImpl
    implements CombiningCollector.Seeds {
        final Object baseParamId;
        private int seedCount = 0;

        private int pairwiseCountDown;
        private int genericSliceDown;
        private CartesianProductHub livePocket;

        private CombiningCollectorSeedsImpl(Object baseParamId,
                int initPairwiseCountDown, int initSliceDown,
                CartesianProductHub initOptionalPocket) {
            this.baseParamId = baseParamId;
            this.pairwiseCountDown = initPairwiseCountDown;
            this.genericSliceDown = initSliceDown;
            this.livePocket = initOptionalPocket;
        }

        static CombiningCollector.Seeds launchUncombined(
                Object baseParamId, int nbrOfValues) {
            return new CombiningCollectorSeedsImpl(baseParamId, 0,
                    /* To allow some pairwise combining,
                     * for better distribution on lists: */
                    nbrOfValues - 1,
                    null);
        }
        static CombiningCollectorSeedsImpl launchCombined(
                Object baseParamId, int nbrOfValues,
                CartesianProductHub optionalCartesianPocket) {
            return new CombiningCollectorSeedsImpl(baseParamId,
                    /* Cap on pairwise: */
                    12 + nbrOfValues,
                    /* Cap on fully combined, if pocket is specified: */
                    10 + 2 * nbrOfValues,
                    optionalCartesianPocket);
        }

        @Override
        public int next(final int bound) {
            if (bound <= 0) {
                throw new IllegalArgumentException("Argument must be a positive int >= 1!");                
            }

            /*
             * Evaluate need to combine a bound factor:
             */
            for (int separateFactorBound = null != livePocket
                    ? genericSliceDown : pairwiseCountDown;

                    separateFactorBound < bound;

                    separateFactorBound = separateFactorBound < pairwiseCountDown
                    ? pairwiseCountDown : Integer.MAX_VALUE) {

                for (int factor = separateFactorBound; 2 <= factor; --factor) {
                    if (0 == bound % factor) {
                        /* Factor will be separately combined: */
                        return next(factor) + factor * next(bound/factor);
                    }
                }
            }

            if ((pairwiseCountDown -= bound) < 0 && 1 == bound) {
                /* Trivial parameter that is not combined: */
                return 0;
            }

            Object seedId = 2 <= ++seedCount
                    ? new ToStringKey("Trailing Combiner Seed", seedCount, baseParamId) {}
                    : /*Parameter Primary Seed:*/baseParamId;

            genericSliceDown /= bound;
            if (null != livePocket) {
                if (1 <= genericSliceDown) {
                    return livePocket.makeFullyCombinedIndexPick(seedId, bound);
                } else {
                    livePocket = null;
                }
            }
            return LazyParamsCoreUtil.makePick(
                    seedId,
                    0 <= pairwiseCountDown || 1 <= genericSliceDown,
                    bound);
        }
    }

    /**
     * Offers deviation from the default overall pairwise combining.
     * Either by using {@link #notCombined()} to ignore combinations and only
     * ensure each parameter value is evaluated at least once;
     * or by having parameter values fully combined over either a specific
     * {@link CartesianProductHub}, using
     * {@link #fullyCombinedOn(CartesianProductHub)}, or over the globally
     * available common hub {@link CartesianProductHub#GLOBAL}, using
     * {@link #fullyCombinedGlobally()}.
     */
    public interface Combiner<T,
            returns_Factory_with_Combiner_selected extends BasicFactory<T>>
    extends BasicFactory<T> {
        returns_Factory_with_Combiner_selected notCombined();
        returns_Factory_with_Combiner_selected fullyCombinedGlobally();
        returns_Factory_with_Combiner_selected fullyCombinedOn(CartesianProductHub fullyCombinedPocket);
    }

    /**
     * Offers ways to produce more fine-grained parameter ID.
     * LazyParams' ability to combine parameter values in a lazy pairwise manner
     * is crucially dependent on the ability to identify each parameter
     * every time it is being reintroduced in a repeated test-execution.
     * <br><br>
     * Parameter identification is achieved by producing a parameter ID
     * every time a parameter value is picked. Default is to produce a
     * parameter ID-string by combining parameter-name (or ToDisplayFunction
     * class-name) with each of its values' class-name and (if implemented)
     * toString(). (On enum name() is used instead of toString())
     * <br>
     * Thereafter the factory progression path for this particular
     * pick is combined with the ID-string to form a core parameter ID,
     * which will ultimately define the parameter. I.e. plenty of effort is
     * invested to make sure parameter value-picks such as ...<pre><code>
     * ScopedLazyParameter.from(false,true).asParameter("foo");
     * </code></pre>... will not be confused with neither ...<pre><code>
     * ScopedLazyParameter.from(false,true).notCombined().asParameter("foo")
     * </code></pre>... nor ...<pre><code>
     * LazyParams.pickValue("foo", false, true);
     * </code></pre>
     * ... where the latter only separates itself because the
     * LazyParams#pickValue(...)-methods make sure to further distinguish the
     * parameter ID of their value-picks by internal use of method
     * {@link #withExtraIdDetails(Object...)}, which purpose is to achieve
     * stronger identification by further separating a particular value-pick
     * from others.
     * <br><br>
     * If the above procedure for producing a parameter ID is unreliable
     * then {@link #withExplicitParameterId(Object)} offers a way to have it
     * completely skipped and instead use argument as parameter-ID, without
     * any further decoration (except for number-of-possible-parameter-values
     * or a potential {@link Combiner#notCombined()} progression, because they
     * are core technical properties that must not change between repetitions
     * and are therefore always implicitly applied as part of parameter-ID by
     * {@link org.lazyparams.core.Lazer} as a away to ensure robustness).
     */
    public interface Identifier<T,
            returns_Factory_with_Identifier_selected extends BasicFactory<T>>
    extends BasicFactory<T> {
        /**
         * Will cancel the default parameter-ID and instead use the specified one.
         * This can be useful for frameworks or reusable
         * test-functionality that produce very specific parameters with
         * well-defined limited sets of possible parameter-values that should
         * not be mixed up with any other test parameter. (E.g. if possible
         * parameter values are false or true, as is the case for showcase
         * feature {@link FalseOrTrue} that makes special effort to enforce
         * reliable parameter identification.)
         * <br><br>
         * It is important the parameter-ID can uniquely identify a parameter
         * repeatedly as a parameterized test is repeated. This is not
         * very hard to achieve but if this somehow goes wrong then it can
         * be quite difficult to conclude a parametrization problem as being a
         * consequence of a poorly defined parameter-ID. Therefore it is
         * usually better not to bother about this opportunity and instead
         * trust LazyParams to produce good parameter IDs with its default
         * functionality.
         * <br>
         * It would of course make sense to use this method when faced with a
         * situation where LazyParams default parameter-ID is somehow
         * unreliable. This could happen if toString() implementations
         * on parameter values for some reason are unreliable (e.g. if parameter
         * values are mutated between test method repetitions, so that their
         * toString() results are affected) but it should rarely happen.
         */
        returns_Factory_with_Identifier_selected withExplicitParameterId(Object paramId);
        /**
         * Have the default parameter ID decorated with some more details,
         * to prevent it from being mistaken for another parameter with same
         * name and values.
         * It is of course unlikely for a test to end up having multiple
         * parameters with same name and values - and this method is rarely
         * of interest for the test-method developer. However, it can be useful
         * for test frameworks or reusable test-functionality that wish to
         * introduce their own parameters and avoid confusion with
         * parameters from elsewhere.
         * <br><br>
         * This method is used internally by the best practice API of
         * {@link LazyParams} to make sure that parameter requested with ...
         * <pre><code>
         * LazyParams.pickValue("foobar", EnumType.FOO, EnumType.BAR);
         * </code></pre> ... will not be confused with ... <pre><code>
         * LazyParams.pickValue("foobar", EnumType.values())
         * </code></pre> ... in case <code>FOO</code> and <code>BAR</code> are all the
         * constants of enum <code>EnumType</code>.
         * <br>
         * Do observe how repeated usage of one of the above statements will
         * yield the same value every time during an isolated test-repetition,
         * as core {@link org.lazyparams.core.Lazer} will notice the same
         * parameter being requested repeatedly. But using both statements
         * during a single test will introduce two separate parameters, because
         * the separate functions
         * {@link LazyParams#pickValue(String,Object,Object...)
         * pickValue(String parameterName, T primaryValue, T... otherValues)}
         * and
         * {@link LazyParams#pickValue(String, Object[])
         * pickValue(String parameterName, T[] values)} (even though they
         * overload one another) will internally provide different extra ID-details.
         */
        returns_Factory_with_Identifier_selected withExtraIdDetails(Object... extraIdDetails);
    }

    /**
     * @see #qronicly()
     * @see #quietly()
     */
    public interface Silencer<T,
            returns_Factory_with_Silencer_selected extends BasicFactory<T>>
    extends BasicFactory<T> {
        /**
         * Display parameter value-pick as part of test-name only if the
         * test fails - but suppress it from test-name if the test succeeds.
         * This is handy if a test has many parameters and there are one or more
         * parameters of a technical nature that will not describe the test very
         * well in a test-report but could still be useful for a developer that
         * investigates a test failure.
         * <br>
         * (The name of this method is inspired by the tools "cronic" and
         * "chronic" that can be used for having cron job output communicated
         * only in case of error. The method name here starts with 'q',
         * because it is unrelated to cron and we also wish the IDE code
         * completion to have the method presented as an alternative on top of
         * the even more silent method {@link #quietly()}.)
         *
         * @see Qronicly
         */
        returns_Factory_with_Silencer_selected qronicly();
        /**
         * Don't display parameter value-pick as part of test-name! Please
         * consider the alternative provided by sibling method {@link #qronicly()},
         * which will display the parameter value-pick in case of test failure.
         * @see #qronicly()
         */
        returns_Factory_with_Silencer_selected quietly();
    }

    private static Map<Class<?>,Map<Class<?>,Class<? extends BasicFactory>>>
            resolveProgressionPathsFor(Class<? extends BasicFactory> progressiveFactoryType) {
        Map<Class<?>,Class<? extends BasicFactory>> featureProgressions =
                new HashMap<Class<?>,Class<? extends BasicFactory>>();
        Map<Class<?>,Map<Class<?>,Class<? extends BasicFactory>>> factoryType2featureProgressions =
                new HashMap<Class<?>,Map<Class<?>,Class<? extends BasicFactory>>>();

        for (Type featureCandidate : progressiveFactoryType.getGenericInterfaces()) {
            if (featureCandidate instanceof ParameterizedType) {
                ParameterizedType feature = (ParameterizedType)featureCandidate;
                Type[] featureTypeParams = feature.getActualTypeArguments();
                if (2 == featureTypeParams.length
                        && featureTypeParams[1] instanceof ParameterizedType) {
                    Class<? extends BasicFactory> progression =
                            ((Class)((ParameterizedType)featureTypeParams[1]).getRawType())
                            .asSubclass(BasicFactory.class);
                    if (false == factoryType2featureProgressions.containsKey(progression)) {
                        factoryType2featureProgressions.putAll(
                                resolveProgressionPathsFor(progression));
                    }
                    featureProgressions.put(
                            (Class<?>)feature.getRawType(), progression);
                }
            }
        }
        factoryType2featureProgressions.put(progressiveFactoryType,
                featureProgressions.isEmpty()
                ? Collections.<Class<?>,Class<? extends BasicFactory>>
                        singletonMap(progressiveFactoryType, BasicFactory.class)
                : Collections.unmodifiableMap(featureProgressions));
        return Collections.unmodifiableMap(factoryType2featureProgressions);
    }

    static class FactoryHandler<T> implements InvocationHandler, FactoryRoot<T> {

        static final Map<Class<?>,Map<Class<?>,Class<? extends BasicFactory>>>
                factoryProgressionPaths = resolveProgressionPathsFor(FactoryRoot.class);

        private final T[] paramValues;
        private final Class<? extends BasicFactory> progressiveFactoryType;

        private boolean combined = true;
        private CartesianProductHub pocket = null;
        private DisplayVerbosity verbosity = DisplayVerbosity.VERBOSE;
        private Object explicitParamId = null;
        private final List<Object> extraIdDetails;

        FactoryHandler(T[] paramValues, Object... extraIdDetails) {
            this.paramValues = paramValues;
            this.extraIdDetails = null == extraIdDetails
                   ? Collections.emptyList() : Arrays.asList(extraIdDetails);
            progressiveFactoryType = FactoryRoot.class;
        }

        private FactoryHandler(FactoryHandler<T> root, Class<?> appliedFeatureType) {
            progressiveFactoryType = factoryProgressionPaths
                    .get(root.progressiveFactoryType).get(appliedFeatureType);
            paramValues = root.paramValues;
            combined = root.combined;
            pocket = root.pocket;
            verbosity = root.verbosity;
            explicitParamId = root.explicitParamId;
            extraIdDetails = new ArrayList<Object>(root.extraIdDetails);
        }

        <FactoryProxy extends BasicFactory<T>> FactoryProxy newProxy() {
            return (FactoryProxy) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[] {progressiveFactoryType}, this);
        }

        private Object invoke(Method method, Object[] args) throws Exception {
            Class<?> appliedFeatureType = method.getDeclaringClass();
            if (BasicFactory.class == appliedFeatureType
                    || false == BasicFactory.class.isAssignableFrom(appliedFeatureType)) {
                return method.invoke(this, args);
            } else {
                FactoryHandler next = new FactoryHandler(this, appliedFeatureType);
                next.extraIdDetails.add(method.getName());
                method.invoke(next, args);
                return next.newProxy();
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable {
            try {
                return invoke(method, args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }

        /* **** Basic Factory stuff ... *****/
        private Object parameterId(List<T> valuesInOrder,
                String combinerDetail, ToDisplayFunction<?> toDisplayDetail) {
            if (null != explicitParamId) {
                extraIdDetails.clear();
                return explicitParamId;
            }

            List<Object> buildIdLogPart = new ArrayList<Object>(extraIdDetails);
            buildIdLogPart.add(combined);
            buildIdLogPart.add(verbosity);
            buildIdLogPart.add(valuesInOrder.toArray());
            buildIdLogPart.add(toDisplayDetail.getClass());
            if (null != pocket) {
                buildIdLogPart.add(pocket);
            }
            IdDetail idLogPart = new IdDetail(buildIdLogPart.toArray());

            if (BasicToDisplayFunction.class == toDisplayDetail.getClass()) {
                String idToString = ((BasicToDisplayFunction)toDisplayDetail).parameterName
                        + "(" + valuesInOrder.size() + " values)";
                return new ToStringKey(idToString, combinerDetail, idLogPart) {};
            } else {
                return new ToStringKey(combinerDetail, idLogPart) {};
            }
        }

        @Override
        public ScopedLazyParameter<T> asParameter(final ToDisplayFunction<? super T> toDisplay) {
            return asParameter(toDisplay, new CombiningCollector<T,T>() {
                @Override
                public T applyOn(List<? extends T> parameValues,
                        CombiningCollector.Seeds combinedSeeds) {
                    return parameValues.get(combinedSeeds.next(parameValues.size()));
                }
            });
        }

        @Override
        public ScopedLazyParameter<T> asParameter(String parameterName) {
            return asParameter(new BasicToDisplayFunction<T>(parameterName));
        }

        @Override
        public <C> ScopedLazyParameter<C> asParameter(String parameterName,
                CombiningCollector<? super T, C> combiningCollector) {
            return asParameter(
                    new BasicToDisplayFunction<C>(parameterName),
                    combiningCollector);
        }

        @Override
        public <C> ScopedLazyParameter<C> asParameter(
                final ToDisplayFunction<? super C> toDisplay,
                final CombiningCollector<? super T, C> combiningCollector) {
            final List<T> valuesOnList = new ArrayList<T>(paramValues.length);
            Collections.addAll(valuesOnList, paramValues);
            final Object paramId = parameterId(valuesOnList,
                    combiningCollector.getClass().getName(), toDisplay);
            final Object scopeKey = new Object();

            return new ScopedLazyParameter<C>() {
                @Override
                public C pickValue() {
                    Configuration scopedConf = LazyParams.currentScopeConfiguration();
                    C picked = scopedConf.getScopedCustomItem(scopeKey);
                    if (null != picked) {
                        return picked;
                    }
                    picked = combiningCollector.applyOn(valuesOnList, combined
                            ? CombiningCollectorSeedsImpl.launchCombined(paramId, valuesOnList.size(), pocket)
                            : CombiningCollectorSeedsImpl.launchUncombined(paramId, valuesOnList.size()));
                    scopedConf.setScopedCustomItem(scopeKey, picked);
                    verbosity.display(paramId, toDisplay, picked);
                    return picked;
                }
            };
        }

        /* **** Feature tweakers ... *****/
        @Override
        public Identifier_and_Silencer_Factory<T> notCombined() {
            combined = false;
            return null;
        }
        @Override
        public Identifier_and_Silencer_Factory<T> fullyCombinedGlobally() {
            return fullyCombinedOn(CartesianProductHub.GLOBAL);
        }
        @Override
        public Identifier_and_Silencer_Factory<T> fullyCombinedOn(
                CartesianProductHub fullyCombinedPocket) {
            pocket = fullyCombinedPocket;
            combined = true;//to be robust if something breaks elsewhere
            return null;
        }
        @Override
        public Combiner_and_Identifier_Factory<T> qronicly() {
            verbosity = DisplayVerbosity.QRONIC;
            return null;
        }
        @Override
        public Combiner_and_Identifier_Factory<T> quietly() {
            verbosity = DisplayVerbosity.QUIET;
            return null;
        }
        @Override
        public Combiner_and_Silencer_Factory<T> withExplicitParameterId(Object paramId) {
            explicitParamId = paramId;
            extraIdDetails.clear();
            return null;
        }
        @Override
        public Combiner_and_Silencer_Factory<T> withExtraIdDetails(Object... extraIdDetails) {
            Collections.addAll(this.extraIdDetails, extraIdDetails);
            return null;
        }
    }

    static class IdDetail implements Callable<Object> {
        private final Object detail;

        IdDetail(Object detail, Map<Object,Object>... log) {
            this.detail = null == detail
                    || detail instanceof IdDetail[]
                    || false == detail.getClass().isArray()
                    ? detail
                    : asArrayOfIdDetails(detail,log);
        }

        private static IdDetail[] asArrayOfIdDetails(
                Object detailDataArray, Map<Object,Object>... log) {
            List<IdDetail> idDetails = new ArrayList<IdDetail>();
            collectDetailArray(detailDataArray, idDetails,
                    null != log && 1 <= log.length
                    ? log[0] : new IdentityHashMap<Object, Object>());
            return idDetails.toArray(new IdDetail[0]);
        }
        private static void collectDetailArray(Object detailDataArray,
                List<IdDetail> idDetails, Map<Object,Object> log) {
            log.put(detailDataArray,"");
            idDetails.add(new IdDetail(detailDataArray.getClass().getName()));
            for (int i = 0, length = Array.getLength(detailDataArray); i < length; ++i) {
                Object nextDetail = Array.get(detailDataArray, i);
                idDetails.add(log.containsKey(nextDetail)
                        ? new IdDetail(log.size()) {} : new IdDetail(nextDetail, log));
            }
        }

        /**
         * This is here just to make sure there is a way to compare
         * {@link IdDetail} instances from separate classloaders.
         * I.e. there is a need to acquire the detail object over a
         * jdk API when comparing instances from separate classloaders.
         */
        @Override public Object call() {
            return detail;
        }

        @Override
        public String toString() {
            return "IdDetail on " + (null == detail ? null
                    : detail.getClass().getName())
                    + (char)0 + (
                            detail instanceof Object[]
                            ? Arrays.deepToString((Object[]) detail)
                            : valueAsText()
                    );
        }

        private String valueAsText() {
            try {
                if (null == detail) {
                    return null;
                }
                Class<?> detailClass = detail.getClass();
                if (detail instanceof Enum) {
                    return ((Enum<?>)detail).name();
                } else if (false == detailClass.isArray()
                        && Object.class != detailClass
                        .getMethod("toString").getDeclaringClass()) {
                    return detail.toString();
                } else {
                    return null;
                }
            } catch (NoSuchMethodException toStringMustAlwaysBe) {
                throw new Error(toStringMustAlwaysBe);
            }
        }

        static Class<?> classOf(Object dtl) {
            return dtl instanceof Class ? (Class<?>)dtl : dtl.getClass();
        }

        /**
         * Workaround to preserve equality when a class definition has
         * multiple classes that are loaded on separate class loaders. */
        private int hashCodeOfDetailClass() {
            Class c = classOf(detail);
            String className = c.getName();
            int hash = 310709;
            /* Class definition class-name is same on separate class loaders,
             * unless it is a synthetic class: */
            if (c.isSynthetic()) {
                /* This is a relaxed hashCode implementation: */
                hash *= 67;
                hash += 212;
                className = reliablePartOfSyntheticClassName(className);
            }
            hash += className.hashCode();
            return hash;
        }

        private static String reliablePartOfSyntheticClassName(String className) {
            int dollarIndex = className.indexOf('$');
            return 0 <= dollarIndex
                    ? className.substring(0, dollarIndex)
                    : className;
        }

        @Override
        public int hashCode() {
            int hash = 3 * 59;
            if (null == detail) {
                return hash;
            }
            hash += hashCodeOfDetailClass();
            if (classOf(detail).isSynthetic()) {
                return hash;
            }
            hash *= 59;
            String textValue = valueAsText();
            if (null != textValue) {
                hash += textValue.hashCode();
            } else if (detail.getClass().isArray()) {
                hash += Arrays.deepHashCode((Object[])detail);
            } else {
                hash += detail.hashCode();
            }
            return hash;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;

            } else if (null == obj) {
                return false;

            } else if (false == getClass().getName().equals(obj.getClass().getName())) {
                return false;

            } else {
                try {
                    return detailEquals(((Callable<Object>)obj).call());
                } catch (Exception mustNeverHappen) {
                    throw new Error(mustNeverHappen);
                }
            }
        }
        private boolean detailEquals(final Object otherDetail) {
            if (detail == otherDetail) {
                return true;

            } else if (null == detail || null == otherDetail) {
                return false;

            } else if (classOf(detail) != classOf(otherDetail)) {
                Class<?> classOnThis = classOf(detail),
                        classOnOther = classOf(otherDetail);
                if (classOnThis.getClassLoader() == classOnOther.getClassLoader()) {
                    return false;
                } else if (classOnThis.isSynthetic()) {
                    return classOnOther.isSynthetic()
                            && reliablePartOfSyntheticClassName(classOnThis.getName())
                            .equals(reliablePartOfSyntheticClassName(classOnOther.getName()));
                } else if (classOnOther.isSynthetic()
                        || false == classOnThis.getName().equals(classOnOther.getName())) {
                    return false;
                }
            }
            if (detail instanceof Object[]) {
                return otherDetail instanceof Object[]
                        && Arrays.deepEquals((Object[])detail, (Object[])otherDetail);
            }

            /*
             * Reach here if both details have same type
             * but are possibly non-synthetic from different classloaders:
             */
            IdDetail otherAsIdDetail = new IdDetail(otherDetail);
            if (hashCode() != otherAsIdDetail.hashCode()) {
                return false;
            }
            String textValue = valueAsText();
            if (null != textValue) {
                return textValue.equals(otherAsIdDetail.valueAsText());

            } else if (null != otherAsIdDetail.valueAsText()) {
                return false;

            } else if (detail.getClass().isSynthetic()) {
                /* Intend to capture dynamicly compiled lambda extressions: */
                return otherDetail.getClass().isSynthetic();

            } else {
                /* Last chance with slim hope for equality ...*/
                return detail.equals(otherDetail);
            }
        }
    }

    public interface BasicFactory<T> {
        ScopedLazyParameter<T> asParameter(String parameterName);
        ScopedLazyParameter<T> asParameter(ToDisplayFunction<? super T> toDisplay);
        <C> ScopedLazyParameter<C> asParameter(String parameterName,
                CombiningCollector<? super T,C> combiningCollector);
        <C> ScopedLazyParameter<C> asParameter(ToDisplayFunction<? super C> toDisplay,
                CombiningCollector<? super T,C> combiningCollector);
    }

    /* **********************************************************************
     * Below are definitions for the fluent factory progression interfaces
     * that secure the progressive fluent factory patterns for creating
     * well-defined ScopedLazyParameter instances.
     *
     * These interface definitions are not edited manually!
     * Instead they are generated by script
     * "explode_factory_progression_path_definitions.pl".
     * **************************************************/

    /**@see BasicFactory
     * @see Identifier
     * @see Silencer */
    public interface Identifier_and_Silencer_Factory<T> extends BasicFactory<T>,
            Identifier<T, Silencer<T, BasicFactory<T>>>,
            Silencer<T, Identifier<T, BasicFactory<T>>> {}
    /**@see BasicFactory
     * @see Combiner
     * @see Silencer */
    public interface Combiner_and_Silencer_Factory<T> extends BasicFactory<T>,
            Combiner<T, Silencer<T, BasicFactory<T>>>,
            Silencer<T, Combiner<T, BasicFactory<T>>> {}
    /**@see BasicFactory
     * @see Combiner
     * @see Identifier */
    public interface Combiner_and_Identifier_Factory<T> extends BasicFactory<T>,
            Combiner<T, Identifier<T, BasicFactory<T>>>,
            Identifier<T, Combiner<T, BasicFactory<T>>> {}

    /**
     * @see #from(Object[]) from(T[])
     * @see #from(Object, Object...) from(T, T...)
     * 
     * @see BasicFactory
     * @see Combiner
     * @see Identifier
     * @see Silencer */
    public interface FactoryRoot<T> extends BasicFactory<T>,
            Combiner<T, Identifier_and_Silencer_Factory<T>>,
            Identifier<T, Combiner_and_Silencer_Factory<T>>,
            Silencer<T, Combiner_and_Identifier_Factory<T>> {}
}
