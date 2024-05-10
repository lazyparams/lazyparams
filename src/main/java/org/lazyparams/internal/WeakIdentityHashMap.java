/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.internal;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Poor man implementation that is much needed by this framework.
 * 
 * @author Henrik Kaipe
 */
class WeakIdentityHashMap<K,V> {

    private final Map<KeyReference,V> coreMap =
            new ConcurrentHashMap<KeyReference, V>();
    private final ReferenceQueue<K> referenceQ = new ReferenceQueue<K>();

    @SuppressWarnings("element-type-mismatch")
    private void cleanDerefenced() {
        Reference<? extends K> unQd;
        while (null != (unQd = referenceQ.poll())) {
            coreMap.remove(unQd);
        }
    }

    void put(K key, V value) {
        cleanDerefenced();
        coreMap.put(new WeakKeyReference(key, referenceQ), value);
    }

    V remove(K key) {
        cleanDerefenced();
        return null == key ? null
                : coreMap.remove(new TmpKeyReference<K>(key));
    }

    /**
     * Used by {@link ConfigurationContext#resetAllCurrentConfig()} during
     * {@link org.lazyparams.LazyParams#uninstall()}
     */
    boolean removeValue(V value) {
        if (null == value) {
            return false;
        }
        for (Iterator<V> i = coreMap.values().iterator(); i.hasNext();) {
            if (i.next() == value) {
                i.remove();
                return true;
            }
        }
        return false;
    }

    V get(K key) {
        return null == key ? null
                : coreMap.get(new TmpKeyReference<K>(key));
    }

    boolean containsKey(K key) {
        return null != key
                && coreMap.containsKey(new TmpKeyReference<K>(key));
    }

    private interface KeyReference {}

    private static class TmpKeyReference<K> implements KeyReference {
        private final Object key;

        TmpKeyReference(Object key) {
            this.key = key;
        }
        @Override public int hashCode() {
            return System.identityHashCode(key);
        }
        @Override public boolean equals(Object obj) {
            return obj instanceof WeakKeyReference
                    && key == ((WeakKeyReference<?>)obj).get();
        }
    }
    private static class WeakKeyReference<K> extends WeakReference<K>
    implements KeyReference {
        private final int hashCode;

        WeakKeyReference(K key, ReferenceQueue<K> derefQ) {
            super(key, derefQ);
            this.hashCode = System.identityHashCode(key);
        }
        @Override public int hashCode() {
            return hashCode;
        }
        @SuppressWarnings("null")
        @Override public boolean equals(Object obj) {
            return this == obj ? true
                    : obj instanceof TmpKeyReference
                    ? obj.equals(this)
                    : get() == ((WeakKeyReference<?>)obj).get();
        }
    }
}
