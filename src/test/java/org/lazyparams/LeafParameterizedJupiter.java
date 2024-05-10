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

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.Assert.assertEquals;
import static org.lazyparams.LazyParams.pickValue;

/**
 * @author Henrik Kaipe
 */
public class LeafParameterizedJupiter {

    @RegisterExtension
    public static final CompositeExtensions mutableExtensions = new CompositeExtensions();

    @Test
    public void normal() {
        int i = pickValue("1st", 34, 42);
        String s = pickValue("2nd", "sdf", "dfwe");
        if (34 == i && "dfwe".equals(s)) {
            throw new IllegalStateException("Fail here");
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {28, 42, 43})
    public void parameterization(int nbr) {
        normal();
        pickValue("xtra", "a One", "a Two");
    }

    @RepeatedTest(2)
    public void repeat() {
        int i = pickValue("1st", 43, 242);
        String s = pickValue("2nd", "prime", "other");
        if (43 == i && "other".equals(s)) {
            throw new IllegalStateException("Fail repeat");
        }
    }

    @Test
    public void noParamsHere() {
//        System.out.println("NO PARAMS ARE HERE!!!");
    }

    @Test
    public void failWithoutParams() {
//        System.out.println("NO PARAMS ARE HERE!!!");
        throw new RuntimeException("FAiLURE");
    }

    @Test
    public void manyParameters() {
        try {
            normal();
        } catch (RuntimeException e) {
            return;
        }
        int tjoho = pickValue("nbr", 1, 2, 3);
        if (pickValue("boolean", false, true)) {
            assertEquals("mix", 2, tjoho);
        }
    }
}
