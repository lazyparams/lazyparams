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
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;

import org.lazyparams.ToDisplayFunction;
import org.lazyparams.showcase.ScopedLazyParameter.BasicFactory;
import org.lazyparams.showcase.ScopedLazyParameter.Combiner;
import org.lazyparams.showcase.ScopedLazyParameter.Silencer;

/**
 * @see <a href="https://en.wikipedia.org/wiki/Musical_ensemble">Musical ensemble</a>
 *
 * @author Henrik Kaipe
 */
public class Ensembles {
    private Ensembles() {}

    /**
     * Wraps a tuple value option and provides it with a {@link #toString()}
     * that is also used for {@link #equals(Object)} and {@link #hashCode()}.
     * The {@link #toString()} is composed of each argument class-name and
     * #toString() result, if implemented. The default implementation
     * on class Object is ignored!
     * This makes equality on ensemble parameter value individual arguments more
     * relaxed than the pretty ardent view on equality that is applied elsewhere
     * by LazyParams.
     * E.g. no two instances of type Object are ever equal - and therefore this
     * will fail:
     * <pre><code>
     *   var foo = LazyParams.pickValue("foo", new Object(), new Object());
     * </code></pre>
     * But with ensembles there is a way to have this accepted by not specifying
     * parameter name for the argument that does not implement equality
     * sufficiently:<pre><code>
 var foo = Ensembles
     .use(new Object(), "1st")
     .or(new Object(), "2nd")
     .asLazyDuo(null, "foo")
     .applyOn((data,name) -> data);
 </code></pre>
     * The above will be accepted without inconsistency issues on repetition
     * because class Object does only have default implementation of #toString(),
     * which is ignored by {@link Tuple#toString()} and
     * {@link Tuple#equals(Object)}. And it will not be considered by core,
     * because the {@link Duo.EnsembleOptions#asLazyDuo(String,String)} doesn't
     * specify parameter-name for first argument, so that its #toString() value
     * will not be considered downstream when {@link ScopedLazyParameter}
     * composes parameter ID.
     */
    private static final class Tuple {
        private final Object[] argsOption;
        private final String toString;

        Tuple(Object[] argsOption) {
            this.argsOption = argsOption;
            StringBuilder sb = new StringBuilder("Tuple-Size=").append(argsOption.length);
            char nextSeparator = '[';
            for (Object eachArg : argsOption) {
                sb.append(nextSeparator); nextSeparator = ',';
                if (null == eachArg) {
                    sb.append(eachArg);
                    continue;
                }
                Class<?> argType = eachArg.getClass();
                sb.append(argType.getName()).append((char)0);
                try {
                    if (eachArg instanceof Object[]) {
                        sb.append(Arrays.deepToString((Object[]) eachArg));
                    } else if (argType.isArray()) {
                        sb.append(Arrays.deepToString(new Object[] {eachArg}));
                    } else if (Object.class
                            == argType.getMethod("toString").getDeclaringClass()) {
                        sb.append(".no tostring.");
                    } else {
                        sb.append(eachArg);
                    }
                } catch (NoSuchMethodException mustNeverHappen) {
                    throw new Error(mustNeverHappen);
                }
            }
            toString = sb.append(']').toString();
        }
        @Override public String toString() {return toString;}

        @Override
        public int hashCode() {
            return 47 * 7 + toString.hashCode();
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (getClass() != obj.getClass()) {
                return false;
            }
            return toString.equals(((Tuple)obj).toString);
        }
    }

    private static Method resolveApplyOnMethodFor(Class<?> ensembleType) {
        for (Method candidate : ensembleType.getDeclaredMethods()) {
            if ("applyOn".equals(candidate.getName())) {
                return candidate;
            }
        }
        throw new NoSuchMethodError("Available methods are "
                + Arrays.toString(ensembleType.getDeclaredMethods()));
    }

