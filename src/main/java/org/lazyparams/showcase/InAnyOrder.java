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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import org.lazyparams.LazyParams;
import org.lazyparams.LazyParamsCoreUtil;
import org.lazyparams.config.Configuration;

/**
 * @author Henrik Kaipe
 */
public class InAnyOrder {
    private InAnyOrder() {}

    private static final Object paramCountConfigKey = new Object() {
        @Override public String toString() { return "InAnyOrder-Param-Count"; }
    };

    public static void runVerbosly(Callable<? extends CharSequence>... tasks) {
        InAnyOrder.<RuntimeException>run(AppendableFeed.VERBOSE, tasks);
    }

    public static void runQronicly(Callable<? extends CharSequence>... tasks) {
        InAnyOrder.<RuntimeException>run(AppendableFeed.QRONIC, tasks);
    }

    /**
     * It might seem strange to support this knowing that if a problem is
     * revealed from the tasks' order then we don't know which
     * order we were dealing with. But this can be justified if the individual
     * tasks introduce parameters in a way that allows us to determine the
     * tasks' order by checking the order of their parameters.
     */
    public static void runQuietly(Runnable... tasks) {
        Callable<String>[] callableTasks = new Callable[tasks.length];
        for (int i = 0; i < tasks.length; ++i) {
            final Runnable eachRunnableTask = tasks[i];
            callableTasks[i] = new Callable<String>() {
                @Override public String call()  {
                    eachRunnableTask.run();
                    return "";
                }
            };
        }
        InAnyOrder.<RuntimeException>run(AppendableFeed.QUIET, callableTasks);
    }

    private static Object resolveExplicitParamId() {
        Configuration config = LazyParams.currentScopeConfiguration();
        Integer inAnyOrderParamsCount = config.getScopedCustomItem(paramCountConfigKey);
        if (null == inAnyOrderParamsCount) {
            inAnyOrderParamsCount = 0;
        }
        config.setScopedCustomItem(paramCountConfigKey, ++inAnyOrderParamsCount);
        return new ToStringKey(paramCountConfigKey.toString(), inAnyOrderParamsCount) {};
    }

    private static <E extends Exception> void run(
            AppendableFeed appendFeed, Callable<? extends CharSequence>[] tasks)
    throws E {
        if (0 == tasks.length) {
            return;
        }
        List<Callable<? extends CharSequence>> reorderedTasks = ScopedLazyParameter
                .from(tasks).quietly()
                /*Prioritize distribution - by using ...*/
                .notCombined()/*- assuming order is unlikely to affect result!*/
                .withExplicitParameterId(resolveExplicitParamId())
                .asParameter("", ToList.<Callable<? extends CharSequence>>combinePermutation())
                .pickValue();
        final String separator = LazyParams
                .currentScopeConfiguration().getValueDisplaySeparator();
        Appendable pendingTaskDescription = appendFeed.nextAppendable();
        for (Callable<? extends CharSequence> task : reorderedTasks) {
            try {
                pendingTaskDescription
                        .append(separator).append(task.call()).append("[");
                pendingTaskDescription = appendFeed
                        .nextAppendable().append("]");
            } catch (Exception ex) {
                try {
                    pendingTaskDescription
                            .append(ex.getClass().getSimpleName()).append("[");
                    appendFeed.nextAppendable().append("]");
                } catch (IOException ignore) {}
                throw (E) ex;
            }
        }
    }

    private enum AppendableFeed {
        VERBOSE, QRONIC, QUIET;

        Appendable nextAppendable() {
            StringBuilder appendable = new StringBuilder();
            switch (this) {
                case VERBOSE:
                    LazyParamsCoreUtil.displayOnSuccess(appendable, appendable);
                case QRONIC:
                    LazyParamsCoreUtil.displayOnFailure(appendable, appendable);
                case QUIET:
                    return appendable;
                default:
                    throw new Error("Unsupported feed: " + this);
            }
        }
    }
}
