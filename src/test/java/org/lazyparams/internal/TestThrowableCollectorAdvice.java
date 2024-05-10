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

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.platform.engine.support.hierarchical.ThrowableCollector;
import org.lazyparams.showcase.Timing;
import org.xml.sax.SAXException;

import static org.junit.Assert.assertThrows;
import org.junit.platform.engine.TestExecutionResult;

/**
 * @author Henrik Kaipe
 */
public class TestThrowableCollectorAdvice {

    ThrowableCollector collector2test = new ThrowableCollector(t -> false);

    @Before
    public void forceAdviceByInitiatingFramework() {
        Timing.displayFromNow();
    }

    @Test
    public void test() {
        collector2test.assertEmpty();//Nothing thrown before exception collected!

        collector2test.execute(() -> { throw new IOException("Stubbed exception"); });

        assertThrows("Collected exception must be thrown",
                IOException.class, collector2test::assertEmpty);
        assertThrows("Exception repeated if repeated",
                IOException.class, collector2test::assertEmpty);

        Exception randomException = new IOException("Other exception");
        ProvideJunitPlatformHierarchical.ThrowableCollectorAdvice.retireResultThrowable(
                TestExecutionResult.failed(randomException));
        assertThrows("Retirement of an unrelated exception changes nothing",
                IOException.class, collector2test::assertEmpty);
        
        /* But retire the collected throwable ... */
        ProvideJunitPlatformHierarchical.ThrowableCollectorAdvice.retireResultThrowable(
                TestExecutionResult.failed(collector2test.getThrowable()));
        /* ... and now the collector should be happy again: */
        collector2test.assertEmpty();

        collector2test.execute(() -> { throw new SAXException(); });
        assertThrows("Ability to throw new collected exception must remain!",
                SAXException.class, collector2test::assertEmpty);
    }
}
