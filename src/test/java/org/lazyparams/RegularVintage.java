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


import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.lazyparams.LazyParams.pickValue;

/**
 * @author Henrik Kaipe
 */
public class RegularVintage {

    /** To verify that repetitions don't repeat
     * <code>@BeforeClass</code> or <code>@AfterClass</code>!
     */
    private static final AtomicBoolean beforeClassIsOn =
            new AtomicBoolean(false);

    @BeforeClass
    public static void beforeClass() {
        assertEquals("One and only beforeClass invocation",
                false, beforeClassIsOn.getAndSet(true));
    }
    @AfterClass
    public static void afterClass() {
        assertEquals("Reset beforeClassIsOn",
                true, beforeClassIsOn.getAndSet(false));
    }

    @Before
    public void assertBeforeClassIsOn() {
        assertEquals("... wheras after-class is not",
                true, beforeClassIsOn.get());
    }

    @Test
    public void normal() {
        int i = pickValue("1st", 34, 42);
        String s = pickValue("2nd", "sdf", "dfwe");
//        System.out.println("1st=" + i + ", 2nd=" + s);
        if (34 == i && "dfwe".equals(s)) {
            throw new IllegalStateException("Fail here");
        }
    }

    @Test
    public void noParams() {
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
//        System.out.println("tjoho=" + tjoho);
    }
}
