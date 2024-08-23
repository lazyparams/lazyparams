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

import org.junit.jupiter.api.Test;
import org.lazyparams.LazyParams;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A little test to make sure a {@link org.lazyparams.core.Lazer} instance does not
 * survive between test-methods when parametrization has been canceled with
 * {@link org.lazyparams.config.Configuration#setMaxTotalCount(int) Configuration#setMaxTotalCount(1)}
 *
 * @author Henrik Kaipe
 */
public class MaxTotalCountAtOneMustNotLeakParametersTest {

    @Test @org.junit.Test
    public void passOn1stAndOnly() {
        LazyParams.currentScopeConfiguration().setMaxTotalCount(1);
        assertThat(LazyParams.pickValue("answer", 42,"2nd value that must not leak"))
                .as("Pass answer")
                .isEqualTo(42);
    }
    @Test @org.junit.Test
    public void passOn1stAndOnly_again() {
        passOn1stAndOnly();
    }
}
