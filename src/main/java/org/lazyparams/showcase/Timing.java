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

import java.text.DecimalFormat;
import org.lazyparams.LazyParamsCoreUtil;

/**
 * @author Henrik Kaipe
 */
public class Timing {
    private Timing() {}

    private static final Object displayKey = new ToStringKey(Timing.class.getSimpleName()) {};

    public static CharSequence displayFromNow() {
        return new CharSequence() {

            long startTime;

            @Override
            public String toString() {
                return new DecimalFormat(" 0.000s").format(
                        (System.currentTimeMillis() - startTime) / 1000.0);
            }

            @Override public int length() {
                return toString().length();
            }
            @Override public char charAt(int i) {
                return toString().charAt(i);
            }
            @Override public CharSequence subSequence(int i, int i1) {
                return toString().subSequence(i, i1);
            }

            /**
             * Try to avoid having execution time include the time required for
             * LazyParams' global installation, by making sure start-time is set
             * <i>after</i> this CharSequence instance has been put on display.
             */
            private CharSequence display() {
                try {
                    LazyParamsCoreUtil.displayOnFailure(displayKey, this);
                    return LazyParamsCoreUtil.displayOnSuccess(displayKey, this);
                } finally {
                    if (startTime <= 0) {
                        startTime = System.currentTimeMillis();
                    }
                }
            }
        }.display();
    }
}
