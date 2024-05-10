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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.locks.ReentrantLock;

import org.lazyparams.config.Configuration;
import org.lazyparams.config.ReadableConfiguration;

import static org.lazyparams.config.Configuration.GLOBAL_CONFIGURATION;

/**
 * @author Henrik Kaipe
 */
public class ConfigurationContext {
    private ConfigurationContext() {}

    private static final Method currentTestConfOnTargetClassLoader = Instrument
            .resolveOnProvidingClassLoader(ConfigurationContext.class, "currentTestConfiguration");

    private static final WeakIdentityHashMap<Object,ConfigurationImpl>
            liveConfigurations = new WeakIdentityHashMap<Object, ConfigurationImpl>();
    private static final ReentrantLock scopeCreationLock = new ReentrantLock();

    /**
     * Could have been final but is instead set by {@link #resetAllCurrentConfig()},
     * in order to allow reset of configuration on all threads. This is
     * necessary in order to support implicit reinstallation of LazyParams
     * after {@link org.lazyparams.LazyParams#uninstall()}!
     *
     * @see #resetAllCurrentConfig()
     * @see Instrumentor#uninstall()
     */
    private static ThreadLocal<ConfigurationImpl> currentConfiguration;
    static {
        resetAllCurrentConfig();
    }
            
    @SuppressWarnings("NonPublicExported")
    public static Configuration currentTestConfiguration() {
        if (null != currentTestConfOnTargetClassLoader) {
            return ConfigurationContext
                    .<RuntimeException>currentConfigFromTargetClassLoader();
        }
        ConfigurationImpl currentConf = currentConfiguration.get();
        while (currentConf.isRetired()) {
            if (GLOBAL_CONFIGURATION == currentConf.parentConfiguration()) {
                currentConfiguration.remove();
                return currentConfiguration.get();
            } else {
                currentConf = (ConfigurationImpl) currentConf.parentConfiguration();
                currentConfiguration.set(currentConf);
            }
        }
        return currentConf;
    }

    private static <E extends Throwable> Configuration currentConfigFromTargetClassLoader()
    throws E {
        try {
            return new ConfigurationOnOtherClassloader(
                    currentTestConfOnTargetClassLoader.invoke(null));
        } catch (InvocationTargetException ex) {
            throw (E) ex.getTargetException();
        } catch (Exception ex) {
            throw (E) ex;
        }
    }

    /**
     * Returns true if this is a new scope.
     * Otherwise false will inform this scope is already open.
     * Used by {@link ContextLifecycleProviderFacade#openExecutionScope(Object)}
     */
    static boolean openScope(Object scopeRef) {
        ConfigurationImpl scopeConfiguration = null;
        scopeCreationLock.lock();
        try {
            scopeConfiguration = liveConfigurations.get(scopeRef);
            if (null == scopeConfiguration || scopeConfiguration.isRetired()) {
                scopeConfiguration = new ConfigurationImpl(currentTestConfiguration());
                liveConfigurations.put(scopeRef, scopeConfiguration);
                return true;
            } else {
                /* It seems scope already opened with configuration.
                 * Probably nothing to worry about ... */
                return false;
            }
        } finally {
            scopeCreationLock.unlock();
            currentConfiguration.set(scopeConfiguration);
        }
    }

    /**
     * Used by {@link ContextLifecycleProviderFacade#closeExecutionScope(Object,Throwable)}
     */
    static ReadableConfiguration retireScope(Object scopeRef) {
        ConfigurationImpl config2retire = liveConfigurations.remove(scopeRef);
        if (null == config2retire) {
            /* Missing configuration hints this scope was never opened
             * - probably because test scope started before LazyParams installed
             * or repeated retirements of this scope, which is fine.*/
            config2retire = currentConfiguration.get();
            if (config2retire.isRetired()
                    && false == config2retire.isRetiredBy(scopeRef)) {
                ReadableConfiguration parentConfig =
                        config2retire.parentConfiguration();
                if (false == parentConfig instanceof ConfigurationImpl) {
                    currentConfiguration.remove();
                    return parentConfig;
                }
                config2retire = (ConfigurationImpl) parentConfig;
                currentConfiguration.set(config2retire);
            }
        } else {
            /* Ensure support for repeated retirements: */
            currentConfiguration.set(config2retire);
        }
        config2retire.retire(scopeRef);
        return config2retire;
    }

    static void resetAllCurrentConfig() {
        if (null != currentTestConfOnTargetClassLoader) {
            /* Don't do this from the wrong classloader!*/
            return;
        }

        if (null != currentConfiguration) {
            /*
             * This seems to be a midair uninstallation.
             * Make sure the current configuration is properly retired:
             */
            final Object retirer = new Object() {
                @Override public String toString() { return "Configuration Reset"; }
            };
            ReadableConfiguration currentConfig = currentConfiguration.get();
            while (currentConfig instanceof ConfigurationImpl) {
                ConfigurationImpl config2retire = (ConfigurationImpl) currentConfig;
                liveConfigurations.removeValue(config2retire);
                config2retire.retire(retirer);
                currentConfig = config2retire.parentConfiguration();
            }
            currentConfiguration.remove();
        }

        currentConfiguration = new InheritableThreadLocal<ConfigurationImpl>() {
            @Override
            protected ConfigurationImpl initialValue() {
                /* A bit ugly - but this is coincidently a good place to have
                 * LazyParams installed, because this will execute on first usage
                 * of any LazyParams feature that require provider installation!
                 * Also it allows for framework to be reinstalled if it somehow
                 * has been momentarily uninstalled by some other activity. */
                Instrument.install();
                return new ConfigurationImpl(GLOBAL_CONFIGURATION);
            }
        };
    }
}
