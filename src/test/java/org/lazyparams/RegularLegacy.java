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

import junit.framework.TestCase;

import static org.lazyparams.LazyParams.pickValue;

/**
 * @author Henrik Kaipe
 */
public class RegularLegacy extends TestCase {

    public void testNormal() {
        int i = pickValue("1st", 34, 42);
        String s = pickValue("2nd", "sdf", "dfwe");
//        System.out.println("1st=" + i + ", 2nd=" + s);
        if (34 == i && "dfwe".equals(s)) {
            throw new IllegalStateException("Fail here");
        }
    }

    public void testNoParams() {
//        System.out.println("NO PARAMS ARE HERE!!!");
    }

    public void testFailWithoutParams() {
//        System.out.println("NO PARAMS ARE HERE!!!");
        throw new RuntimeException("FAiLURE");
    }

    public void testManyParameters() {
        try {
            testNormal();
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
