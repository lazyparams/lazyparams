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
import org.junit.jupiter.api.BeforeAll;
import org.lazyparams.showcase.FalseOrTrue;

/**
 * When being run with {@link UninstallMultiscopedTest} then this test emulates
 * {@link LazyParams#uninstall()} during static initialization for a test that
 * then introduces parameter during Jupiter static scope - e.g. from a
 * {@link BeforeAll @BeforeAll} method.
 * This is a regression test for a bug that prevented proper implicit reinstall
 * in static scope. What happened was that the {@link org.lazyparams.core.Lazer}
 * instance of the first implicit installation was lost - except for info on its
 * pending repetition, which forced a repetition of static scope but with a new
 * {@link org.lazyparams.core.Lazer} instance, which somehow replaced the first
 * one and was then prevailed for the remainder of the test execution.
 *
 * @author Henrik Kaipe
 */
public class UninstallMultiscoped {

    static boolean uninstallOn1stRun = true;

    @BeforeAll static void forceUninstallOn1stRun() {
        if (uninstallOn1stRun) {
            LazyParams.uninstall();
        }
        uninstallOn1stRun = FalseOrTrue.pickBoolean("already installed", "force uninstall");
    }

    @Test void test() {
        LazyParams.pickValue("dummy", 1,2,3);
    }
}
