/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.showcase;

import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lazyparams.internal.PowerMockRunnerLight;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Henrik Kaipe
 */
@RunWith(PowerMockRunnerLight.class)
public class PowermockedScopedContextFeatures {

    static final ScopedLazyParameter<StringBuilder> stringBuilderParam =
            ScopedLazyParameter.from("first","second").asParameter("built",
                    (values,seeds)
                    -> new StringBuilder(values.get(seeds.next(values.size()))));

    @Test
    public void threeParamsFullyCombined() {
        FullyCombined.pickFullyCombined("a", 1,2);
        Integer b = FullyCombined.pickFullyCombined("b", 1,2);
        FullyCombined.pickFullyCombined("c", 1,2);
        assertEquals("Repeat b", b, FullyCombined.pickFullyCombined("b", 1,2));
    }

    @Test
    public void scopeCachedValue() {
        StringBuilder starter = stringBuilderParam.pickValue();
        starter.append(" start");
        StringBuilder ender = stringBuilderParam.pickValue();
        ender.append(" end");
        assertSame(starter, ender);
    }

    @Test
    public void runPermutation() {
        ToList.combinePermutation().<Supplier<String>>pickList(
                list -> list.stream().map(Supplier::get).collect(Collectors.joining("->")),
                () -> "A", () -> "B", () -> "C");
    }
}
