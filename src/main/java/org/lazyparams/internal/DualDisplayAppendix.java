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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Henrik Kaipe
 */
class DualDisplayAppendix implements CharSequence {
    private final Map<Object,CharSequence> failureDisplayParts =
            new LinkedHashMap<Object,CharSequence>();
    private final Map<Object,CharSequence> successDisplayParts =
            new LinkedHashMap<Object, CharSequence>();

    private volatile String displayAppendixTextCache = null;
    private volatile boolean success = false;

    void display(Object displayPartRef,
            CharSequence displayAppendixPart, boolean success) {
        Map<Object,CharSequence> target = success
                ? successDisplayParts : failureDisplayParts;
        synchronized (target) {
            target.put(displayPartRef, displayAppendixPart);
        }
        if (this.success == success) {
            /* Force refresh on appendix text cache: */
            displayAppendixTextCache = null;
        }
    }

    void setResult(boolean success) {
        this.success = success;
        displayAppendixTextCache = null;
    }

    @Override
    public int length() {
        return toString().length();
    }
    @Override
    public char charAt(int i) {
        return toString().charAt(i);
    }
    @Override
    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }
    @Override
    public String toString() {
        String cacheText = displayAppendixTextCache;
        if (null == cacheText) {
            Map<Object,CharSequence> source = this.success
                    ? successDisplayParts : failureDisplayParts;
            synchronized (source) {
                cacheText = displayAppendixTextCache;
                if (null == cacheText) {
                    StringBuilder sb = new StringBuilder(10 + 10 * source.size());
                    for (CharSequence eachDisplayPart : source.values()) {
                        sb.append(eachDisplayPart);
                    }
                    displayAppendixTextCache = cacheText = sb.toString();
                }
            }
        }
        return cacheText;
    }
}
