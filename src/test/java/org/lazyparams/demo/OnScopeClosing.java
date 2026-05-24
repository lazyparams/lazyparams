/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.demo;

import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.lazyparams.LazyParams;
import org.lazyparams.config.Configuration;

/**
 * @author Henrik Kaipe
 */
public class OnScopeClosing {

    private static final Lock configLockKey = new ReentrantLock() {
        @Override public String toString() { return OnScopeClosing.class.getSimpleName(); }
    };
    private static final Configuration.ScopeRetirementPlan<OnScopeClosing>
            retirementPlan = new Configuration.ScopeRetirementPlan<OnScopeClosing>() {

        @Override public void apply(OnScopeClosing closing) throws Exception {
            closing.targetScopeConfig = null;
            Exception firstException = null;
            AutoCloseable lifoResourceToClose;
            while (null != (lifoResourceToClose = closing.stack.pollLast())) {
                try {
                    lifoResourceToClose.close();
                } catch (Exception x) {
                    if (null == firstException) {
                        firstException = x;
                    } else {
                        firstException.addSuppressed(x);
                    }
                }
            }
            if (null != firstException) {
                throw firstException;
            }
        }
    };

    private Configuration targetScopeConfig;
    private Deque<AutoCloseable> stack = new LinkedBlockingDeque<AutoCloseable>();

    private OnScopeClosing(Configuration targetScopeConfig) {
        this.targetScopeConfig = targetScopeConfig;
    }

    private static OnScopeClosing resolve() {
        Configuration targetConfig = LazyParams.currentScopeConfiguration();
        configLockKey.lock();
        try {
            OnScopeClosing resolvedCloser =
                    targetConfig.getScopedCustomItem(configLockKey);
            if (null == resolvedCloser
                    || false == targetConfig.equals(resolvedCloser.targetScopeConfig)) {
                targetConfig.setScopedCustomItem(configLockKey,
                        resolvedCloser = new OnScopeClosing(targetConfig),
                        retirementPlan);
            }
            return resolvedCloser;
        } finally {
            configLockKey.unlock();
        }
    }

    public static <T extends AutoCloseable> T attach(T resourceToClose) {
        if (null == resourceToClose) {
            throw new NullPointerException("Registered resource must not be null!");
        }
        OnScopeClosing targetClosing = resolve();
        if (null == targetClosing.targetScopeConfig) {
            throw new IllegalStateException("Configuration on scope already closed!");
        }
        targetClosing.stack.addLast(resourceToClose);
        return resourceToClose;
    }
}