    private static Object progress(Method method, final Object[] progressArgs,
            final TupleFunction recordFactory, final BasicFactory<Tuple> currentProgression)
    throws Exception {
        Class<?> applied = method.getDeclaringClass();

        if (false == BasicFactory.class.isAssignableFrom(applied)) {
            return method.invoke(currentProgression, progressArgs);

        } else if (applied != BasicFactory.class
                && applied.isAssignableFrom(ScopedLazyParameter.FactoryRoot.class)) {
            final BasicFactory<Tuple> nextProgression =
                    (BasicFactory<Tuple>) method.invoke(currentProgression, progressArgs);
            return Proxy.newProxyInstance(
                    BasicFactory.class.getClassLoader(),
                    resolveInterfaces(nextProgression.getClass()).toArray(new Class[0]),
                    new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    try {
                        return progress(method, args, recordFactory, nextProgression);
                    } catch (InvocationTargetException ex) {
                        throw ex.getTargetException();
                    }
                }
            });
        }

        final BasicFactory parameterFactory = new BasicFactory() {
            @Override public ScopedLazyParameter asParameter(String parameterName) {
                return asParameter(new BasicToDisplayFunction(parameterName));
            }
            @Override public ScopedLazyParameter asParameter(ToDisplayFunction toDisplay) {
                return currentProgression.asParameter(toDisplay,
                        new ScopedLazyParameter.CombiningCollector<Tuple,Object>() {
                            @Override
                            public Object applyOn(List<? extends Tuple> parameterValues,
                                    ScopedLazyParameter.CombiningCollector.Seeds combinedSeeds) {
                                return recordFactory.applyUnchecked(parameterValues
                                        .get(combinedSeeds.next(parameterValues.size())));
                            }
                        });
            }
            @Override public ScopedLazyParameter asParameter(String parameterName,
                    ScopedLazyParameter.CombiningCollector combiningCollector) {
                return asParameter(new BasicToDisplayFunction(parameterName), combiningCollector);
            }
            @Override public ScopedLazyParameter asParameter(ToDisplayFunction toDisplay,
                    final ScopedLazyParameter.CombiningCollector combiningCollector) {
                return currentProgression.asParameter(toDisplay,
                        new ScopedLazyParameter.CombiningCollector<Tuple,Object>() {
                            List<Object> recordsCache;
                            @Override
                            public Object applyOn(List<? extends Tuple> parameterValues,
                                    ScopedLazyParameter.CombiningCollector.Seeds combinedSeeds) {
                                if (null == recordsCache) {
                                    recordsCache = new ArrayList<Object>(parameterValues.size());
                                    for (Tuple eachTuple : parameterValues) {
                                        recordsCache.add(recordFactory.applyUnchecked(eachTuple));
                                    }
                                }
                                return combiningCollector.applyOn(recordsCache, combinedSeeds);
                            }
                        });
            }
        };
        if (BasicFactory.class == applied) {
            return method.invoke(parameterFactory, progressArgs);

        } else if (method.getName().equals("asArgumentsTo")) {
            final Method recordFactoryMethod = method
                    .getParameterTypes()[0].getDeclaredMethods()[0];
            final TupleFunction remappedRecordFactory = new TupleFunction() {
                @Override Object apply(Tuple tuple) throws Throwable {
                    try {
                        return recordFactoryMethod
                                .invoke(progressArgs[0], tuple.argsOption);
                    } catch (InvocationTargetException ex) {
                        throw ex.getTargetException();
                    }
                }
            };
            return Proxy.newProxyInstance(
                    BasicFactory.class.getClassLoader(),
                    new Class[] {ScopedLazyParameter.FactoryRoot.class},
                    new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    try {
                        return progress(method, args, remappedRecordFactory, currentProgression);
                    } catch (InvocationTargetException ex) {
                        throw ex.getTargetException();
                    }
                }
            });
        } else if (method.getName().equals("asParameter")) {
            /*The ensemble specific overload of "asParameter" with multiple
             * string arguments - one for each participant in ensemble pick ...*/
            final Class<?> ensembleType = method.getDeclaringClass().getDeclaringClass();
            return parameterFactory.asParameter(new ToDisplayFunction() {
                @Override public CharSequence apply(Object ensemble) {
                    try {
                        return (CharSequence) resolveApplyOnMethodFor(ensembleType).invoke(ensemble,
                                newToDisplayFunctionForParameterNames(ensembleType, progressArgs));
                    } catch (Exception ex) {
                        throw Ensembles.<RuntimeException>unchecked(
                                ex instanceof InvocationTargetException
                                ? ((InvocationTargetException)ex).getTargetException()
                                : ex);
                    }
                }
            });

        } else if (method.getName().startsWith("asLazy")) {
            final Class<?> ensembleType = method.getReturnType();
            final Callable<?> ensemblePicker = 1 == progressArgs.length
                    && (progressArgs[0] instanceof String || null == progressArgs[0])
                    ? new Callable<Object>() {
                @Override public Object call() {
                    return parameterFactory.asParameter((String)progressArgs[0]);
                }
            }       : new Callable<Object>() {
                <F> F newEnsembleToDisplayFunction(final Class<F> ensembleFunctionType) {
                    return 1 == progressArgs.length ? new Handler() {
                        @Override
                        Object invoke(String name, Object[] tuplePick) throws Exception {
                            if (1 == progressArgs.length) {
                                return ensembleFunctionType.getDeclaredMethods()[0]
                                        .invoke(progressArgs[0], tuplePick);
                            } else {
                                assert progressArgs.length == tuplePick.length :
                                        "Number of parameter names " + Arrays.toString(progressArgs)
                                        + " must be same as tuple length: " + tuplePick.length;
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < progressArgs.length; ++i) {
                                    sb.append(", ").append(progressArgs[i])
                                            .append('=').append(tuplePick[i]);
                                }
                                return sb.subSequence(2, sb.length());
                            }
                        }
                    }.asHandlerFor(ensembleFunctionType)
                    : (F) newToDisplayFunctionForParameterNames(ensembleType, progressArgs);
                }
                @Override
                public Object call() throws Exception {
                    return parameterFactory.asParameter(new ToDisplayFunction() {
                        @Override public CharSequence apply(Object ensemble) {
                            Method applier = resolveApplyOnMethodFor(ensembleType);
                            Object ensemble2display = newEnsembleToDisplayFunction(
                                    applier.getParameterTypes()[0]);
                            try {
                                return (CharSequence)
                                        applier.invoke(ensemble, ensemble2display);
                            } catch (Exception ex) {
                                throw Ensembles.<RuntimeException>unchecked(
                                        ex instanceof InvocationTargetException
                                        ? ((InvocationTargetException)ex).getTargetException()
                                        : ex);
                            }
                        }
                    });
                }
            };

            return Proxy.newProxyInstance(
                    ensembleType.getClassLoader(), new Class[] {ensembleType},
                    new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    try {
                        ScopedLazyParameter hotParameter =
                                (ScopedLazyParameter) ensemblePicker.call();
                        return method.invoke(hotParameter.pickValue(), args);
                    } catch (Exception ex) {
                        throw Ensembles.<RuntimeException>unchecked(
                                ex instanceof InvocationTargetException
                                ? ((InvocationTargetException)ex).getTargetException()
                                : ex);
                    }
                }
            });
        }

        throw new UnsupportedOperationException("" + method);
    }

    private static Collection<Class<?>> resolveInterfaces(Class<?> progressionType) {
        if (Object.class == progressionType) {
            return new HashSet<Class<?>>();
        }
        Collection<Class<?>> interfaces;
        if (progressionType.isInterface()) {
            interfaces = new ArrayList<Class<?>>();
            interfaces.add(progressionType);
        } else {
            interfaces = resolveInterfaces(progressionType.getSuperclass());
        }
        for (Class<?> eachExtendedInterface : progressionType.getInterfaces()) {
            interfaces.addAll(resolveInterfaces(eachExtendedInterface));
        }
        return interfaces;
    }

    private static Object newToDisplayFunctionForParameterNames(
            Class<?> ensembleType, final Object[] parameterNames) {
        final Method applier = resolveApplyOnMethodFor(ensembleType);
        return new Handler() {
            @Override Object invoke(String name, Object[] tuplePick) throws Throwable {
                assert parameterNames.length == tuplePick.length
                        : "Number of parameter names " + Arrays.toString(parameterNames)
                        + " must be same as tuple length: " + tuplePick.length;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < parameterNames.length; ++i) {
                    if (null != parameterNames[i]) {
                        sb.append(" ").append(parameterNames[i])
                                .append('=').append(toString(tuplePick[i]));
                    }
                }
                return 1 <= sb.length() ? sb.subSequence(1, sb.length()) : "";
            }
            Object toString(Object tupleElement) {
                if (null == tupleElement || false == tupleElement.getClass().isArray()) {
                    return tupleElement;
                } else if (tupleElement.getClass().getComponentType().isPrimitive()) {
                    int length = Array.getLength(tupleElement);
                    Object[] elements = new Object[length];
                    for (int i = 0; i < length; ++i) {
                        elements[i] = Array.get(tupleElement, i);
                    }
                    return Arrays.deepToString(elements);
                } else {
                    return Arrays.deepToString((Object[])tupleElement);
                }
            }
        }.asHandlerFor(applier.getParameterTypes()[0]);
    }

    private static <O> O newRecordOptions(final Class<O> optionsType,
            final TupleFunction recordFactory, Tuple[] parentArgsOptions, Object[] nextArgsOption) {
        Tuple nextOption = new Tuple(nextArgsOption);

        final Tuple[] argsOptions;
        if (null == parentArgsOptions) {
            argsOptions = new Tuple[] {nextOption};
        } else {
            argsOptions = Arrays.copyOf(parentArgsOptions, parentArgsOptions.length + 1);
            argsOptions[argsOptions.length - 1] = nextOption;
        }

        return optionsType.cast(Proxy.newProxyInstance(
                optionsType.getClassLoader(), new Class[] {optionsType},
                new InvocationHandler() {
            Object invoke(Method method, Object[] args) throws Exception {
                if (method.getName().equals("or")) {
                    return newRecordOptions(optionsType, recordFactory, argsOptions, args);
                } else {
                    return progress(method, args, recordFactory,
                            ScopedLazyParameter.from(argsOptions));
                }
            }
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                try {
                    return invoke(method, args);
                } catch (InvocationTargetException ex) {
                    throw ex.getTargetException();
                }
            }
        }));
    }

    private static <O> O newRecordOptions(
            Class<O> optionsType, Class<?> ensembleType, Object... firstArgsOption) {
        return newRecordOptions(optionsType, newEnsembleFactory(ensembleType), null, firstArgsOption);
    }

    private static <ArgRoot,F> ArgRoot newArgumentsRoot(
            Class<ArgRoot> rootType, final Class<F> recordFactoryType, final F recordFactory) {
        final Class<?> optionsType = rootType.getDeclaredMethods()[0].getReturnType();
        return new Handler() {
            @Override Object invoke(String name, Object[] args) throws Throwable {
                if (false == "use".equals(name)) {
                    throw new IllegalStateException(
                            "Unexpection invocation " + name + Arrays.asList(args));
                }
                return newRecordOptions(optionsType, new TupleFunction() {
                    @Override Object apply(Tuple tuple) throws Exception {
                        return recordFactoryType.getDeclaredMethods()[0]
                                .invoke(recordFactory, tuple.argsOption);
                    }
                }, null, args);
            }
        }.asHandlerFor(rootType);
    }

    private static TupleFunction newEnsembleFactory(final Class<?> ensembleType) {
        return new TupleFunction() {
            @Override Object apply(final Tuple tuple) {
                return Proxy.newProxyInstance(
                        ensembleType.getClassLoader(),
                        new Class[] {ensembleType},
                        new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args)
                    throws Throwable {
                        try {
                            if ("toString".equals(method.getName())) {
                                return Arrays.toString(tuple.argsOption);
                            } else if (method.getDeclaringClass() == ensembleType) {
                                return method.getParameterTypes()[0].getDeclaredMethods()[0]
                                        .invoke(args[0], tuple.argsOption);
                            } else {
                                return method.invoke(this, args);
                            }
                        } catch (InvocationTargetException ex) {
                            throw ex.getTargetException();
                        }
                    }
                });
            }
        };
    }

    private static abstract class TupleFunction {

        abstract Object apply(Tuple tuple) throws Throwable;

        Object applyUnchecked(Tuple tuple) {
            try {
                return apply(tuple);
            } catch (InvocationTargetException ex) {
                throw Ensembles.<RuntimeException>unchecked(ex.getTargetException());
            } catch (Throwable ex) {
                throw Ensembles.<RuntimeException>unchecked(ex);
            }
        }
    }

    private static <E extends Throwable> E unchecked(Throwable ex) throws E {
        throw (E)ex;
    }

    @FunctionalInterface
    public interface GroupFunction<T,R,E extends Throwable> {
        R apply(List<T> arguments) throws E;
    }

    /* ************************************************************************
     * Below are the Ensembles API definitions, which consist of
     * fluent factory methods and their interface-definitions. All
     * are implemented by using the private functionality above.
     * They enable intuitive and type-safe parameterization of arguments
     * to functions that have between 2 and 10 parameters.
     *
     * Code below this point is not edited manually!
     * Instead it is generated by script "generate-ensembles-api.pl".
     * ************************************************************************/

    public static <T,U,R> Duo.ArgumentOptionsRoot<T,U,R>
            asArgumentsTo(Duo.Function<T,U,R,?> recordFactory) {
        return newArgumentsRoot(Duo.ArgumentOptionsRoot.class,
                Duo.Function.class, recordFactory);
    }
    public static <T,U> Duo.EnsembleOptions<T,U>
            use(T t, U u) {
        return newRecordOptions(Duo.EnsembleOptions.class, Duo.class, t,u);
    }

    public static <T,U,V,R> Trio.ArgumentOptionsRoot<T,U,V,R>
            asArgumentsTo(Trio.Function<T,U,V,R,?> recordFactory) {
        return newArgumentsRoot(Trio.ArgumentOptionsRoot.class,
                Trio.Function.class, recordFactory);
    }
    public static <T,U,V> Trio.EnsembleOptions<T,U,V>
            use(T t, U u, V v) {
        return newRecordOptions(Trio.EnsembleOptions.class, Trio.class, t,u,v);
    }

    public static <T,U,V,W,R> Quartet.ArgumentOptionsRoot<T,U,V,W,R>
            asArgumentsTo(Quartet.Function<T,U,V,W,R,?> recordFactory) {
        return newArgumentsRoot(Quartet.ArgumentOptionsRoot.class,
                Quartet.Function.class, recordFactory);
    }
    public static <T,U,V,W> Quartet.EnsembleOptions<T,U,V,W>
            use(T t, U u, V v, W w) {
        return newRecordOptions(Quartet.EnsembleOptions.class, Quartet.class, t,u,v,w);
    }

    public static <T,U,V,W,X,R> Quintet.ArgumentOptionsRoot<T,U,V,W,X,R>
            asArgumentsTo(Quintet.Function<T,U,V,W,X,R,?> recordFactory) {
        return newArgumentsRoot(Quintet.ArgumentOptionsRoot.class,
                Quintet.Function.class, recordFactory);
    }
    public static <T,U,V,W,X> Quintet.EnsembleOptions<T,U,V,W,X>
            use(T t, U u, V v, W w, X x) {
        return newRecordOptions(Quintet.EnsembleOptions.class, Quintet.class, t,u,v,w,x);
    }

    public static <T,U,V,W,X,Y,R> Sextet.ArgumentOptionsRoot<T,U,V,W,X,Y,R>
            asArgumentsTo(Sextet.Function<T,U,V,W,X,Y,R,?> recordFactory) {
        return newArgumentsRoot(Sextet.ArgumentOptionsRoot.class,
                Sextet.Function.class, recordFactory);
    }
    public static <T,U,V,W,X,Y> Sextet.EnsembleOptions<T,U,V,W,X,Y>
            use(T t, U u, V v, W w, X x, Y y) {
        return newRecordOptions(Sextet.EnsembleOptions.class, Sextet.class, t,u,v,w,x,y);
    }

    public static <T,U,V,W,X,Y,Z,R> Septet.ArgumentOptionsRoot<T,U,V,W,X,Y,Z,R>
            asArgumentsTo(Septet.Function<T,U,V,W,X,Y,Z,R,?> recordFactory) {
        return newArgumentsRoot(Septet.ArgumentOptionsRoot.class,
                Septet.Function.class, recordFactory);
    }
    public static <T,U,V,W,X,Y,Z> Septet.EnsembleOptions<T,U,V,W,X,Y,Z>
            use(T t, U u, V v, W w, X x, Y y, Z z) {
        return newRecordOptions(Septet.EnsembleOptions.class, Septet.class, t,u,v,w,x,y,z);
    }

    public static <T,U,V,W,X,Y,Z,Å,R> Octet.ArgumentOptionsRoot<T,U,V,W,X,Y,Z,Å,R>
            asArgumentsTo(Octet.Function<T,U,V,W,X,Y,Z,Å,R,?> recordFactory) {
        return newArgumentsRoot(Octet.ArgumentOptionsRoot.class,
                Octet.Function.class, recordFactory);
    }
    public static <T,U,V,W,X,Y,Z,Å> Octet.EnsembleOptions<T,U,V,W,X,Y,Z,Å>
            use(T t, U u, V v, W w, X x, Y y, Z z, Å å) {
        return newRecordOptions(Octet.EnsembleOptions.class, Octet.class, t,u,v,w,x,y,z,å);
    }

    public static <T,U,V,W,X,Y,Z,Å,Ä,R> Nonet.ArgumentOptionsRoot<T,U,V,W,X,Y,Z,Å,Ä,R>
            asArgumentsTo(Nonet.Function<T,U,V,W,X,Y,Z,Å,Ä,R,?> recordFactory) {
        return newArgumentsRoot(Nonet.ArgumentOptionsRoot.class,
                Nonet.Function.class, recordFactory);
    }
    public static <T,U,V,W,X,Y,Z,Å,Ä> Nonet.EnsembleOptions<T,U,V,W,X,Y,Z,Å,Ä>
            use(T t, U u, V v, W w, X x, Y y, Z z, Å å, Ä ä) {
        return newRecordOptions(Nonet.EnsembleOptions.class, Nonet.class, t,u,v,w,x,y,z,å,ä);
    }

    public static <T,U,V,W,X,Y,Z,Å,Ä,Ö,R> Decet.ArgumentOptionsRoot<T,U,V,W,X,Y,Z,Å,Ä,Ö,R>
            asArgumentsTo(Decet.Function<T,U,V,W,X,Y,Z,Å,Ä,Ö,R,?> recordFactory) {
        return newArgumentsRoot(Decet.ArgumentOptionsRoot.class,
                Decet.Function.class, recordFactory);
    }
    public static <T,U,V,W,X,Y,Z,Å,Ä,Ö> Decet.EnsembleOptions<T,U,V,W,X,Y,Z,Å,Ä,Ö>
            use(T t, U u, V v, W w, X x, Y y, Z z, Å å, Ä ä, Ö ö) {
        return newRecordOptions(Decet.EnsembleOptions.class, Decet.class, t,u,v,w,x,y,z,å,ä,ö);
    }

    public interface Duo<T,U> {
        <R,E extends Throwable> R applyOn(Function<? super T,? super U,R,E> function) throws E;
        <E extends Throwable> void execute(Consumer<? super T,? super U,E>  consumer) throws E;

        interface ArgumentOptionsRoot<T,U,R> {
            <O extends ArgumentOptions<T,U,R,O>> O use(T t, U u);
        }
        interface ArgumentOptions<T,U,R,O extends ArgumentOptions<T,U,R,O>>
        extends ScopedLazyParameter.FactoryRoot<R> {
            O or(T t, U u);
        }
        interface EnsembleOptions<T,U>
        extends ArgumentOptions<T,U,Duo<T,U>,EnsembleOptions<T,U>> {
            ScopedLazyParameter<Duo<T,U>> asParameter(String tName, String uName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(parameterName).pickValue()
             * </code></pre>... but the result Duo from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Duo<T,U> asLazyDuo(String parameterName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(tName, uName ...).pickValue()
             * </code></pre>... but the result Duo from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Duo<T,U> asLazyDuo(String tName, String uName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(toDisplayFunction).pickValue()
             * </code></pre>... but the result Duo from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Duo<T,U> asLazyDuo(Function<? super T,? super U,? extends CharSequence,?> toDisplayFunction);
            <R, RF extends ScopedLazyParameter.FactoryRoot<R>>
                    RF asArgumentsTo(Function<T,U,R,?> recordFactory);
        }

        @FunctionalInterface interface Function<T,U,R,E extends Throwable>
        {   R apply(T t, U u) throws E;}
        @FunctionalInterface interface Consumer<T,U,E extends Throwable>
        {void accept(T t, U u) throws E;}
    }

    public interface Trio<T,U,V> {
        <R,E extends Throwable> R applyOn(Function<? super T,? super U,? super V,R,E> function) throws E;
        <E extends Throwable> void execute(Consumer<? super T,? super U,? super V,E>  consumer) throws E;

        interface ArgumentOptionsRoot<T,U,V,R> {
            <O extends ArgumentOptions<T,U,V,R,O>> O use(T t, U u, V v);
        }
        interface ArgumentOptions<T,U,V,R,O extends ArgumentOptions<T,U,V,R,O>>
        extends ScopedLazyParameter.FactoryRoot<R> {
            O or(T t, U u, V v);
        }
        interface EnsembleOptions<T,U,V>
        extends ArgumentOptions<T,U,V,Trio<T,U,V>,EnsembleOptions<T,U,V>> {
            ScopedLazyParameter<Trio<T,U,V>> asParameter(String tName, String uName, String vName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(parameterName).pickValue()
             * </code></pre>... but the result Trio from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Trio<T,U,V> asLazyTrio(String parameterName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(tName, uName ...).pickValue()
             * </code></pre>... but the result Trio from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Trio<T,U,V> asLazyTrio(String tName, String uName, String vName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(toDisplayFunction).pickValue()
             * </code></pre>... but the result Trio from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Trio<T,U,V> asLazyTrio(Function<? super T,? super U,? super V,? extends CharSequence,?> toDisplayFunction);
            <R, RF extends ScopedLazyParameter.FactoryRoot<R>>
                    RF asArgumentsTo(Function<T,U,V,R,?> recordFactory);
        }

        @FunctionalInterface interface Function<T,U,V,R,E extends Throwable>
        {   R apply(T t, U u, V v) throws E;}
        @FunctionalInterface interface Consumer<T,U,V,E extends Throwable>
        {void accept(T t, U u, V v) throws E;}
    }

    public interface Quartet<T,U,V,W> {
        <R,E extends Throwable> R applyOn(Function<? super T,? super U,? super V,? super W,R,E> function) throws E;
        <E extends Throwable> void execute(Consumer<? super T,? super U,? super V,? super W,E>  consumer) throws E;

        interface ArgumentOptionsRoot<T,U,V,W,R> {
            <O extends ArgumentOptions<T,U,V,W,R,O>> O use(T t, U u, V v, W w);
        }
        interface ArgumentOptions<T,U,V,W,R,O extends ArgumentOptions<T,U,V,W,R,O>>
        extends ScopedLazyParameter.FactoryRoot<R> {
            O or(T t, U u, V v, W w);
        }
        interface EnsembleOptions<T,U,V,W>
        extends ArgumentOptions<T,U,V,W,Quartet<T,U,V,W>,EnsembleOptions<T,U,V,W>> {
            ScopedLazyParameter<Quartet<T,U,V,W>> asParameter(String tName, String uName, String vName, String wName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(parameterName).pickValue()
             * </code></pre>... but the result Quartet from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Quartet<T,U,V,W> asLazyQuartet(String parameterName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(tName, uName ...).pickValue()
             * </code></pre>... but the result Quartet from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Quartet<T,U,V,W> asLazyQuartet(String tName, String uName, String vName, String wName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(toDisplayFunction).pickValue()
             * </code></pre>... but the result Quartet from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Quartet<T,U,V,W> asLazyQuartet(Function<? super T,? super U,? super V,? super W,? extends CharSequence,?> toDisplayFunction);
            <R, RF extends ScopedLazyParameter.FactoryRoot<R>>
                    RF asArgumentsTo(Function<T,U,V,W,R,?> recordFactory);
        }

        @FunctionalInterface interface Function<T,U,V,W,R,E extends Throwable>
        {   R apply(T t, U u, V v, W w) throws E;}
        @FunctionalInterface interface Consumer<T,U,V,W,E extends Throwable>
        {void accept(T t, U u, V v, W w) throws E;}
    }

    public interface Quintet<T,U,V,W,X> {
        <R,E extends Throwable> R applyOn(Function<? super T,? super U,? super V,? super W,? super X,R,E> function) throws E;
        <E extends Throwable> void execute(Consumer<? super T,? super U,? super V,? super W,? super X,E>  consumer) throws E;

        interface ArgumentOptionsRoot<T,U,V,W,X,R> {
            <O extends ArgumentOptions<T,U,V,W,X,R,O>> O use(T t, U u, V v, W w, X x);
        }
        interface ArgumentOptions<T,U,V,W,X,R,O extends ArgumentOptions<T,U,V,W,X,R,O>>
        extends ScopedLazyParameter.FactoryRoot<R> {
            O or(T t, U u, V v, W w, X x);
        }
        interface EnsembleOptions<T,U,V,W,X>
        extends ArgumentOptions<T,U,V,W,X,Quintet<T,U,V,W,X>,EnsembleOptions<T,U,V,W,X>> {
            ScopedLazyParameter<Quintet<T,U,V,W,X>> asParameter(String tName, String uName, String vName, String wName, String xName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(parameterName).pickValue()
             * </code></pre>... but the result Quintet from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Quintet<T,U,V,W,X> asLazyQuintet(String parameterName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(tName, uName ...).pickValue()
             * </code></pre>... but the result Quintet from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Quintet<T,U,V,W,X> asLazyQuintet(String tName, String uName, String vName, String wName, String xName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(toDisplayFunction).pickValue()
             * </code></pre>... but the result Quintet from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Quintet<T,U,V,W,X> asLazyQuintet(Function<? super T,? super U,? super V,? super W,? super X,? extends CharSequence,?> toDisplayFunction);
            <R, RF extends ScopedLazyParameter.FactoryRoot<R>>
                    RF asArgumentsTo(Function<T,U,V,W,X,R,?> recordFactory);
        }

        @FunctionalInterface interface Function<T,U,V,W,X,R,E extends Throwable>
        {   R apply(T t, U u, V v, W w, X x) throws E;}
        @FunctionalInterface interface Consumer<T,U,V,W,X,E extends Throwable>
        {void accept(T t, U u, V v, W w, X x) throws E;}
    }

    public interface Sextet<T,U,V,W,X,Y> {
        <R,E extends Throwable> R applyOn(Function<? super T,? super U,? super V,? super W,? super X,? super Y,R,E> function) throws E;
        <E extends Throwable> void execute(Consumer<? super T,? super U,? super V,? super W,? super X,? super Y,E>  consumer) throws E;

        interface ArgumentOptionsRoot<T,U,V,W,X,Y,R> {
            <O extends ArgumentOptions<T,U,V,W,X,Y,R,O>> O use(T t, U u, V v, W w, X x, Y y);
        }
        interface ArgumentOptions<T,U,V,W,X,Y,R,O extends ArgumentOptions<T,U,V,W,X,Y,R,O>>
        extends ScopedLazyParameter.FactoryRoot<R> {
            O or(T t, U u, V v, W w, X x, Y y);
        }
        interface EnsembleOptions<T,U,V,W,X,Y>
        extends ArgumentOptions<T,U,V,W,X,Y,Sextet<T,U,V,W,X,Y>,EnsembleOptions<T,U,V,W,X,Y>> {
            ScopedLazyParameter<Sextet<T,U,V,W,X,Y>> asParameter(String tName, String uName, String vName, String wName, String xName, String yName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(parameterName).pickValue()
             * </code></pre>... but the result Sextet from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Sextet<T,U,V,W,X,Y> asLazySextet(String parameterName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(tName, uName ...).pickValue()
             * </code></pre>... but the result Sextet from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Sextet<T,U,V,W,X,Y> asLazySextet(String tName, String uName, String vName, String wName, String xName, String yName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(toDisplayFunction).pickValue()
             * </code></pre>... but the result Sextet from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Sextet<T,U,V,W,X,Y> asLazySextet(Function<? super T,? super U,? super V,? super W,? super X,? super Y,? extends CharSequence,?> toDisplayFunction);
            <R, RF extends ScopedLazyParameter.FactoryRoot<R>>
                    RF asArgumentsTo(Function<T,U,V,W,X,Y,R,?> recordFactory);
        }

        @FunctionalInterface interface Function<T,U,V,W,X,Y,R,E extends Throwable>
        {   R apply(T t, U u, V v, W w, X x, Y y) throws E;}
        @FunctionalInterface interface Consumer<T,U,V,W,X,Y,E extends Throwable>
        {void accept(T t, U u, V v, W w, X x, Y y) throws E;}
    }

    public interface Septet<T,U,V,W,X,Y,Z> {
        <R,E extends Throwable> R applyOn(Function<? super T,? super U,? super V,? super W,? super X,? super Y,? super Z,R,E> function) throws E;
        <E extends Throwable> void execute(Consumer<? super T,? super U,? super V,? super W,? super X,? super Y,? super Z,E>  consumer) throws E;

        interface ArgumentOptionsRoot<T,U,V,W,X,Y,Z,R> {
            <O extends ArgumentOptions<T,U,V,W,X,Y,Z,R,O>> O use(T t, U u, V v, W w, X x, Y y, Z z);
        }
        interface ArgumentOptions<T,U,V,W,X,Y,Z,R,O extends ArgumentOptions<T,U,V,W,X,Y,Z,R,O>>
        extends ScopedLazyParameter.FactoryRoot<R> {
            O or(T t, U u, V v, W w, X x, Y y, Z z);
        }
        interface EnsembleOptions<T,U,V,W,X,Y,Z>
        extends ArgumentOptions<T,U,V,W,X,Y,Z,Septet<T,U,V,W,X,Y,Z>,EnsembleOptions<T,U,V,W,X,Y,Z>> {
            ScopedLazyParameter<Septet<T,U,V,W,X,Y,Z>> asParameter(String tName, String uName, String vName, String wName, String xName, String yName, String zName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(parameterName).pickValue()
             * </code></pre>... but the result Septet from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Septet<T,U,V,W,X,Y,Z> asLazySeptet(String parameterName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(tName, uName ...).pickValue()
             * </code></pre>... but the result Septet from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Septet<T,U,V,W,X,Y,Z> asLazySeptet(String tName, String uName, String vName, String wName, String xName, String yName, String zName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(toDisplayFunction).pickValue()
             * </code></pre>... but the result Septet from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Septet<T,U,V,W,X,Y,Z> asLazySeptet(Function<? super T,? super U,? super V,? super W,? super X,? super Y,? super Z,? extends CharSequence,?> toDisplayFunction);
            <R, RF extends ScopedLazyParameter.FactoryRoot<R>>
                    RF asArgumentsTo(Function<T,U,V,W,X,Y,Z,R,?> recordFactory);
        }

        @FunctionalInterface interface Function<T,U,V,W,X,Y,Z,R,E extends Throwable>
        {   R apply(T t, U u, V v, W w, X x, Y y, Z z) throws E;}
        @FunctionalInterface interface Consumer<T,U,V,W,X,Y,Z,E extends Throwable>
        {void accept(T t, U u, V v, W w, X x, Y y, Z z) throws E;}
    }

    public interface Octet<T,U,V,W,X,Y,Z,Å> {
        <R,E extends Throwable> R applyOn(Function<? super T,? super U,? super V,? super W,? super X,? super Y,? super Z,? super Å,R,E> function) throws E;
        <E extends Throwable> void execute(Consumer<? super T,? super U,? super V,? super W,? super X,? super Y,? super Z,? super Å,E>  consumer) throws E;

        interface ArgumentOptionsRoot<T,U,V,W,X,Y,Z,Å,R> {
            <O extends ArgumentOptions<T,U,V,W,X,Y,Z,Å,R,O>> O use(T t, U u, V v, W w, X x, Y y, Z z, Å å);
        }
        interface ArgumentOptions<T,U,V,W,X,Y,Z,Å,R,O extends ArgumentOptions<T,U,V,W,X,Y,Z,Å,R,O>>
        extends ScopedLazyParameter.FactoryRoot<R> {
            O or(T t, U u, V v, W w, X x, Y y, Z z, Å å);
        }
        interface EnsembleOptions<T,U,V,W,X,Y,Z,Å>
        extends ArgumentOptions<T,U,V,W,X,Y,Z,Å,Octet<T,U,V,W,X,Y,Z,Å>,EnsembleOptions<T,U,V,W,X,Y,Z,Å>> {
            ScopedLazyParameter<Octet<T,U,V,W,X,Y,Z,Å>> asParameter(String tName, String uName, String vName, String wName, String xName, String yName, String zName, String åName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(parameterName).pickValue()
             * </code></pre>... but the result Octet from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Octet<T,U,V,W,X,Y,Z,Å> asLazyOctet(String parameterName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(tName, uName ...).pickValue()
             * </code></pre>... but the result Octet from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Octet<T,U,V,W,X,Y,Z,Å> asLazyOctet(String tName, String uName, String vName, String wName, String xName, String yName, String zName, String åName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(toDisplayFunction).pickValue()
             * </code></pre>... but the result Octet from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Octet<T,U,V,W,X,Y,Z,Å> asLazyOctet(Function<? super T,? super U,? super V,? super W,? super X,? super Y,? super Z,? super Å,? extends CharSequence,?> toDisplayFunction);
            <R, RF extends ScopedLazyParameter.FactoryRoot<R>>
                    RF asArgumentsTo(Function<T,U,V,W,X,Y,Z,Å,R,?> recordFactory);
        }

        @FunctionalInterface interface Function<T,U,V,W,X,Y,Z,Å,R,E extends Throwable>
        {   R apply(T t, U u, V v, W w, X x, Y y, Z z, Å å) throws E;}
        @FunctionalInterface interface Consumer<T,U,V,W,X,Y,Z,Å,E extends Throwable>
        {void accept(T t, U u, V v, W w, X x, Y y, Z z, Å å) throws E;}
    }

    public interface Nonet<T,U,V,W,X,Y,Z,Å,Ä> {
        <R,E extends Throwable> R applyOn(Function<? super T,? super U,? super V,? super W,? super X,? super Y,? super Z,? super Å,? super Ä,R,E> function) throws E;
        <E extends Throwable> void execute(Consumer<? super T,? super U,? super V,? super W,? super X,? super Y,? super Z,? super Å,? super Ä,E>  consumer) throws E;

        interface ArgumentOptionsRoot<T,U,V,W,X,Y,Z,Å,Ä,R> {
            <O extends ArgumentOptions<T,U,V,W,X,Y,Z,Å,Ä,R,O>> O use(T t, U u, V v, W w, X x, Y y, Z z, Å å, Ä ä);
        }
        interface ArgumentOptions<T,U,V,W,X,Y,Z,Å,Ä,R,O extends ArgumentOptions<T,U,V,W,X,Y,Z,Å,Ä,R,O>>
        extends ScopedLazyParameter.FactoryRoot<R> {
            O or(T t, U u, V v, W w, X x, Y y, Z z, Å å, Ä ä);
        }
        interface EnsembleOptions<T,U,V,W,X,Y,Z,Å,Ä>
        extends ArgumentOptions<T,U,V,W,X,Y,Z,Å,Ä,Nonet<T,U,V,W,X,Y,Z,Å,Ä>,EnsembleOptions<T,U,V,W,X,Y,Z,Å,Ä>> {
            ScopedLazyParameter<Nonet<T,U,V,W,X,Y,Z,Å,Ä>> asParameter(String tName, String uName, String vName, String wName, String xName, String yName, String zName, String åName, String äName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(parameterName).pickValue()
             * </code></pre>... but the result Nonet from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Nonet<T,U,V,W,X,Y,Z,Å,Ä> asLazyNonet(String parameterName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(tName, uName ...).pickValue()
             * </code></pre>... but the result Nonet from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Nonet<T,U,V,W,X,Y,Z,Å,Ä> asLazyNonet(String tName, String uName, String vName, String wName, String xName, String yName, String zName, String åName, String äName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(toDisplayFunction).pickValue()
             * </code></pre>... but the result Nonet from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Nonet<T,U,V,W,X,Y,Z,Å,Ä> asLazyNonet(Function<? super T,? super U,? super V,? super W,? super X,? super Y,? super Z,? super Å,? super Ä,? extends CharSequence,?> toDisplayFunction);
            <R, RF extends ScopedLazyParameter.FactoryRoot<R>>
                    RF asArgumentsTo(Function<T,U,V,W,X,Y,Z,Å,Ä,R,?> recordFactory);
        }

        @FunctionalInterface interface Function<T,U,V,W,X,Y,Z,Å,Ä,R,E extends Throwable>
        {   R apply(T t, U u, V v, W w, X x, Y y, Z z, Å å, Ä ä) throws E;}
        @FunctionalInterface interface Consumer<T,U,V,W,X,Y,Z,Å,Ä,E extends Throwable>
        {void accept(T t, U u, V v, W w, X x, Y y, Z z, Å å, Ä ä) throws E;}
    }

    public interface Decet<T,U,V,W,X,Y,Z,Å,Ä,Ö> {
        <R,E extends Throwable> R applyOn(Function<? super T,? super U,? super V,? super W,? super X,? super Y,? super Z,? super Å,? super Ä,? super Ö,R,E> function) throws E;
        <E extends Throwable> void execute(Consumer<? super T,? super U,? super V,? super W,? super X,? super Y,? super Z,? super Å,? super Ä,? super Ö,E>  consumer) throws E;

        interface ArgumentOptionsRoot<T,U,V,W,X,Y,Z,Å,Ä,Ö,R> {
            <O extends ArgumentOptions<T,U,V,W,X,Y,Z,Å,Ä,Ö,R,O>> O use(T t, U u, V v, W w, X x, Y y, Z z, Å å, Ä ä, Ö ö);
        }
        interface ArgumentOptions<T,U,V,W,X,Y,Z,Å,Ä,Ö,R,O extends ArgumentOptions<T,U,V,W,X,Y,Z,Å,Ä,Ö,R,O>>
        extends ScopedLazyParameter.FactoryRoot<R> {
            O or(T t, U u, V v, W w, X x, Y y, Z z, Å å, Ä ä, Ö ö);
        }
        interface EnsembleOptions<T,U,V,W,X,Y,Z,Å,Ä,Ö>
        extends ArgumentOptions<T,U,V,W,X,Y,Z,Å,Ä,Ö,Decet<T,U,V,W,X,Y,Z,Å,Ä,Ö>,EnsembleOptions<T,U,V,W,X,Y,Z,Å,Ä,Ö>> {
            ScopedLazyParameter<Decet<T,U,V,W,X,Y,Z,Å,Ä,Ö>> asParameter(String tName, String uName, String vName, String wName, String xName, String yName, String zName, String åName, String äName, String öName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(parameterName).pickValue()
             * </code></pre>... but the result Decet from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Decet<T,U,V,W,X,Y,Z,Å,Ä,Ö> asLazyDecet(String parameterName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(tName, uName ...).pickValue()
             * </code></pre>... but the result Decet from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Decet<T,U,V,W,X,Y,Z,Å,Ä,Ö> asLazyDecet(String tName, String uName, String vName, String wName, String xName, String yName, String zName, String åName, String äName, String öName);
            /**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(toDisplayFunction).pickValue()
             * </code></pre>... but the result Decet from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {@link Combiner} and
             * {@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/
            Decet<T,U,V,W,X,Y,Z,Å,Ä,Ö> asLazyDecet(Function<? super T,? super U,? super V,? super W,? super X,? super Y,? super Z,? super Å,? super Ä,? super Ö,? extends CharSequence,?> toDisplayFunction);
            <R, RF extends ScopedLazyParameter.FactoryRoot<R>>
                    RF asArgumentsTo(Function<T,U,V,W,X,Y,Z,Å,Ä,Ö,R,?> recordFactory);
        }

        @FunctionalInterface interface Function<T,U,V,W,X,Y,Z,Å,Ä,Ö,R,E extends Throwable>
        {   R apply(T t, U u, V v, W w, X x, Y y, Z z, Å å, Ä ä, Ö ö) throws E;}
        @FunctionalInterface interface Consumer<T,U,V,W,X,Y,Z,Å,Ä,Ö,E extends Throwable>
        {void accept(T t, U u, V v, W w, X x, Y y, Z z, Å å, Ä ä, Ö ö) throws E;}
    }

    public interface AllEnsemblesFunction<T,R,E extends Throwable>
    extends Duo.Function<T,T,R,E>,
            Trio.Function<T,T,T,R,E>,
            Quartet.Function<T,T,T,T,R,E>,
            Quintet.Function<T,T,T,T,T,R,E>,
            Sextet.Function<T,T,T,T,T,T,R,E>,
            Septet.Function<T,T,T,T,T,T,T,R,E>,
            Octet.Function<T,T,T,T,T,T,T,T,R,E>,
            Nonet.Function<T,T,T,T,T,T,T,T,T,R,E>,
            Decet.Function<T,T,T,T,T,T,T,T,T,T,R,E> {}

    /**
     * @deprecated
     * There is doubt whether this is a good idea.
     */
    @Deprecated
    public static AllEnsemblesFunction<Object,String,RuntimeException> joining(final CharSequence delimiter) {
        return groupBy(new GroupFunction<Object,String,RuntimeException>() {
            public String apply(List<Object> arguments) {
                StringBuilder sb = new StringBuilder();
                for (Object eachArg : arguments) {
                    sb.append(delimiter).append(eachArg);
                }
                return sb.substring(delimiter.length());
            }
        });
    }

    public static <T,R,E extends Throwable> AllEnsemblesFunction<T,R,E> groupBy(
            final GroupFunction<T,R,E> groupFunction) {
        return new AllEnsemblesFunction<T,R,E>() {
            @Override public R apply(T t,T u) throws E {
                List<T> arguments = new ArrayList();
                arguments.addAll(Arrays.asList(t,u));
                return groupFunction.apply(arguments);
            }
            @Override public R apply(T t,T u,T v) throws E {
                List<T> arguments = new ArrayList();
                arguments.addAll(Arrays.asList(t,u,v));
                return groupFunction.apply(arguments);
            }
            @Override public R apply(T t,T u,T v,T w) throws E {
                List<T> arguments = new ArrayList();
                arguments.addAll(Arrays.asList(t,u,v,w));
                return groupFunction.apply(arguments);
            }
            @Override public R apply(T t,T u,T v,T w,T x) throws E {
                List<T> arguments = new ArrayList();
                arguments.addAll(Arrays.asList(t,u,v,w,x));
                return groupFunction.apply(arguments);
            }
            @Override public R apply(T t,T u,T v,T w,T x,T y) throws E {
                List<T> arguments = new ArrayList();
                arguments.addAll(Arrays.asList(t,u,v,w,x,y));
                return groupFunction.apply(arguments);
            }
            @Override public R apply(T t,T u,T v,T w,T x,T y,T z) throws E {
                List<T> arguments = new ArrayList();
                arguments.addAll(Arrays.asList(t,u,v,w,x,y,z));
                return groupFunction.apply(arguments);
            }
            @Override public R apply(T t,T u,T v,T w,T x,T y,T z,T å) throws E {
                List<T> arguments = new ArrayList();
                arguments.addAll(Arrays.asList(t,u,v,w,x,y,z,å));
                return groupFunction.apply(arguments);
            }
            @Override public R apply(T t,T u,T v,T w,T x,T y,T z,T å,T ä) throws E {
                List<T> arguments = new ArrayList();
                arguments.addAll(Arrays.asList(t,u,v,w,x,y,z,å,ä));
                return groupFunction.apply(arguments);
            }
            @Override public R apply(T t,T u,T v,T w,T x,T y,T z,T å,T ä,T ö) throws E {
                List<T> arguments = new ArrayList();
                arguments.addAll(Arrays.asList(t,u,v,w,x,y,z,å,ä,ö));
                return groupFunction.apply(arguments);
            }
        };
    }
}
