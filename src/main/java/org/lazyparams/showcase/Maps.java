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

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiFunction;

import org.lazyparams.LazyParams;
import org.lazyparams.ToDisplayFunction;

/**
 * @author Henrik Kaipe
 * @deprecated
 * This kind of ugly API has been deprecated on behalf of {@link ToPick} that
 * offers collectors for creating parameters from streams. The {@link ToPick}
 * collectors are much more elegant and easy to reuse for other innovation.
 *
 * @see ToPick#from()
 */
@Deprecated
public class Maps {
    private Maps() {}

    private final static ToDisplayFunction<Map.Entry<Object,Object>> default2display =
            new ToDisplayFunction<Map.Entry<Object,Object>>() {
        @Override
        public CharSequence apply(Map.Entry value) {
            return value.getKey() +  " => " + value.getValue();
        }
    };

    public static <K extends Comparable<K>,V> Map.Entry<K,V> pickEntry(Map<K,V> entries) {
        return pickEntry(default2display, entries);
    }

    public static <K extends Comparable<? extends DK>,V extends DV, DK,DV> Map.Entry<K,V> pickEntry(
            ToDisplayFunction<? super Map.Entry<DK,DV>> toDisplay,
            Map<K,V> entries) {
        return (Map.Entry<K,V>) pickEntry(toDisplay, new TreeMap<DK,V>((Map)entries));
    }

    public static <K extends Comparable<? extends DK>,V,DK> Map.Entry<K,V> pickEntry(
            BiFunction<DK, ? super V,? extends CharSequence> toDisplay,
            Map<K,V> entries) {
        return (Map.Entry<K, V>) pickEntry(toDisplay, new TreeMap<DK,V>((Map)entries));
    }

    public static <K,V> Map.Entry<K,V> pickEntry(SortedMap<K,V> entries) {
        return pickEntry(default2display, entries);
    }

    public static <K extends DK,V extends DV, DK,DV> Map.Entry<K,V> pickEntry(
            ToDisplayFunction<? super Map.Entry<DK,DV>> toDisplay,
            SortedMap<K,V> entries) {
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Cannot pick entry from empty map!");
        }
        return LazyParams.pickValue((ToDisplayFunction)toDisplay,
                entries.entrySet().toArray(new Map.Entry[entries.size()]));
    }

    public static <K,V> Map.Entry<K,V> pickEntry(
            final BiFunction<? super K, ? super V, ? extends CharSequence> toDisplay,
            SortedMap<K,V> entries) {
        return pickEntry(new ToDisplayFunction<Map.Entry<K,V>>() {
            @Override
            public CharSequence apply(Map.Entry<K,V> entry) {
                return toDisplay.apply(entry.getKey(), entry.getValue());
            }
        }, entries);
    }
}
