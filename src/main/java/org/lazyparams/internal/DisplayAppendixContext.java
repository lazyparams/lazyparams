/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.internal;

import org.lazyparams.config.Configuration;

/**
 * @author Henrik Kaipe
 */
public class DisplayAppendixContext {
    private DisplayAppendixContext() {}

    public static CharSequence display(
            Object displayPartRef, CharSequence displayAppendixPart, boolean success) {
        DualDisplayAppendix displayAppendix = resolve(true);
        displayAppendix.display(displayPartRef, displayAppendixPart, success);
        return displayAppendix;
    }

    static DualDisplayAppendix resolve(boolean force) {
        Configuration scopeConfig = ConfigurationContext.currentTestConfiguration();
        DualDisplayAppendix currentAppendix = scopeConfig
                .getScopedCustomItem(ConfigKey.DISPLAY_APPENDIX);
        if (null == currentAppendix && force) {
            scopeConfig.setScopedCustomItem(ConfigKey.DISPLAY_APPENDIX,
                    currentAppendix = new DualDisplayAppendix());
        }
        return currentAppendix;
    }

    static void coverParentScope(Configuration parentScopeConfig) {
        if (null != parentScopeConfig.getScopedCustomItem(ConfigKey.DISPLAY_APPENDIX)) {
            Configuration scopeConfig = ConfigurationContext.currentTestConfiguration();
            scopeConfig.setScopedCustomItem(
                    ConfigKey.DISPLAY_APPENDIX, new DualDisplayAppendix());
        }
    }

    private enum ConfigKey { DISPLAY_APPENDIX }
}
