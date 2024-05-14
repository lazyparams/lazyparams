/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.config;

/**
 * @author Henrik Kaipe
 */
public interface ReadableConfiguration {

    static final ReadableConfiguration GLOBAL_DEFAULTS = new ReadableConfiguration() {

        @Override public int getMaxFailureCount() { return 5; }
        @Override public int getMaxTotalCount() { return 100; }
        @Override public String getValueDisplaySeparator() {return " "; }
        @Override public boolean alsoUseValueDisplaySeparatorBeforeToDisplayFunction() {
            return true;
        }
    };

    int getMaxFailureCount();
    int getMaxTotalCount();
    String getValueDisplaySeparator();
    boolean alsoUseValueDisplaySeparatorBeforeToDisplayFunction();
}
