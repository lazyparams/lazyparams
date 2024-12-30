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

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.lazyparams.LazyParams;
import org.lazyparams.LazyParamsCoreUtil;
import org.lazyparams.config.Configuration;
import org.lazyparams.core.Lazer;
import org.lazyparams.internal.LazerContext;
import org.lazyparams.showcase.ScopedLazyParameter;

/**
 * Poor man utilities to feature parameters that are not combined.
 * I.e. a parameter that is introduced by {@link #pick(String,Object...)} will
 * just make sure all its values are picked at least once and thereafter not
 * force any additional repetitions for combining these values with other
 * parameters.
 * <br/>
 * It is still desirable to have parameter value combinations well distributed
 * because it makes feature {@link #forceRepeatUntilDesiredTotalCount(int)}
 * interesting, as it would allow developer to specify the number of repetitions
 * and trust the parameter value combinations will be well-distributed without
 * being very strict about satisfying any combinatorial conditions.
 * <br/>
 * For now the feature {@link #forceRepeatUntilDesiredTotalCount(int)} is just
 * specified here as tested demo feature, because there are many possible
 * solutions for achieving this and it's not clear which one is better. Here the
 * repetition count is handled by the feature function itself (using scoped
 * custom configuration) but a better idea is perhaps to have a repetition
 * counter available as a core feature that can be used for anything. (A reason
 * for not introducing repetition count as a core feature is the risk of core
 * failure "inconsistent parameter value pick" in case the repetition count is
 * used as a condition for parameter introduction and execution path.)
 *
 * @author Henrik Kaipe
 */
public class Uncombined {

    private static final Map<Lazer,AtomicInteger> repeatCounts =
            new WeakHashMap<Lazer,AtomicInteger>() {
        @Override public AtomicInteger get(Object key) {
            AtomicInteger count = super.get(key);
            if (null == count) {
                put((Lazer)key, count = new AtomicInteger(1));
            }
            return count;
        }
    };

    static void forceRepeatUntilDesiredTotalCount(int desiredTotalCount) {
        assert 1 <= desiredTotalCount : "Desired total count must be at least 1";
        Configuration config = LazyParams.currentScopeConfiguration();
        if (config.getMaxTotalCount() < desiredTotalCount) {
            config.setMaxTotalCount(desiredTotalCount);
        }

        LazyParamsCoreUtil.makePick(Key.TOTAL_COUNT, false, desiredTotalCount);
        AtomicInteger counter = repeatCounts.get(LazerContext.resolveLazer());
        String msg = config.getValueDisplaySeparator() + "#" + counter;
        LazyParamsCoreUtil.displayOnFailure(Key.TOTAL_COUNT, msg);
        LazyParamsCoreUtil.displayOnSuccess(Key.TOTAL_COUNT, msg);
        /*
         * Increase counter when repetition scope closes: */
        config.setScopedCustomItem(Key.TOTAL_COUNT, counter, Key.TOTAL_COUNT);
    }

    static <T> T pick(String name, T... values) {
        return ScopedLazyParameter.from(values)
                .notCombined()
                .withExtraIdDetails(Uncombined.class)
                .asParameter(name)
                .pickValue();
    }

    private enum Key implements Configuration.ScopeRetirementPlan<AtomicInteger> {
        TOTAL_COUNT;

        @Override
        public void apply(AtomicInteger countAtRetiredScope) {
            countAtRetiredScope.incrementAndGet();
        }
    }
}
