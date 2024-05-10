/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.core;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import org.lazyparams.LazyParams;
import org.lazyparams.showcase.CartesianProductHub;
import org.lazyparams.showcase.Ensembles;
import org.lazyparams.showcase.FalseOrTrue;
import org.lazyparams.showcase.FullyCombined;
import org.lazyparams.showcase.InAnyOrder;
import org.lazyparams.showcase.Qronicly;
import org.lazyparams.showcase.ScopedLazyParameter;
import org.lazyparams.showcase.ToList;
import org.lazyparams.showcase.ToPick;

/**
 * LAZy parameter values combinER
 *
 * @author Henrik Kaipe
 */
public class Lazer {

    private String crumbsLog = "";
    /**
     * Used by {@link ValueStats#isTemporarilyParkedOnPrimaryValue()}
     */
    private int countParkedPrimaryValues = 0;

    private final Map<String,Lazynition> reservedCrumbs = new HashMap<String,Lazynition>();
    private final List<ValueInformation> allValueInfos = new ArrayList<ValueInformation>();

    private final List<ValueInformation> pickCrumbValues =
            new ArrayList<ValueInformation>();
    private final Map<Lazynition,ValueInformation[]> coreStatKeys =
            new HashMap<Lazynition, ValueInformation[]>();
    /**
     * Each entry of {@link #coreStatKeys} will here have an entry showing the
     * stack for the parameter's initial introduction.
     */
    private final Map<Lazynition,ExpectedParameterRepetition> firstIntroductionStacks =
            new IdentityHashMap<Lazynition,ExpectedParameterRepetition>();

    public void startNew() {
        registerEndOfLineAtLastCrumbValue();
        crumbsLog = "";
        countParkedPrimaryValues = 0;
        pickCrumbValues.clear();
    }

    private void registerEndOfLineAtLastCrumbValue() {
        if (2 <= pickCrumbValues.size()) {
            pickCrumbValues.remove(pickCrumbValues.size() - 1)
                    .registerEndOfLine();
        }
    }

