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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lazyparams.LazyParams;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Henrik Kaipe
 */
public class ToListFullyCombined {

    /** @see ToListDefaultPairwise#nbrOfListSourceValues */
    final int nbrOfListSourceValues = LazyParams.pickValue("src-size", 1,2,3,4,5,6,7,8,9,10);

    final long startTime = System.currentTimeMillis();
    final Set<List<?>> combinationsLog = new HashSet<>();

    @BeforeEach void displayExecTime() {
        DisplayVerbosity.VERBOSE.display(new Object(),
                startTime -> {
                    String time = "000"
                            + (System.currentTimeMillis() - startTime);
                    time = time.substring(time.length() - 4, time.length());
                    return time.charAt(0) + "." + time.substring(1) + 's';
                },
                startTime);
    }

    void assertNoDuplicate(List<?> resultList) {
        assertThat(combinationsLog.add(resultList))
                .as("Must not be duplicate of " + resultList)
                .isTrue();
    }

    @Test
    public void oneOrTwo() {
        LazyParams.currentScopeConfiguration().setMaxFailureCount(21);
        List<String> allSrc = Arrays.asList(null, "b", "C", "4", "5", "6", null, "HH", "!!", "10");
        List<?> result =  ScopedLazyParameter
                .from(allSrc.subList(0, nbrOfListSourceValues).toArray(new String[0]))
                .fullyCombinedGlobally()
                .asParameter(l -> "1or2=" + l, ToList.combineOneOrTwo())
                .pickValue();
        assertThat(result.size()).as("Size of combined 1 or 2")
                .isLessThanOrEqualTo(2);
        assertNoDuplicate(result);
    }

    @Test
    public void individuallyCombined() {
        List<Integer> allSrc = Arrays.asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20);
        List<Integer> result = ScopedLazyParameter
                .from(allSrc.subList(0, nbrOfListSourceValues).toArray(new Integer[0]))
                .fullyCombinedGlobally()
                .asParameter("individuals", ToList.combineElementsIndividually())
                .pickValue();
        assertNoDuplicate(result);
    }

    /**
     * Full combining on a cartesian product hub will only occur when number of
     * parameter values are three or less, i.e. with list permutation as only
     * parameter the resolved lists will be the same as for the default pairwise
     * combining when there are only three values or less. (Fully combined lists
     * will form more combinations when there are additional parameters, however.)
     * <br/>
     * With four values the full combining will resolve all permutations. But
     * in practice only three values are combined on the cartesian product - and
     * then the remaining combinations are resolved as cartesian tuple is
     * combined with the last value with the pairwise strategy.
     * <br/>
     * With five or six values there will be 60 permutations resolved. 3 values
     * and a factor will be fully combined with its tuples pairwise combined
     * with fifth value and remaining factor on forth. A factor of sixth value
     * will also enjoy pairwise combining but it will not increase the number
     * permutations (because factors of 6 (i.e. 2 and 3) are smaller than 5).
     * <br/>
     * Starting from seven there will be four values on the cartesian product,
     * which tuples will now be numbered 24 that combine with fifth value to
     * form 120 permutations, when there are seven of eight values.
     * (Pairwise combining of factor (3) on sixth value
     * is also there but still does not effect the total number permutations.)
     * <br/>
     * With nine values or more the sixth value will also be pairwise combined
     * with the cartesian permutations, which four values will still form
     * 24 tuples times 6 results in 144 permutations.
     * <br/>
     * From here on the number of permutations increase with more values but
     * slower from here on. - The seventh value will be part of the pairwise
     * combining only after at least 13 input parameter values. The fifth
     * value will be melded onto the cartesian product when there are at least
     * 55 input values.
     * <br/>
     * Obviously fully combined permutations do not include all permutations,
     * far from it, and its because with each extra value on the list the
     * total number of permutations grows exponentially, therewith quickly
     * making it ridiculous to navigate through all permutations.:
     * <pre>
     * Number of values in list: 0 1 2 3  4   5   6    7     8      9      10
     * Possible permutations:    1 1 2 6 24 120 720 5040 40320 362880 3628800
     * Combined permutations:    1 1 2 6 24  60  60  120   120    144     144
     *Fully combined seed tuples:1 1 2 6 12  12  12   24    24     24      24
     * </pre>
     */
    @Test
    public void permutations() {
        LazyParams.currentScopeConfiguration().setMaxTotalCount(144);
        List<Character> allSrc = Arrays.asList('z', 'Y', 'x', '9', '8', '7', 'W', 'v', '!', 'T');
        allSrc = allSrc.subList(0, nbrOfListSourceValues);
        List<Character> result = ScopedLazyParameter
                .from(allSrc.toArray(new Character[0]))
                .fullyCombinedGlobally()
                .asParameter("permutation", ToList.combinePermutation())
                .pickValue();
        assertThat(result)
                .hasSize(nbrOfListSourceValues);
        assertNoDuplicate(result);
    }
}
