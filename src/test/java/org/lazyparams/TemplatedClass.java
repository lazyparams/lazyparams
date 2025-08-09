/*
 * Copyright 2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams;

import java.lang.annotation.RetentionPolicy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.AfterParameterizedClassInvocation;
import org.junit.jupiter.params.BeforeParameterizedClassInvocation;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.lazyparams.showcase.FalseOrTrue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Henrik Kaipe
 */
@ParameterizedClass
@EnumSource
public class TemplatedClass {

    static boolean introduceClassinvParams;
    static int latestClassinv;

    TemplatedClass(RetentionPolicy policy) {
        if (introduceClassinvParams) {
            LazyParams.pickValue("inConstructor", 1,2);
        }
    }

    @BeforeAll static void decideOnClassinvParams() {
        introduceClassinvParams = FalseOrTrue
                .pickBoolean("allow_classinv_params", "prohibit_classinv_params");
    }

    @AfterParameterizedClassInvocation
    static void afterClassinv() {
        assertEquals(latestClassinv,
                LazyParams.pickValue("clsInv", 4,5));
    }

    @BeforeParameterizedClassInvocation
    static void beforeClassinv() {
        if (introduceClassinvParams) {
            latestClassinv = LazyParams.pickValue("clsInv", 4,5);
        }
    }
    
    @Test void normalTest() {
        LazyParams.pickValue("normal", 41,42);
    }

    @ValueSource(strings = {"tjo","tjim"})
    @ParameterizedTest void parameterized(String value) {}
}
