/*
 * Copyright 2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.showcase;

import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.lazyparams.LazyParams;

/**
 * To try performance with one parameter having many (thousands) of values.
 * It was specifically designed to check how the performance of a reused
 * {@link ScopedLazyParameter} instance (with lots of values) can benefit from
 * having the constructor calculate {@link ToStringKey#hashCode()} preemptively.
 *
 * @author Henrik Kaipe
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OneWithMany {

    private static Integer[] values;

    ScopedLazyParameter<Integer> reusedParameter;

    ScopedLazyParameter<Integer> parameter() {
        return ScopedLazyParameter.from(values).asParameter("v");
    }

    @BeforeAll
    static void initParameterValues() {
        values = IntStream.range(0, 4000)
                .mapToObj(Integer::valueOf)
                .toArray(Integer[]::new);
    }
    @AfterAll
    static void tearDown() {
        values = null;
    }

    @Order(1)
    @Test void warmup() {
        LazyParams.pickValue("dummy", 0,1,2,3,4,5,6,7,8,9);
    }

    @Order(2)
    @Test void reusedParameter() {
        if (null == reusedParameter) {
            reusedParameter = parameter();
        }
        reusedParameter.pickValue();
    }

    @Order(3)
    @Test void freshParameter() {
        parameter().pickValue();
    }

    @AfterEach void setMaxRepeat() {
        LazyParams.currentScopeConfiguration().setMaxTotalCount(values.length);
    }
}
