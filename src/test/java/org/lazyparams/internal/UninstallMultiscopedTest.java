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

import org.lazyparams.LazyParams;
import org.junit.Rule;
import org.junit.Test;
import org.lazyparams.VerifyJupiterRule;

/**
 * Prepares execution of {@link UninstallMultiscoped} by making sure
 * {@link LazyParams#install() LazyParams is already installed} before test is
 * started and then force it to {@link LazyParams#uninstall()} before static
 * scope and its parameters are introduced for the first time.
 * And then verify test executes as expected.
 *
 * @author Henrik Kaipe
 */
public class UninstallMultiscopedTest {

    @Rule
    public VerifyJupiterRule expect = new VerifyJupiterRule(UninstallMultiscoped.class);

    @Test
    public void test() {
        /*
         * Here the purpose is to make sure uninstall during static Jupiter
         * scope works and does not break things if implicit installation (i.e.
         * parameter introduction) happens right after uninstallation.
         */
        LazyParams.install();

        /*
         * Set the static field that is here used to force uninstall: */
        UninstallMultiscoped.uninstallOn1stRun = true;

        for (String staticPart : new String[] {
            " force uninstall", " already installed"
        }) {
            for (int i = 1; i <= 3; ++i) {
                expect.pass(staticPart + " / dummy=" + i);
            }
            expect.pass(staticPart);
            expect.pass(UninstallMultiscoped.class.getSimpleName() + staticPart,
                    UninstallMultiscoped.class.getName() + staticPart);
        }
    }
}
