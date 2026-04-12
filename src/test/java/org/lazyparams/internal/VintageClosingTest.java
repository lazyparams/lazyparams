/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.internal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.Suite;
import org.lazyparams.LazyParams;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import static org.lazyparams.internal.VintageClosingResource.*;

/**
 * @author Henrik Kaipe
 */
@PowerMockIgnore("org.lazyparams.internal.VintageClosingResource")
@PowerMockRunnerDelegate
@RunWith(Suite.class)
@Suite.SuiteClasses({
    VintageClosingTest.Vanilla1.class,
    VintageClosingTest.Vanilla2.class,
    VintageClosingTest.Powermocked1.class,
    VintageClosingTest.Powermocked2.class
})
public class VintageClosingTest {

    static VintageClosingResource[] stagedForClosing;

    VintageClosingResource[] resources2openAndClose =
            getClass().getAnnotation(Resources.class).value();

    @Before
    public void assertAllOtherResourcesAreClosed() {
        VintageClosingResource.assertAllClosedExcept(resources2openAndClose);
    }

    @Test
    public void pickAndOpen() {
        LazyParams.pickValue("opens", resources2openAndClose).open();
    }

    @After
    public void stageForClosing() {
        stagedForClosing = resources2openAndClose;
    }

    @AfterClass
    public static void registerClosing() {
        Stream.of(stagedForClosing).forEach(opened -> LazyParams.currentScopeConfiguration()
                .setScopedCustomItem(new Object(), opened, AutoCloseable::close));

                /* Same as above but without the retirement plan AutoCloseable::close ...*/
//                .setScopedCustomItem(new Object(), opened));
                /* ... would fail the test during #assertAllOtherResourcesAreClosed() ! */
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface Resources { VintageClosingResource[] value(); }

    @RunWith(JUnit4.class)
    @Resources({vanilla1,vanilla2,vanilla3})
    public static class Vanilla1 extends VintageClosingTest {}

    @RunWith(JUnit4.class)
    @Resources({vanilla4,vanilla5,vanilla6})
    public static class Vanilla2 extends VintageClosingTest {}

    @RunWith(PowerMockRunnerLight.class)
    @Resources({powermocked1,powermocked2,powermocked3})
    public static class Powermocked1 extends VintageClosingTest {}

    @RunWith(PowerMockRunnerLight.class)
    @Resources({powermocked4,powermocked5,powermocked6})
    public static class Powermocked2 extends VintageClosingTest {}
}
