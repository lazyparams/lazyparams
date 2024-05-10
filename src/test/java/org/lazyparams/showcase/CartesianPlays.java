/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.showcase;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lazyparams.LazyParams;
import org.lazyparams.showcase.ScopedLazyParameter.Combiner;
import org.lazyparams.showcase.ScopedLazyParameter.BasicFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Henrik Kaipe
 */
public class CartesianPlays {
    static final String fooSupporterX = "X-open", fooSupporterY = "Y-open";

    static boolean globalFooSupport;

    @BeforeAll static void pickGlobalSupport() {
        globalFooSupport = FalseOrTrue.pickBoolean("Global open");
    }

    int pick1stFrom3() {
        return LazyParams.pickValue("1stFrom3", 1,2,3);
    }
    void failFooOnA() {
        assertThat(LazyParams.pickValue("foo", "A","B","C","D"))
                .as("foo")
                .isNotEqualTo("A");
    }

    @Test void noCartesian() {
        pick1stFrom3();
        boolean fooSupportX = pickBoolean(fooSupporterX, null);
        Boolean fooSupportY = pickBoolean(fooSupporterY, null);
        if (globalFooSupport || fooSupportX || fooSupportY) {
            failFooOnA();
        }
        assertThat(pickBoolean(fooSupporterY, null))
                .as("Repeated pick of " + fooSupporterY)
                .isEqualTo(fooSupportY);
    }

    @Test void singleCartesian() {
        pick1stFrom3();
        BasicFactory<Boolean> booleanPickOnHub = ScopedLazyParameter
                .from(false, true)
                .fullyCombinedOn(new CartesianProductHub() {});
        Boolean fooSupportX = booleanPickOnHub.asParameter(fooSupporterX).pickValue();
        boolean fooSupportY = booleanPickOnHub.asParameter(fooSupporterY).pickValue();
        if (globalFooSupport || fooSupportX || fooSupportY) {
            failFooOnA();
        }
        assertThat(booleanPickOnHub.asParameter(fooSupporterX).pickValue())
                .as("Repeated pick of " + fooSupporterX)
                .isSameAs(fooSupportX);
    }

    @Test void individualCartesians() {
        pick1stFrom3();
        Combiner<Boolean,?> booleanCombiner = ScopedLazyParameter.from(false, true);
        boolean fooSupportX = booleanCombiner
                .fullyCombinedOn(new CartesianProductHub() {}).asParameter(fooSupporterX)
                .pickValue();
        boolean fooSupportY = booleanCombiner
                .fullyCombinedOn(new CartesianProductHub() {}).asParameter(fooSupporterY)
                .pickValue();
        if (globalFooSupport || fooSupportX || fooSupportY) {
            failFooOnA();
        }
    }

    @Test void repeatBoolean() {
        String boolName = "repeatedBool";
        CartesianProductHub firstHub = new CartesianProductHub() {};
        boolean first = pickBoolean(boolName, firstHub);
        if (globalFooSupport) {
            LazyParams.pickValue(s->s, "on same cartesian product hub ...");
            assertThat(pickBoolean(boolName, firstHub))
                    .as("Repeated boolean-pick with same name and hub")
                    .isEqualTo(first);
        } else {
            LazyParams.pickValue(s->s, "on new cartesian product hub:");
            pickBoolean(boolName, new CartesianProductHub() {});
        }
    }

    Boolean pickBoolean(String parameterName, CartesianProductHub hub) {
        return ScopedLazyParameter.from(false, true)
                .fullyCombinedOn(hub)
                .asParameter(parameterName).pickValue();
    }
}
