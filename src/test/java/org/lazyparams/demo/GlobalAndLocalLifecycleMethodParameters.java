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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demo class for educational purposes.
 * It is not an example of best practice, however. Instead it's for comparing
 * LazyParams with traditional parametrization patterns.
 *
 * @author Henrik Kaipe
 */
public class GlobalAndLocalLifecycleMethodParameters {
    static String global;
    int local;

    @BeforeAll
    static void setupGlobal(
            @StringParam({"gblFoo","gblBar"})  String global)
    {
        GlobalAndLocalLifecycleMethodParameters.global = global;
    }

    @BeforeEach
    void setupLocal(
            @IntParam({1,2,3})    int local)
    {
        this.local = local;
    }

    @Test void vanilla() {
        assertThat(global).as("global").startsWith("gbl");
        assertThat(local).as("local").isGreaterThan(0).isLessThan(5);
    }

    @Test void extraParameters(
            @StringParam({"foo","bar","buz"}) String extraStr,
            @IntParam     ({ 41,  42,  43})   int extraInt)
    {
        vanilla();
        assertThat(extraInt).as("extraInt").isIn(41, 42, 43);
        assertThat(extraStr).as("extraStr").hasSize(3);
    }

    @ExtendWith(LazyParameterResolver.class) @Retention(RetentionPolicy.RUNTIME)
    @interface IntParam { int[] value(); }

    @ExtendWith(LazyParameterResolver.class) @Retention(RetentionPolicy.RUNTIME)
    @interface StringParam { String[] value(); }
}
