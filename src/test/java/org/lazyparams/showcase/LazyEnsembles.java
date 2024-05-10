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

import org.junit.jupiter.api.Test;
import org.lazyparams.LazyParams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Henrik Kaipe
 */
public class LazyEnsembles {

    static final Ensembles.Quartet<String, Integer, String, Boolean> qParam = Ensembles
            .use("1st", 1, "nbr 1", false)
            .or("2nd", 22, "nbr 2nd", true)
            .or("3rd", 3, "three", true)
            .or("4th", 404, "goes forth", false)
            .asLazyQuartet("order", "int", "desc", "b");

    @BeforeEach void startTime() {
        Timing.displayFromNow();
    }

    @Test void testModulo() {
        qParam.execute((s1,i,s2,b) -> assertEquals(0, i % 2, "Modulo 2"));
    }

    @Test void testBool() {
        Boolean bool2expect = LazyParams.pickValue("expct", false, true);
        Boolean paramValueBool = qParam.applyOn((s1,i,s2,b) -> b);
        assertEquals(bool2expect, paramValueBool, "Q-Param boolean value");
    }
}
