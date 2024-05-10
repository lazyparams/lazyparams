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

import org.lazyparams.LazyParams;
import org.lazyparams.LazyParamsCoreUtil;
import org.lazyparams.ToDisplayFunction;
import org.lazyparams.config.Configuration;

/**
 * @author Henrik Kaipe
 */
 enum DisplayVerbosity {
    /**Default - will make parameter value-pick part of test display-name!*/
    VERBOSE,
    /**@see ScopedLazyParameter.Silencer#qronicly()*/
    QRONIC,
    /**@see ScopedLazyParameter.Silencer#quietly()*/
    QUIET;

    <T> void display(Object paramId, ToDisplayFunction<T> toDisplay, T value) {
        if (QUIET == this) {
            return /*without dwelling further into this!*/;
        }
        /*
         * Prepare (lazy) content to display ...
         */
        CharSequence content2display = toDisplay.apply(value);
        Configuration scopedConfig = LazyParams.currentScopeConfiguration();
        if (scopedConfig.alsoUseValueDisplaySeparatorBeforeToDisplayFunction()) {
            final String displaySeparator = scopedConfig.getValueDisplaySeparator();
            final CharSequence coreOnDisplay = content2display;
            content2display = new CharSequence() {
                /*Also support display of lazy content!*/

                @Override
                public int length() {
                    return toString().length();
                }
                @Override
                public char charAt(int i) {
                    return toString().charAt(i);
                }
                @Override
                public CharSequence subSequence(int i, int j) {
                    return toString().subSequence(i, j);
                }

                @Override
                public String toString() {
                    return displaySeparator + coreOnDisplay;
                }
            };
        }
        /*
         * Display it ...
         */
        LazyParamsCoreUtil.displayOnFailure(paramId, content2display);
        if (VERBOSE == this) {
            LazyParamsCoreUtil.displayOnSuccess(paramId, content2display);
        }
    }
}
