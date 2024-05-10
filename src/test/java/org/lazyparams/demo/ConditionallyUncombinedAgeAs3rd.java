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

import org.junit.jupiter.api.Test;
import org.lazyparams.LazyParams;
import org.lazyparams.showcase.FullyCombined;
import org.lazyparams.showcase.ScopedLazyParameter;

/**
 * @author Henrik Kaipe
 */
public class ConditionallyUncombinedAgeAs3rd {

    @Test
    public void test() {
        int jvPick = FullyCombined.pickFullyCombined("jv", 1,2,3,4,5,6);
        boolean jv1or2 = 1 == jvPick || 2 == jvPick;
        AgeRange ageRange = AgeRange.pick(
                jv1or2 ? "1 or 2" : 6 == jvPick ? "case 6" : "case default");
        ageRange.pickAge(false == jv1or2);
        if (jv1or2 || 6 == jvPick && AgeRange.ELDERLY != ageRange) {
            return;
        }
        LazyParams.pickValue("plf", 7,8,9,10);
        LazyParams.pickValue("impl", 5,6,7,8,9,10);
    }

    enum AgeRange {
        ELDERLY(91,92,93,94,95,96,97,98,99),
        GROWNUP(21,22,23,24,25,26,27,28,29,30),
        YOUNG(11,12,13,14,15,16,17,18,19,20),
        CHILD(1,2,3,4,5,6,7,8,9,10);

        final Integer[] ageSelection;
        private AgeRange(Integer... ageSelection) {
            this.ageSelection = ageSelection;
        }

        Integer pickAge(boolean combine) {
            ScopedLazyParameter.Combiner<Integer,?> combiner =
                    ScopedLazyParameter.from(ageSelection);
            ScopedLazyParameter.BasicFactory<Integer> factory =
                    combine && ELDERLY == this ? combiner : combiner.notCombined();
            return factory.asParameter("age").pickValue();
        }

        static AgeRange pick(Object extraIdDetail) {
            return ScopedLazyParameter.from(values())
                    .withExtraIdDetails(extraIdDetail)
                    .asParameter("age-range").pickValue();
        }
    }
}
