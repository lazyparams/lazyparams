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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lazyparams.showcase.Timing;
import org.lazyparams.showcase.Qronicly;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Henrik Kaipe
 */
public class DisplayStatic {

    @BeforeAll static void beforeAll() {
        Timing.displayFromNow();
    }

    @Test void regularWithoutParameter() {}

    @Test void displaySomething() {
        LazyParamsCoreUtil.displayOnSuccess(new Object(), "displayed");
    }

    @Test void singleValueParameter() {
        LazyParams.pickValue(i -> "The 1", 1);
    }

    @Test void valuesX3() {
        assertThat(Qronicly.pickValue(s -> "X".equals(s) ? "Draw"
                : "1".equals(s) ? "Home" : "Away", "1","X","2"))
                .as("must not be a draw")
                .isNotEqualTo("X");
    }
}
