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

import org.junit.Before;
import org.junit.runner.RunWith;
import org.lazyparams.RegularVintage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Henrik Kaipe
 */
@RunWith(PowerMockRunnerLight.class)
public class PowermockRegular extends RegularVintage {

    @Before
    public void thereShouldBeAMethodBackToJunitClassloader() {
        assertThat(ProvideJunitVintage.FilteredRunnerAdvice.recordOnJunitClassloader)
                .as("Method for recording runner-stuff on JUnit classloader")
                .isNotNull();
    }
}
