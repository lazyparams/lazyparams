/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.demo;

import java.util.Arrays;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Henrik Kaipe
 */
public enum CloseableLifecycledResource implements AutoCloseable {
    BEFORE_ALL,
    BEFORE_EACH_TEST,
    DURING_TEST_ONE,
    DURING_TEST_TWO,
    DURING_TEST_TWO_EXTRA,
    AFTER_EACH_TEST,
    AFTER_ALL;

    private static final EnumSet<CloseableLifecycledResource> openOnes =
            EnumSet.noneOf(CloseableLifecycledResource.class);

    public static void assertOpenOnesAreJust(
            CloseableLifecycledResource... expectedOpenResources) {
        if (0 == expectedOpenResources.length) {
            assertThat(openOnes).as("Open Lifecycle Resources").isEmpty();
        } else {
            assertThat(openOnes).as("Open Lifecycle Resources")
                    .isEqualTo(EnumSet.copyOf(Arrays.asList(expectedOpenResources)));
        }
    }

    public static void assertOpenOnesWhenTestMethodStarts() {
        assertOpenOnesAreJust(BEFORE_ALL, BEFORE_EACH_TEST);
    }

    public void open() {
        openOnes.add(this);
    }

    @Override public void close() {
        openOnes.remove(this);
    }
}
