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

/**
 * @author Henrik Kaipe
 */
public class FalseOrTrue {
    private FalseOrTrue() {}

    public static boolean pickBoolean(CharSequence displayOnTrue) {
        return pickBoolean(displayOnTrue, null);
    }
    public static boolean pickBoolean(
            final CharSequence displayOnTrue, final CharSequence displayOnFalse) {
        Object paramId = new ToStringKey(
                "" + displayOnTrue + (char)0 + displayOnFalse,
                displayOnTrue, displayOnFalse) {};
        boolean result = 1 == LazyParamsCoreUtil.makePick(paramId, true, 2);
        CharSequence toDisplay = result ? displayOnTrue : displayOnFalse;
        if (null != toDisplay) {
            toDisplay = LazyParams.currentScopeConfiguration()
                    .getValueDisplaySeparator() + toDisplay;
            LazyParamsCoreUtil.displayOnFailure(paramId, toDisplay);
            LazyParamsCoreUtil.displayOnSuccess(paramId, toDisplay);
        }
        return result;
    }
}
