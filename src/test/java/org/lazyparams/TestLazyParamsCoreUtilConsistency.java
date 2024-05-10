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

import java.lang.reflect.Method;
import org.junit.After;
import org.junit.Test;
import org.lazyparams.showcase.FullyCombined;

/**
 * Simple little test which secondary purpose is to be runnable on old JVMs.
 *
 * @author Henrik Kaipe
 */
public class TestLazyParamsCoreUtilConsistency {

    @Test
    public void match_OnProperClassLoader_constant_with_method() {
        String targetMethodName = FullyCombined
                .<LazyParamsCoreUtil.OnProperClassLoader>pickFullyCombined().name();
        Method[] methods = LazyParamsCoreUtil.class.getMethods();
        for (Method m : methods) {
            if (m.getName().equals(targetMethodName)) {
                /* Success: */
                return;
            }
        }
        throw new AssertionError(targetMethodName
                + " does not specify any method on " + LazyParamsCoreUtil.class);
    }

    @After
    public void displayJdkVersion() {
        LazyParams.pickValue(new ToDisplayFunction<String>() {
            @Override public CharSequence apply(String version) {
                return "on Java version " + version;
            }
        }, System.getProperty("java.version"));
    }
}
