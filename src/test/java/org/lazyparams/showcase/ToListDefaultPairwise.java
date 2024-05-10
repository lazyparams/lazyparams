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
 * Evaluates a bunch of usage scenarios for {@link ToList}.
 * This test relies heavily on reuse of test-instance when a Jupiter test-method
 * is repeated by LazyParams.
 *
 * @author Henrik Kaipe
 */
public class ToListDefaultPairwise {

    /**
     * This parameter value is picked in static scope.
     * Jupiter does, unlike JUnit-4, create test instances in static scope and
     * therewith this parameter is introduced in static scope. Jupiter does
     * create a separate test-instance for each test-method but the above
     * value will nonetheless be the same on all those instances until next
     * iteration of static scope when new test instances are created etc.
     * As default LazyParams will reuse the test instance when iterating through
     * parameter values of a test method, only changing to next test instance
     * when done as continuing with next test method.
     * <br/>
     * This test makes use of this test instance reuse behavior when repeating
     * a parameterized test method but the traditional "normal" is to have a
     * new test-instance on each test-method repetition. If the traditional
     * normal is desired then a special annotation
     * {@link LazyInstantiateTestClass} can be used to delay test instance
     * creation until test method scope is opened, therewith forcing creation
     * of new test instance each time the test method is repeated.
     */
    final int nbrOfListSourceValues = LazyParams.pickValue("src-size", 0,1,2,3,4,5,6,7,8,9);

    /**
     * Used by {@link #intuitiveSeedsOnOneOrTwo()}
     */
    ScopedLazyParameter.CombiningCollector.Seeds artificialSeeds;

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
    public void intuitiveSeedsOnOneOrTwo() {
        Object[] forceRepeatWithTheseDummyValues = new Object[
                nbrOfListSourceValues + nbrOfListSourceValues * (nbrOfListSourceValues - 1) / 2];
        Arrays.fill(forceRepeatWithTheseDummyValues, "");
        ScopedLazyParameter.from(forceRepeatWithTheseDummyValues).notCombined()
                .asParameter("").pickValue();

        if (null == artificialSeeds) {
            artificialSeeds = new ScopedLazyParameter.CombiningCollector.Seeds() {
                int major = - 1, minor = - 1;
                @Override
                public int next(int bound) {
                    if (nbrOfListSourceValues <= bound) {
                        ++major;
                        return major %= bound;
                    } else {
                        ++minor; minor %= bound;
                        DisplayVerbosity.VERBOSE.display(
                                new Object(), v->v, major+","+minor);
                        return minor;
                    }
                }
            };
        }
        List<String> allSrc = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
        allSrc = allSrc.subList(0, nbrOfListSourceValues);
        final List<Object> result = ToList
                .combineOneOrTwo().applyOn(allSrc, artificialSeeds);
        LazyParams.pickValue(String::valueOf, result);
        assertThat(result.size()).as("Size of combined 1 or 2")
                .isLessThanOrEqualTo(2);
        assertNoDuplicate(result);
    }

    /**
     * Some intentional failures are fired when running this test.
     * They are failed duplication check on the list results
     * from {@link ToList#combineOneOrTwo()}. They happen because the list
     * of input parameter values has an intentionally duplicated null-value
     * that occurs twice in the list of parameter values.
     */
    @Test
    public void actualSeedsOnOneOrTwo() {
        List<String> allSrc = Arrays.asList(null, "b", "C", "4", "5", "6", null, "HH", "!!");
        List<String> result = ToList.combineOneOrTwo().pickList("1or2",
                allSrc.subList(0, nbrOfListSourceValues).toArray(new String[0]));
        assertThat(result.size()).as("Size of combined 1 or 2")
                .isLessThanOrEqualTo(2);
        assertNoDuplicate(result);
    }

    @Test
    public void oneOrPermutationOfTwo() {
        List<String> allSrc = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
        String noisePrefix = "Noise prefix: _ ";
        List<String> result = allSrc.stream().limit(nbrOfListSourceValues)
                .collect(ToPick.as(
                        list -> "1or2=" + Arrays.asList(list.toArray()),
                        ToList.combineOneOrPermutationOfTwo()::pickList));
        assertThat(result.size()).as("Size of combined 1 or permutation of 2")
                .isLessThanOrEqualTo(2);
        assertNoDuplicate(result);
    }

    @Test
    public void individuallyCombined() {
        List<Integer> allSrc = Arrays.asList(11, 12, 13, 14, 15, 16, 17, 18, 19);
        List<Integer> result = ToList.combineElementsIndividually().pickList(
                l -> "all={size=" + l.size() + "}" + l,
                allSrc.subList(0, nbrOfListSourceValues).toArray(new Integer[0]));
        assertNoDuplicate(result);
    }

    @Test
    public void permutations() {
        List<Character> allSrc = Arrays.asList('z', 'Y', 'x', '9', '8', '7', 'W', 'v', '!');
        List<Character> result = allSrc.stream().limit(nbrOfListSourceValues)
                .collect(ToPick.from())
                .asParameter("permutation", ToList.combinePermutation())
                .pickValue();
        assertThat(result).hasSize(nbrOfListSourceValues);
        assertNoDuplicate(result);
    }
}
