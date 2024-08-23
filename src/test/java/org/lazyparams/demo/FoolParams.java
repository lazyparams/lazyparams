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

import org.lazyparams.LazyParamsCoreUtil;

/**
 * Offers functionality, which recommended best practice API implementation is
 * provided by {@link LazyParams}. This is a faster - but less safe -
 * implementation that is intended as a reference to get an idea on
 * how (hopefully) little performance is affected by the fine-grained default
 * parameter-ID composition of {@link ScopedLazyParameter}, which is used by
 * the best practice API of {@link LazyParams} for more careful recognition of
 * repeated parameters, so that potential errors because of parameter mix-ups
 * can be avoided with more confidence.
 * <br/>
 * I.e. this is a not fool-proof equivalent of {@link LazyParams} and hence the
 * name "FoolParams".
 *
 * @see org.lazyparams.LazyParams
 *
 * @author Henrik Kaipe
 */
public class FoolParams {

    /**
     * Technically it achieves the same parametrization as
     * {@link LazyParams#pickValue(String,Object[]) but with a little better
     * performance, because it bypasses safe-guards that {@link LazyParams}
     * achieves by relying on the fine-grained default parameter-ID composition
     * that {@link ScopedLazyParameter} provides.
     * Here the parameter-ID is simply the parameter-name. It helps performance
     * but could make framework accidentally mistake two separate parameters as
     * one and the same and therewith cause errors that are hard to recognize
     * for a developer. Therefore this implementation is not part of main.
     *
     * @see org.lazyparams.LazyParams#pickValue(String,Object[])
     * @see LazyParamsCoreUtil
     */
    public static <T> T pickValue(final String parameterName, T... possibleParamValues) {
        T value = possibleParamValues[
                LazyParamsCoreUtil.makePick(parameterName, true, possibleParamValues.length)];
        String onDisplay = " " + parameterName + '=' + value;
        LazyParamsCoreUtil.displayOnSuccess(onDisplay, onDisplay);
        LazyParamsCoreUtil.displayOnFailure(onDisplay, onDisplay);
        return value;
    }
}
