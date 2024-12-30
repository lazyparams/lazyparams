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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.lazyparams.LazyParams;
import org.lazyparams.LazyParamsCoreUtil;
import org.lazyparams.config.Configuration;

/**
 * A CartesianProductHub instance is used as argument to method
 * {@link ScopedLazyParameter.Combiner#fullyCombinedOn(CartesianProductHub)}
 * to make LazyParams seek out all parameter-value combinations for the
 * parameter-picks that are specified as fully combined on the specified
 * CartesianProductHub instance.
 * By default LazyParams will use a pairwise combination strategy that will make
 * sure each parameter-value is combined at least once with each value of all
 * other coevaluated parameters. In situations when more zealous evaluation of
 * certain parameter value combinations is necessary, then it can be a good idea
 * to have the values from the parameters of concern be fully combined on a
 * CartesianProductHub instance, which is specified by being argument to
 * {@link ScopedLazyParameter.Combiner#fullyCombinedOn(CartesianProductHub)}
 * when a value is picked from one of the parameters of concern.
 * <br>
 * Parameters that are fully combined on same
 * {@link CartesianProductHub} will be treated as a separate unit by
 * LazyParams when other, pairwise-combined values from other parameters are
 * picked. This means that with many parameters it is possible to have pockets
 * of parameters, which values are fully combined between themselves but
 * pairwise combined with values from other parameters.
 *
 * @see ScopedLazyParameter.Combiner#fullyCombinedOn(CartesianProductHub)
 * @author Henrik Kaipe
 */
public abstract class CartesianProductHub {
    /**
     * The globally available CartesianProductHub that is used by the
     * pickFullyCombined(...) methods of {@link FullyCombined}.
     * @see FullyCombined
     * @see ScopedLazyParameter.Combiner#fullyCombinedGlobally()
     */
    public static final CartesianProductHub GLOBAL = new CartesianProductHub() {};

    private final String hubToString;

    protected CartesianProductHub(String hubId) {
        this.hubToString = getClass().getName()
                + (null == hubId ? "" : ": " + hubId);
    }

    protected CartesianProductHub() {
        this(null);
    }

    /**
     * Makes a fully combined pick for specified parameter
     * from range 0 to nbrOfParamValues (exclusive).
     */
    int makeFullyCombinedIndexPick(Object paramCoreId, int nbrOfParamValues) {
        LinkedHashMap<Object,Integer> hubCrumbs = resolveHubCrumpsFor(this);

        List<Integer> crumbValues = new ArrayList<Integer>(hubCrumbs.size() + 1);
        for (Map.Entry<Object,Integer> crumbEntry : hubCrumbs.entrySet()) {
            if (crumbEntry.getKey().equals(paramCoreId)) {
                /* Already picked inside of current scope: */
                return pickIndexWithOffset(paramCoreId, crumbValues, nbrOfParamValues);

            } else {
                crumbValues.add(crumbEntry.getValue());
            }
        }
        try {
            return pickIndexWithOffset(paramCoreId, crumbValues, nbrOfParamValues);
        } finally {
            hubCrumbs.put(paramCoreId, crumbValues.get(crumbValues.size() - 1));
        }
    }

    /**
     * A bit complicated as indexes of what is already picked on this hub will
     * be used to offset the core pick for better distribution. Without somehow
     * handling this the core would initially just walk through possible values
     * of first parameter while just picking primary value from subsequent
     * parameters on this {@link CartesianProductHub} hub, until all values of
     * first parameter have been picked at least once. Thereafter it will start
     * to walk through alternative second parameter values while combining them
     * with first parameter values, continuing to just pick primary value of any
     * subsequent parameters until a first parameter-value has walked through
     * all its combinations with second parameter, and so on - therewith
     * requiring many repetitions before evaluating anything but primary value
     * for last parameter to have been combined on this hub.<br>
     * The poor distribution would not be much of a problem if all parameters
     * of the test were combined in this manner on a common hub. But the
     * intention here is to support a pocket, in which a subset of parameters
     * have their values fully combined, and then have each fully combined
     * record pairwise combined with other parameter values (or other fully
     * combined parameter value records from other pockets). Under such
     * circumstances it is desirable to have good distribution on values picked
     * from a fully combined pocket as it is more likely to have positive impact
     * on the pairwise combining with surrounding parameter values.
     */
    private int pickIndexWithOffset(
            Object paramCoreId, List<Integer> crumbValues, int nbrOfParamValues) {
        int offset = 0;
        for (Integer crumb : crumbValues) {
            offset += crumb;
        }
        Object finalizedParamId = new ToStringKey(
                "CartesianProductHub crumbs on top of core ID",
                crumbValues.toString(), paramCoreId) {};
        int preliminaryPick = LazyParamsCoreUtil.makePick(
                finalizedParamId, true, nbrOfParamValues);
        crumbValues.add(preliminaryPick);
        offset += preliminaryPick;
        return offset % nbrOfParamValues;
    }

    private static LinkedHashMap<Object,Integer> resolveHubCrumpsFor(
            CartesianProductHub hub) {
        Configuration scopedConfig = LazyParams.currentScopeConfiguration();
        Object hubConfigKey = new ToStringKey(hub.toString(), hub, scopedConfig) {};
        LinkedHashMap<Object,Integer> hubCrumbs =
                scopedConfig.getScopedCustomItem(hubConfigKey);
        if (null == hubCrumbs) {
            hubCrumbs = new LinkedHashMap<Object,Integer>();
            scopedConfig.setScopedCustomItem(hubConfigKey, hubCrumbs);
        }
        return hubCrumbs;
    }

    @Override
    public int hashCode() {
        return 67 * 7 + hubToString.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (false == getClass().getName().equals(obj.getClass().getName())) {
            return false;
        }
        String thisToString = toString();
        String objToString = obj.toString();
        return thisToString == objToString
                || null != thisToString && thisToString.equals(objToString);
    }

    @Override
    public String toString() {
        return hubToString;
    }
}
