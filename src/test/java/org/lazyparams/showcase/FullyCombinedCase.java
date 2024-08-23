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

import java.lang.annotation.RetentionPolicy;
import java.util.Formatter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.lazyparams.LazyParams.pickValue;
import static org.lazyparams.showcase.FullyCombined.pickFullyCombined;

/**
 * @author Henrik Kaipe
 */
public class FullyCombinedCase {

    int pairwise12(String parameterName) {
        return pickValue(parameterName, 1, 2);
    }
    int cartesian12(String parameterName) {
        return pickFullyCombined(parameterName, 1, 2);
    }

    @Test
    void pairwise12() {
        int a,b,c,d, e, f;
        a = pairwise12("a");
        b = pairwise12("b");
        c = pairwise12("c");
        d = pairwise12("d");
        e = pairwise12("e");
        f = pairwise12("f");
        pairwise12("g");
        pairwise12("h");
    }

    @Test
    void repeat_a_x5() {
        Object first, second, third, forth, fifth;
        first = pairwise12("a");                   /*1st*/
        second = pickValue("a", new Integer[] {1,2});/*2nd*/
        third = pickValue(i -> "a=" + i, 1,2);

        assertThat(pickValue("a", new Integer[] {1,2}))
                .as("Expected to be same as *2nd*, i.e. no new parameter!")
                .isEqualTo(second);

        forth = pickValue(i -> "a=" + i, new Object[] {1,2});

        assertThat(pairwise12("a"))
                .as("Expected to be same aa *1st*, i.e. no new paramter!")
                .isEqualTo(first);

        fifth = pickValue("a", new Object[] {1,2});

        assertThat(pickValue("a", new Integer[] {1,2}))
                .as("Expected to be same as *2nd*, i.e. no new parameter!")
                .isEqualTo(second);
    }

    @Test
    void cartesian12() {
        int a,b,c,d,e,f;
        a = cartesian12("a");
        b = cartesian12("b");
        c = cartesian12("c");
        assertThat(cartesian12("b"))
                .as("Repeated b must be same result again!")
                .isEqualTo(b);
        d = cartesian12("d");
        e = cartesian12("e");
        assertThat(cartesian12("e"))
                .as("Repeated e must be same result again!")
                .isEqualTo(e);
        f = cartesian12("f");
        if (6 == a + b + c + d + e + f) {
            throw new IllegalStateException("Need to fail a little");
        }
    }

    @Test
    void mixed12() {
        int a,b,c,d,e,f;
        a = pairwise12("a");
        b = pairwise12("b");
        c = cartesian12("c");
        d = cartesian12("d");
        e = pairwise12("e");
        f = pairwise12("f");
        if (11 <= a + b + c + d + e + f) {
            throw new IllegalStateException("Fail a little");
        }
    }

    @Test
    void failOn21() {
        int a,b,c,d,e,f;
        a = pairwise12("a");
        b = pairwise12("b");
        if (2 == a && 1 == b) {
            throw new IllegalStateException("Early failure");
        }
        c = pairwise12("c");
        d = pairwise12("d");
        e = pairwise12("e");
        f = pairwise12("f");
    }

    /** Also checks qronic display of parameter value */
    @Test
    void separateProducts() {
        ScopedLazyParameter.from(1).qronicly().asParameter(I -> "QRONIC FAILURE").pickValue();

        ScopedLazyParameter.Combiner<Integer,?> oneOrTwo = ScopedLazyParameter.from(1, 2);
        ScopedLazyParameter.BasicFactory<Integer> prd1 =
                oneOrTwo.fullyCombinedOn(new CartesianProductHub() {});
        ScopedLazyParameter.BasicFactory<Integer> prd2 =
                oneOrTwo.fullyCombinedOn(new CartesianProductHub() {});
        ScopedLazyParameter.BasicFactory<Integer> prd3 =
                oneOrTwo.fullyCombinedOn(new CartesianProductHub() {});

        int a,b,c,d,e,f;
        a = prd1.asParameter("a").pickValue();
        b = prd1.asParameter("b").pickValue();
        c = prd2.asParameter("c").pickValue();
        d = prd2.asParameter("d").pickValue();
        e = prd3.asParameter("e").pickValue();
        f = prd3.asParameter("f").pickValue();
        if (7 == b + c + d + e) {
            throw new IllegalStateException("Unlucky 7");
        }
    }

    /**
     * This test-method is a reference for comparing the resulting parameter
     * combinations with those of {@link #separateProducts()}.
     * It is not part of the unit test suite of this project.
     */
    @Test
    void pairwise1234() {
        ScopedLazyParameter.BasicFactory<Integer> pick123or4 =
                ScopedLazyParameter.from(1,2,3,4);
        pick123or4.asParameter("a").pickValue();
        pick123or4.asParameter("b").pickValue();
        pick123or4.asParameter("c").pickValue();
    }

    @Test
    void pairwise1234fromBasicFactoryWithExplicitId() {
        ScopedLazyParameter.BasicFactory<Integer> pick123or4 = ScopedLazyParameter
                .from(1,2,3,4).withExplicitParameterId("idOn1234");
        int a = pick123or4.asParameter("a").pickValue();
        int b = pick123or4.asParameter("b").pickValue();
        int c = pick123or4.asParameter("c").pickValue();
        assertThat(a).as("a - should be equal to c").isEqualTo(c);
        assertThat(b).as("b - should be equal to c").isEqualTo(c);
    }

    @Test
    void enums() {
        RetentionPolicy tmp = pickFullyCombined();
        Formatter.BigDecimalLayoutForm tmp2 = pickFullyCombined();
        assertThat(FullyCombined.<RetentionPolicy>pickFullyCombined())
                .as("Repeated default enum-pick on RetentionPolicy")
                .isSameAs(tmp);
        FullyCombined.pickFullyCombined(RetentionPolicy.values());
        FullyCombined.pickFullyCombined(
                "final_layout", Formatter.BigDecimalLayoutForm.values());
    }
}
