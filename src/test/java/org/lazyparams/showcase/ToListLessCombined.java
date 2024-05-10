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
public class ToListLessCombined {

    /** @see ToListDefaultPairwise#nbrOfListSourceValues */
    final int nbrOfListSourceValues = LazyParams.pickValue("src-size", 1,2,3,4,5,6,7,8,9);

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
        List<String> allSrc = Arrays.asList(null, "b", "C", "4", "5", "6", null, "HH", "!!");
        List<?> result = allSrc.stream().limit(nbrOfListSourceValues)
                .collect(ToPick.from())
                .notCombined()
                .asParameter(l -> "1or2=" + l, ToList.combineOneOrTwo())
                .pickValue();
        assertThat(result.size()).as("Size of combined 1 or 2")
                .isLessThanOrEqualTo(2);
        assertNoDuplicate(result);
    }

    @Test
    public void oneOrPermutationOfTwo() {
        List<String> allSrc = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
        List<String> result = ScopedLazyParameter
                .from(allSrc.subList(0, nbrOfListSourceValues).toArray(new String[0]))
                .notCombined()
                .asParameter("1or2", ToList.combineOneOrPermutationOfTwo())
                .pickValue();
        assertThat(result.size()).as("Size of combined 1 or permutation of 2")
                .isLessThanOrEqualTo(2);
        assertNoDuplicate(result);
    }

    @Test
    public void individuallyCombined() {
        List<Integer> allSrc = Arrays.asList(11, 12, 13, 14, 15, 16, 17, 18, 19);
        List<Integer> result = allSrc.stream().limit(nbrOfListSourceValues)
                .collect(ToPick.from())
                .notCombined()
                .asParameter("individuals", ToList.combineElementsIndividually())
                .pickValue();
        assertNoDuplicate(result);
    }

    @Test
    public void permutations() {
        List<Character> allSrc = Arrays.asList('z', 'Y', 'x', '9', '8', '7', 'W', 'v', '!');
        List<Character> result = allSrc.stream().limit(nbrOfListSourceValues).collect(
                ToPick.as("permutation", ToList.combinePermutation()::pickList));
        assertThat(result)
                .hasSize(nbrOfListSourceValues);
        assertNoDuplicate(result);
    }
}