    public boolean pendingCombinations() throws ExpectedParameterRepetition {
        if (reservedCrumbs.containsKey(crumbsLog)) {
            throw firstIntroductionStacks.get(reservedCrumbs.get(crumbsLog));
        }

        for (ValueInformation[] valueInfoOfOptions : coreStatKeys.values()) {
            for (ValueInformation valueInfo : valueInfoOfOptions) {
                if (0 < valueInfo.stats.forwardRequestCount
                        || valueInfo.stats.totalCount <= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public int pick(Object paramId, boolean combinePairwise, int numberOfValues) {
        int valuesMax = 65480;
        if (valuesMax < numberOfValues) {
            throw new IllegalArgumentException("At most " + valuesMax
                    + " is supported - but was " + numberOfValues);
        }
        return pick(paramId, combinePairwise, (char)numberOfValues);
    }

    private int pick(Object paramId, boolean combinePairwise, char numberOfValues) {
        Lazynition paramDefinition = new Lazynition(paramId, combinePairwise, numberOfValues);
        ValueInformation[] valueOptions = coreStatKeys.get(paramDefinition);
        if (null == valueOptions) {
            valueOptions = new ValueInformation[numberOfValues];
            for (char i = 0; i < numberOfValues; ++i) {
                valueOptions[i] = new ValueInformation(paramDefinition, i);
            }
            coreStatKeys.put(paramDefinition, valueOptions);
            firstIntroductionStacks.put(paramDefinition,
                    new ExpectedParameterRepetition("... at '" + crumbsLog + "' pick"));
        } else {
            paramDefinition = valueOptions[0].parameterDefinition;
            for (ValueInformation valueAlreadyPicked : pickCrumbValues) {
                if (paramDefinition == valueAlreadyPicked.parameterDefinition) {
                    return valueAlreadyPicked.itemValueIndex;
                }
            }
        }
        this.<Error>enforceHistoricalCrumbsConsistency(paramDefinition);
        ValueInformation pickedValue = makePick(valueOptions);
        pickedValue.registerPick();
        return pickedValue.itemValueIndex;
    }

    private ValueInformation makePick(ValueInformation[] valueOptions) {
        ValueInformation bestSoFar = null;
        for (int i = valueOptions.length; 0 <= --i;) {
            if (valueOptions[i].stats.isBetterThan(bestSoFar)) {
                bestSoFar = valueOptions[i];
            }
        }
        return bestSoFar;
    }

    private <E extends Throwable> void enforceHistoricalCrumbsConsistency(
            Lazynition paramDefinition)
    throws E {
        Lazynition alreadyReserved = reservedCrumbs.put(crumbsLog, paramDefinition);

        if (null != alreadyReserved && alreadyReserved != paramDefinition) {
            /* Put back and raise an error! */
            reservedCrumbs.put(crumbsLog, alreadyReserved);
            ExpectedParameterRepetition inconsistency =
                    firstIntroductionStacks.get(alreadyReserved);
            try {
                inconsistency.addSuppressed(new Throwable("INCONSISTENCY DETECTED!"));
            } catch (Throwable addSuppressed_requires_java7_butJustIgnoreItOnMoreAncientJvms) {}
            throw (E) inconsistency;

        } else {
            /* Happy path with consistent crumbs history during repetition */
        }
    }

    private final class ValueStats {
        private final boolean combined;
        /**
         * Used by {@link #isTemporarilyParkedOnPrimaryValue()}
         */
        private final int lookbackSeed;
        /**
         * Satisfied when >= 1
         * This is the first combine condition to satisfy
         * (unless {@link #isTemporarilyParkedOnPrimaryValue()}).
         */
        private volatile int totalCount = 0;
        /**
         * Is satisfied when == 0
         * This - in a weighted combination with {@link #enablerCount} -
         * forms the secondary condition to satisfy.
         * This condition is initially satisfied but will become pending
         * as forward value-picks register pending combos.
         * <br/>
         * This property is never used for a value on an uncombined parameter.
         * (i.e. if <code>{@link #combined} == false</code>).
         */
        private volatile int forwardRequestCount = 0;
        /**
         * Is satisfied when == 0
         * This - in a weighted combination with {@link #forwardRequestCount} -
         * forms the secondary condition to satisfy.
         * This condition is initially satisfied but will become pending
         * as forward value-picks register pending combos.
         */
        private volatile int enablerCount;

        /**
         * Satisfied when empty!
         * This is the least prioritized condition to satisfy - but it must
         * none-the-less be satisfied!
         * Keep in mind this condition set is volatile and can be
         * repopulated with additional pending combos after briefly
         * being satisfied.
         * In case multiple value options have the chance to satisfy the
         * same number of combos - then the option which satisfied combos
         * having the lowest sum of forwardRequestCount shall have preference,
         * because combos with higher forwardRequestCount are more likely
         * to soon show up again.
         * <br/>
         * This map is also used for uncombined parameter values (i.e. those
         * having <code>{@link #combined} == false)</code>, even though they
         * never have any pending combos. Instead an uncombined parameter value
         * that still has <code>{@link #totalCount} == 0</code> uses the
         * null-entry to reference a set of ValueInformation instances that
         * can enable its first pick.
         */
        private final Map<ValueInformation,Set<ValueInformation>> pendingCombos =
                new HashMap<ValueInformation, Set<ValueInformation>>();

        /**
         * Satisfied when all value counters are zero!
         * This is just an optimization that keeps counts on how many times
         * each {@link ValueInformation} instance occurs in map
         * {@link #pendingCombos}:
         * Counter initial value is minus {@link Integer#MAX_VALUE} and is
         * increased by plus {@link Integer#MAX_VALUE} when removed from
         * {@link #pendingCombos} key set.
         * <br/>
         * This set is never used for a value on an uncombined parameter.
         * (i.e. if <code>{@link #combined} == false</code>).
         */
        private final ValueInfoCounterSet comboEnablerCounts =
                new ValueInfoCounterSet(-Integer.MAX_VALUE);

        /**
         * Not a condition to satisfy - but a final help for nice distribution
         * in case other conditions are tied.
         * Satisfied {@link #pickCrumbValues} will be searched for count == 1.
         * If multiple options have the same number of {@link #pickCrumbValues}
         * at count == 1 then the option with lowest
         * {@link #forwardRequestCount} sum shall be picked.
         * If there is still a tie then continue with count == 2 etc ...
         */
        private final ValueInfoCounterSet satisfiedComboCounts = new ValueInfoCounterSet();

        /**
         * Transient scoring info that will be reset each time
         * a pick is made for new upstream picks.
         */
        private final List<Long> levelScores = new ArrayList<Long>() {
            @Override
            public Long get(int index) {
                for (int i = size(); i <= index; ++i) {
                    add(0L);
                }
                return super.get(index);
            }
        };

        /**
         * Keeps copies of the string-of-crumbs after which this
         * value has ended up being the last value picked.
         * Alternatively where all combinations of later parameter values are
         * already walked through. This is a safe-guard against repeating
         * value-picks that will lead nowhere new, i.e. that will not lead to
         * unknown grounds where new combinations of parameter values can happen.
         */
        private final Set<String> endOfLineKeys = new HashSet<String>();

        ValueStats(boolean combine, int primaryCountExceptionLookbackLength) {
            this.combined = combine;
            if (false == combine) {
                pendingCombos.put(null, new ValueInfoCounterSet());
            }
            this.lookbackSeed = primaryCountExceptionLookbackLength;
        }

        private boolean isTemporarilyParkedOnPrimaryValue() {
            if (/*Dont apply unless there is a live lookback seed:*/
                    lookbackSeed <= totalCount
                    || 10  <= totalCount //separate max when parameter has many values
                    ) {
                return false;
            }

            /*********
             * To determine best(?) value has been a trial-and-error process.
             * This initial value of this variable is not a very scientific
             * approach on how to patch a little optimizing on the lazy
             * algorithm that is used to combine records for covering all
             * pairwise combinations.
             */
            int lookbackCountdown = lookbackSeed - lookbackSeed / 5
                    + countParkedPrimaryValues / (lookbackSeed - 1);

            ListIterator<ValueInformation> lookbackIteration =
                    pickCrumbValues.listIterator(pickCrumbValues.size());
            while (lookbackCountdown <= lookbackIteration.nextIndex()) {
                ValueInformation lookbackInfo = lookbackIteration.previous();

                if (false == lookbackInfo.stats.combined
                        || 1 == lookbackInfo.stats.lookbackSeed) {
                    continue;

                } else if (2 <= lookbackInfo.stats.totalCount) {
                    return false;

                } else if (0 == --lookbackCountdown) {
                    ++countParkedPrimaryValues;
                    return true;
                }
            }
            return false;
        }

        /**
         * @return weighted score that combines {@link #forwardRequestCount}
         *         and {@link #enablerCount}
         */
        long forwardScore() {
            return (1 + forwardRequestCount) * (2 + enablerCount);
        }

        synchronized private List<Long> evaluateLevels() {
            if (false == pickCrumbValues.isEmpty() && levelScores.isEmpty()) {
                int crumbScoreSize = Integer.MAX_VALUE / pickCrumbValues.size();
                for (ValueInformation upstreamCombo : pickCrumbValues) {
                    int level = satisfiedComboCounts.get(upstreamCombo);
                    if (0 == level && false == upstreamCombo.stats.combined) {
                        level = 1;
                    }
                    levelScores.set(level, levelScores.get(level)
                            + crumbScoreSize
                            - upstreamCombo.stats.forwardScore());
                }
                long levelZeroScore = levelScores.get(0);
                if (0 < levelZeroScore) {
                    /* Adjust for pending combos: */
                    levelZeroScore -= crumbScoreSize / (1+pendingCombos.size());
                    /* Deduct forward scores enablers
                     * that are not pending combos: */
                    for (ValueInformation enabler : comboEnablerCounts) {
                        levelZeroScore -= enabler.stats.forwardScore();
                    }
                    levelScores.set(0, levelZeroScore);
                }
            }
            return levelScores;
        }

        synchronized private boolean isBetterThan(ValueInformation bestSoFar) {
            if (false == pickCrumbValues.isEmpty()
                    && endOfLineKeys.contains(crumbsLog)) {
                return null == bestSoFar;
            }
            levelScores.clear(); /* Level-scores must be reset here! */
            if (combined) {
                /*
                 * Walk through crumbs to look for new combos to satisfy!
                 */
                for (ValueInformation upstreamCombo : pickCrumbValues) {
                    if (upstreamCombo.stats.combined
                            && 0 == satisfiedComboCounts.get(upstreamCombo)) {
                        Set<ValueInformation> enablers =
                                pendingCombos.get(upstreamCombo);
                        if (null == enablers) {
                            pendingCombos.put(upstreamCombo,
                                    enablers = new ValueInfoCounterSet());
                            ++upstreamCombo.stats.forwardRequestCount;
                        }
                        for (ValueInformation upstreamEnablr : pickCrumbValues){
                            if (upstreamEnablr != upstreamCombo
                                    && enablers.add(upstreamEnablr)) {
                                ++upstreamEnablr.stats.enablerCount;
                                comboEnablerCounts.increase(upstreamEnablr);
                            }
                        }
                    }
                }
            } else if (totalCount <= 0) {
                Set<ValueInformation> enablers = pendingCombos.get(null);
                for (ValueInformation upstreamEnablr : pickCrumbValues) {
                    if (enablers.add(upstreamEnablr)) {
                        ++upstreamEnablr.stats.enablerCount;
                    }
                }
            }
            if (null == bestSoFar || 0 == totalCount
                    || 0 == bestSoFar.stats.totalCount && bestSoFar.itemValueIndex < 10
                    && isTemporarilyParkedOnPrimaryValue()
                    || bestSoFar.stats.endOfLineKeys.contains(crumbsLog)) {
                return true;
            }
            ValueStats bestStatsSoFar = bestSoFar.stats;
            if (0 == bestStatsSoFar.totalCount
                    || forwardRequestCount < bestStatsSoFar.forwardRequestCount) {
                return false;
            } else  if (bestStatsSoFar.forwardRequestCount < forwardRequestCount) {
                return true;
            }
            evaluateLevels();
            List<Long> bestSoFarLevels = bestStatsSoFar.evaluateLevels();
            if (1 <= levelScores.size()) {
                long level0Diff = levelScores.get(0) - bestSoFarLevels.get(0);
                if (0 != level0Diff) {
                    return 0 < level0Diff;
                }
            }
            /* Mind enabler count and total count before iterating levels above 0: */
            if (weightedEnablerCount() != bestStatsSoFar.weightedEnablerCount()) {
                return bestStatsSoFar.weightedEnablerCount() < weightedEnablerCount();
            }
            if (enablerCount != bestStatsSoFar.enablerCount) {
                return bestStatsSoFar.enablerCount < enablerCount;
            }
            for (int i = 1; i < levelScores.size(); ++i) {
                long levelScoreDiff = levelScores.get(i) - bestSoFarLevels.get(i);
                if (0 != levelScoreDiff) {
                    return 0 < levelScoreDiff;
                }
            }
            /* Total count is the final tie breaker: */
            return totalCount <= bestStatsSoFar.totalCount;
        }

        private double weightedEnablerCount() {
            /*
             * Enabler count is reduced by total count for uncombined values and
             * values that don't have any pending forward requests:
             */
            assert combined || 0 == forwardRequestCount
                    : "Non-combined parameter value must have zero"
                    + " forwardRequestCount - but was " + forwardRequestCount;
            return combined ? enablerCount
                    : (1 + enablerCount) * Math.pow(0.5, totalCount);
        }
    }

    /**
     * ValueInformation set implementation that is optimized for the Lazer algorithms.
     * With many parameters and values it can make algorithms run more than
     * 4 times faster than with regular hash-sets and hash-maps.
     */
    private final class ValueInfoCounterSet extends AbstractSet<ValueInformation> {
        private final int initialValue;

        private int[] counts = new int[0];
        private int size;

        ValueInfoCounterSet(int initialDefaultValue) {
            this.initialValue = initialDefaultValue;
        }
        ValueInfoCounterSet() { this(0); }

        /**
         * Iterates as expected when being used as set.
         * Only iterates entries with positive counts when used as count map!
         */
        @Override
        public Iterator<ValueInformation> iterator() {
            return new Iterator<ValueInformation>() {
                int nextIndex = -1;
                int lastIndex = -1;
                { prepareNext(); }

                @Override
                public boolean hasNext() {
                    return nextIndex < Integer.MAX_VALUE;
                }
                @Override
                public ValueInformation next() {
                    try {
                        return allValueInfos.get(nextIndex);
                    } finally {
                        prepareNext();
                    }
                }
                @Override
                public void remove() {
                    if (0 <= lastIndex) {
                        removeAt(lastIndex);
                    }
                }
                void prepareNext() {
                    lastIndex = nextIndex;
                    while (++nextIndex < counts.length) {
                        if (0 < counts[nextIndex]) {
                            return;
                        }
                    }
                    nextIndex = Integer.MAX_VALUE;
                }
            };
        }

        int add(ValueInformation vInfo, int delta) {
            if (counts.length <= vInfo.valueInfoId) {
                int oldLength = counts.length;
                counts = Arrays.copyOf(counts, allValueInfos.size());
                if (0 != initialValue) {
                    Arrays.fill(counts, oldLength, counts.length, initialValue);
                }
            }
            int newCount = (counts[vInfo.valueInfoId] += delta);
            if (1 == newCount) {
                ++size;
            }
            return newCount;
        }
        int increase(ValueInformation vInfo) { return add(vInfo,  1); }
        int decrease(ValueInformation vInfo) { return add(vInfo, -1); }

        int get(ValueInformation vInfo) {
            if (counts.length <= vInfo.valueInfoId) {
                return initialValue;
            } else {
                return counts[vInfo.valueInfoId];
            }
        }

        @Override
        public boolean add(ValueInformation vInfo) {
            return 1 == add(vInfo, 1);
        }
        private boolean removeAt(int infoId) {
            assert 0 == initialValue
                    : "Removal only supported when count starts at 0!";
            if (counts.length <= infoId || 0 == counts[infoId])  {
                return false;
            }
            counts[infoId] = 0;
            --size;
            return true;
        }
        @Override
        public boolean remove(Object vInfo) {
            return removeAt(((ValueInformation) vInfo).valueInfoId);
        }
        @Override
        public int size() {
            assert 0 == initialValue
                    : "Size is only supported when count starts at 0,"
                    + " i.e. when there are no spooky defaults.";
            return size;
        }
    }

    private final class ValueInformation {
        private final Lazynition parameterDefinition;
        private final int itemValueIndex;
        private final ValueStats stats;

        private final int valueInfoId;

        @SuppressWarnings("LeakingThisInConstructor")
        ValueInformation(Lazynition item, int itemValueIndex) {
            this.parameterDefinition = item;
            this.itemValueIndex = itemValueIndex;
            this.stats = new ValueStats(
                    item.combinePairwise,
                    /* Seed lookback with value-range on primary value if combined: */
                    item.combinePairwise && 0 == itemValueIndex ? item.valueRange : 0);
            this.valueInfoId = allValueInfos.size();
            allValueInfos.add(this);
        }

        private synchronized void registerPick() {
            if (1 == ++stats.totalCount && false == stats.combined) {
                for (ValueInformation upstreamEnablr : stats.pendingCombos.remove(null)) {
                    --upstreamEnablr.stats.enablerCount;
                }
            }
            for (ValueInformation combo2register : pickCrumbValues) {
                if (1 == stats.satisfiedComboCounts.increase(combo2register)
                        && false == combo2register.stats.combined) {
                    /* When not combined a default count of 1 is enforced
                     * when not yet increased. - Therefore an increase to 1 must
                     * be increased again to manifest an actual increase: */
                    stats.satisfiedComboCounts.increase(combo2register);
                }
                Set<ValueInformation> enablers =
                        stats.pendingCombos.remove(combo2register);
                if (null != enablers) {
                    --combo2register.stats.forwardRequestCount;
                    stats.comboEnablerCounts.add(combo2register, Integer.MAX_VALUE);
                    for (ValueInformation eachRetiredEnabler : enablers) {
                        stats.comboEnablerCounts.decrease(eachRetiredEnabler);
                        --eachRetiredEnabler.stats.enablerCount;
                    }
                }
            }
            crumbsLog += (char)(this.itemValueIndex + '0');
            pickCrumbValues.add(this);
        }

        private void registerEndOfLine() {
            crumbsLog = crumbsLog.substring(0, crumbsLog.length() - 1);
            stats.endOfLineKeys.add(crumbsLog);
            if (crumbsLog.length() <= 1) {
                return;
            }
            if (pickCrumbValues.size() <= 1) {
                return;
            }
            for (ValueInformation peerInfo : coreStatKeys.get(parameterDefinition)) {
                if (this == peerInfo) {
                    continue;
                }

                ValueStats peerStats = peerInfo.stats;
                if (peerStats.totalCount <= 0) {
                    /* There is an unused value at this end-of-line: */ 
                    return;

                } else if (false == peerStats.combined && peerStats.enablerCount <= 0) {
                    /* Already used uncombined value that is not enabler should
                     * not prevent deeper end-of-line registation. */
                    continue;

                } else if (false == peerStats.endOfLineKeys.contains(crumbsLog)) {
                    /* There is still a valid value at this end-of-line! */
                    return;
                }
            }
            /* Also register end-of-line at end of key, as there are no more
             * unused parameter-value combinations available:*/
            registerEndOfLineAtLastCrumbValue();
        }
    }

    /**
     * LAZY parameter defiNITION
     */
    private final static class Lazynition {
        private final Object paramId;
        private final boolean combinePairwise;
        private final int valueRange;

        Lazynition(Object paramId, boolean combinePairwise, char valueRange) {
            this.paramId = paramId;
            this.combinePairwise = combinePairwise;
            this.valueRange = valueRange;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 11 * hash + (this.paramId != null ? this.paramId.hashCode() : 0);
            hash = 11 * hash + (combinePairwise ? 0 : 37);
            hash = 11 * hash + this.valueRange;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Lazynition other = (Lazynition) obj;
            if (this.valueRange != other.valueRange
                    || this.combinePairwise != other.combinePairwise) {
                return false;
            }
            return this.paramId == other.paramId
                    || this.paramId != null && this.paramId.equals(other.paramId);
        }
    }

    /**
     * Stores the stack-trace of a parameter's initial introduction
     * to its {@link Lazer} instance. This information is kept internally to
     * compose decent error message in case a historical crumbs inconsistency is
     * detected by {@link Lazer#enforceHistoricalCrumbsConsistency(Lazynition)}
     * on a future repetition.
     *
     * @see Lazer#enforceHistoricalCrumbsConsistency(Lazynition)
     * @see Lazer#pendingCombinations()
     */
    public static class ExpectedParameterRepetition extends Throwable {
        private static final List<String> frameworkStackClasses = Arrays.asList(
                "java.",
                Lazer.class.getName(),
                LazyParams.class.getName(),
                ScopedLazyParameter.class.getName(),
                ToList.class.getName(),
                FalseOrTrue.class.getName(),
                FullyCombined.class.getName(),
                CartesianProductHub.class.getName(),
                Ensembles.class.getName(),
                InAnyOrder.class.getName(),
                Qronicly.class.getName(),
                ToPick.class.getName(),
                "jdk.", "javax.", "com.sun.", "sun.");

        ExpectedParameterRepetition(String message) {
            super(message);
        }

        /** Try to navigate stack in order to locate a place where things
         * might have started to go wrong. */
        @Override public String getMessage() {
            StringBuilder buildMsg =
                    new StringBuilder("Inconsistent parameter value pick\n");
            STACK_ELEMENTS:
            for (StackTraceElement stkElm : getStackTrace()) {
                if (0 < stkElm.getLineNumber()) {
                    String className = stkElm.getClassName();
                    if (null == className) {
                        continue;
                    }
                    for (String fwkClassName : frameworkStackClasses) {
                        if (className.startsWith(fwkClassName)) {
                            continue STACK_ELEMENTS;
                        }
                    }
                    if (null == stkElm.getMethodName()
                            || stkElm.getMethodName().startsWith("pickVal")) {
                        continue;
                    }
                    return buildMsg.append("... probably because a parameter that was introduced at ...\n")
                            .append(stkElm)
                            .append("\n... was not recognized when test was repeated!")
                            .toString();
                }
            }
            return buildMsg.append(super.getMessage()).toString();
        }
    }
}
