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

import java.io.IOException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.lazyparams.showcase.Qronicly;
import org.lazyparams.showcase.ToPick;

/**
 * Try some functionality with little coverage from other tests.
 *
 * @author Henrik Kaipe
 */
public class WithExpectRule {

    @Rule
    public final ExpectedException exceptionRule = ExpectedException.none();

    @Test @SuppressWarnings({"ThrowableInstanceNotThrown"})
    public void test() throws Exception {
        if (Boolean.TRUE.equals(Arrays
                .asList(new int[] {2,3,5}, true, false).stream()
                .collect(ToPick.as("arrayOrExpect")))) {
            exceptionRule.expect(Exception.class);
        }
        throw Qronicly.pickValue(e->e.getClass().getSimpleName(), new Exception[] {
            new IOException(), new NoSuchElementException()
        });
    }
}
