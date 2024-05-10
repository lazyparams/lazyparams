/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.lazyparams.config.Configuration;
import org.lazyparams.core.Lazer;
import org.lazyparams.internal.DisplayAppendixContext;
import org.lazyparams.internal.LazerContext;
import org.lazyparams.internal.Instrument;

/**
 * This low-level API targets the internal parts of LazyParams.
 *
 * The feature API functions of {@link LazyParams class LazyParams} (i.e. the
 * pickValue(...) functions) are implemented upon
 * {@link org.lazyparams.showcase.ScopedLazyParameter},
 * which uses this low-level API.
 *
 * Worth noticing from this API is how LazyParams internally only deals with
 * int parameters, which possible values range from 0 to a parameter-specific
 * upper limit (exclusive). Also there is a clean distinction between choice of
 * parameter values and appendix to test display-name. It is for feature
 * implementations (e.g. {@link org.lazyparams.showcase.ScopedLazyParameter})
 * to decide how the internally picked int values are translated to their proper
 * test parameter values and how to have it displayed in the test-name appendix.
 * 
 * @author Henrik Kaipe
 */
public class LazyParamsCoreUtil {
    private LazyParamsCoreUtil() {}

    static final String INCONSISTENCY_DETECTED_APPENDIX = " PARAMETER_INCONSISTENCY_DETECTED";

    private static final boolean onWrongClassLoader = null != OnProperClassLoader.makePick.target;

    public static int makePick(
            Object parameterId, boolean combinePairwise, int numberOfParamValues) {
        if (onWrongClassLoader) {
            return (Integer) OnProperClassLoader.makePick.<RuntimeException>invoke(
                    parameterId, combinePairwise, numberOfParamValues);
        }
        if (null == parameterId) {
            throw new NullPointerException("Non-null parameter ID required!");
        } else if (numberOfParamValues <= 0) {
            throw new IllegalArgumentException("Parameter must have at least one possible value!");
        }
        try {
            return LazerContext.resolveLazer().pick(
                    parameterId, combinePairwise, numberOfParamValues);

        } catch (VirtualMachineError oom) {
            throw oom;
        } catch (Throwable ex) {
            if (Lazer.ExpectedParameterRepetition.class.isInstance(ex)) {
                /* It's debatable whether it's a good idea to handle this here
                 * but nevertheless it's a common gate that can help having
                 * relevant situations reported in an understandable manner: */
                Object reportId = new Object();
                displayOnFailure(reportId, INCONSISTENCY_DETECTED_APPENDIX);
                displayOnSuccess(reportId, INCONSISTENCY_DETECTED_APPENDIX);
            }
            throw LazyParamsCoreUtil.<RuntimeException>unchecked(ex);
        }
    }

    private static <E extends Throwable> E unchecked(Throwable ex) throws E {
        throw (E) ex;
    }

    public static CharSequence displayOnSuccess(
            Object displayPartRef, CharSequence displayAppendix) {
        if (onWrongClassLoader) {
            return (CharSequence) OnProperClassLoader.displayOnSuccess
                    .<RuntimeException>invoke(displayPartRef, displayAppendix);
        }
        return DisplayAppendixContext.display(
                displayPartRef, displayAppendix, true);
    }

    public static CharSequence displayOnFailure(
            Object displayPartRef, CharSequence displayAppendix) {
        if (onWrongClassLoader) {
            return (CharSequence) OnProperClassLoader.displayOnFailure
                    .<RuntimeException>invoke(displayPartRef, displayAppendix);
        }
        return DisplayAppendixContext.display(
                displayPartRef, displayAppendix, false);
    }

    public static Configuration globalConfiguration() {
        return Configuration.GLOBAL_CONFIGURATION;
    }

    enum OnProperClassLoader {
        makePick(Object.class, boolean.class, int.class),
        displayOnSuccess(Object.class, CharSequence.class),
        displayOnFailure(Object.class, CharSequence.class);

        private final Method target;

        OnProperClassLoader(Class... methodParams) {
            target = Instrument.resolveOnProvidingClassLoader(
                    LazyParamsCoreUtil.class, name(), methodParams);
        }

        <E extends Throwable> Object invoke(Object... arguments) throws E {
            try {
                return target.invoke(null, arguments);
            } catch (InvocationTargetException ex) {
                throw (E) ex.getTargetException();
            } catch (Exception ex) {
                throw (E) ex;
            }
        }
    }
}
