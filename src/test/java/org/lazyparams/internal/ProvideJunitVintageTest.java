/*
 * Copyright 2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.internal;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.lazyparams.LazyParams;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Henrik Kaipe
 */
public class ProvideJunitVintageTest {

    @Test void notifierMethod() throws NoSuchMethodException {

        // Given ...
        ProvideJunitVintage e = LazyParams.pickValue();

        // When ...
        Method notifierMethod = e.notifierMethod();

        // Then verify ...
        assertEquals(RunNotifier.class,
                notifierMethod.getDeclaringClass(), "Declaring class");
        assertTrue(Modifier.isPublic(notifierMethod.getModifiers()),
                "must be public");
        assertEquals(e.name(), notifierMethod.getName(), "name");
        assertThat(notifierMethod.getParameterTypes()[0])
                .as("parameter type")
                .isIn(Description.class, Failure.class);
    }
}
