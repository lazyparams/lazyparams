/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams;

/**
 * @see LazyParams#pickValue(ToDisplayFunction,Object,Object...)
 * @see LazyParams#pickValue(ToDisplayFunction,Object[])
 * @author Henrik Kaipe
 */
@FunctionalInterface
public interface ToDisplayFunction<T> {

    CharSequence apply(T value);
}
