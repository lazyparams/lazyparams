/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.demo;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.FutureTask;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import org.lazyparams.showcase.Ensembles;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Just some test on discarded experimental functionality, in order to
 * get some coverage on Ensembles.
 *
 * @author Henrik Kaipe
 */
public class ExampleEnsemblesTest {

    abstract class AbstractErased implements BiFunction {}
    abstract class SubClasFiles implements BiFunction<CharSequence,File,String> {}
    interface NonArraySuptype<T> extends BiFunction<String,T[],File> {}
    abstract class NonArrayFirst implements NonArraySuptype<StringBuilder>, Serializable {}
    interface NonArrayLast<E,T extends List<E>> extends Set<Comparable<Set<T>>>, NonArraySuptype<T> {}
    abstract class ListBiFunction<E,T extends List<E>> implements BiFunction<Object, T[], Void> {}
    abstract class ArrayListFunction<S> extends ListBiFunction<S, ArrayList<S>> {}
    abstract class SubArray extends FutureTask<Void> implements NonArrayLast<File,CopyOnWriteArrayList<File>> {
        public SubArray(Callable<Void> callable) { super(callable); }
    }

    @Test
    public void testArgResolution() {
        Ensembles.<Class<? extends BiFunction>, Class<?>>

                use(AbstractErased.class, Object.class)
                .or(SubClasFiles.class,   File.class)
                .or(NonArraySuptype.class, Object[].class)
                .or(NonArrayFirst.class,   StringBuilder[].class)
                .or(NonArrayLast.class,    List[].class)
                .or(ListBiFunction.class,  List[].class)
                .or(ArrayListFunction.class, ArrayList[].class)
                .or(SubArray.class,         CopyOnWriteArrayList[].class)

                .asLazyDuo((type,expect) -> "resolve " + expect.getSimpleName()
                        + " from " + type.getSimpleName())
                .execute((type,expect) -> {

            assertSame(expect, secondArgumentFor(type));
        });
    }

    static Class secondArgumentFor(Class<? extends BiFunction> valuePickerClass) {
        if (false == BiFunction.class.isAssignableFrom(valuePickerClass)) {
            throw new IllegalArgumentException(
                    "Not a BiFunction type: " + valuePickerClass);
        }
        return resolveRawClass(secondTypeArgOf((Type)valuePickerClass));
    }
    private static Class resolveRawClass(Type type) {
        while (false == type instanceof Class) {
            if (type instanceof ParameterizedType) {
                type = ((ParameterizedType)type).getRawType();
            } else if (type instanceof TypeVariable) {
                type = ((TypeVariable)type).getBounds()[0];
            } else if (type instanceof GenericArrayType) {
                Class<?> elementType = resolveRawClass(
                        ((GenericArrayType)type).getGenericComponentType());
                return Array.newInstance(elementType, 0).getClass();
            } else {
                throw new IllegalArgumentException(
                        "Unexpected type: " + type.getClass());
            }
        }
        return (Class<?>) type;
    }
    private static Type secondTypeArgOf(Type valuePickerType) {
        final Class raw = resolveRawClass(valuePickerType);
        if (BiFunction.class == raw) {
            if (valuePickerType instanceof ParameterizedType) {
                Type argType = ((ParameterizedType)valuePickerType)
                        .getActualTypeArguments()[1];
                if (Object.class != argType) {
                    return argType;
                }
            }
            return Object.class;

        } else {
            Type resolved = null;
            if (false == raw.isInterface()
                    && BiFunction.class.isAssignableFrom(raw.getSuperclass())) {
                resolved = secondTypeArgOf(raw.getGenericSuperclass());
            } else {
                for (Type interf : raw.getGenericInterfaces()) {
                    Class interfRaw = resolveRawClass(interf);
                    if (BiFunction.class.isAssignableFrom(interfRaw)) {
                        resolved = secondTypeArgOf(interf);
                        break;
                    }
                }
            }
            if (null == resolved) {
                throw new IllegalArgumentException(
                        "Not a BiFunction type: " + valuePickerType);
            } else if (resolved instanceof Class || raw == valuePickerType) {
                return resolved;
            } else {
                return actualTypeArg((ParameterizedType)valuePickerType, resolved);
            }
        }
    }
    private static Type actualTypeArg(ParameterizedType declType, Type typeVar) {
        if (typeVar instanceof TypeVariable) {
            TypeVariable[] allTypeVars = ((TypeVariable)typeVar)
                    .getGenericDeclaration().getTypeParameters();
            for (int i = 0; i < allTypeVars.length; ++i) {
                if (typeVar.equals(allTypeVars[i])) {
                    return declType.getActualTypeArguments()[i];
                }
            }
        } else if (typeVar instanceof GenericArrayType) {
            final Type actualComponentType = actualTypeArg(
                    declType, ((GenericArrayType)typeVar).getGenericComponentType());
            return new GenericArrayType() {
                @Override public Type getGenericComponentType() {
                    return actualComponentType;
                }
            };
        }
        throw new IllegalArgumentException(typeVar + " resolution failed on " + declType);
    }
}
