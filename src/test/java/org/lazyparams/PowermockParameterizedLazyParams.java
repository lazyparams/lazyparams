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

import java.util.Arrays;
import java.util.List;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.lazyparams.internal.PowerMockRunnerLight;
import org.lazyparams.showcase.FullyCombined;
import org.powermock.api.easymock.PowerMock;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Henrik Kaipe
 */
@RunWith(PowerMockRunnerLight.class)
@PowerMockRunnerDelegate(Parameterized.class)
public class PowermockParameterizedLazyParams {

    @Parameterized.Parameter
    public Integer[] timeOptions;

    @Parameterized.Parameters
    public static List<?> timeOptions() {
        return Arrays.asList(new Integer[][][] {
            {{12,56,84}},
            {{991011, 9872050}}
        });
    }

    @Test public void test() {
        LazyParams.currentScopeConfiguration().setMaxFailureCount(7);

        PowerMock.mockStatic(System.class);
        System.currentTimeMillis();
        EasyMock.expectLastCall().andAnswer(new IAnswer<Long>() {
            @Override
            public Long answer() throws Throwable {
                return LazyParams.pickValue("stubbedTime", timeOptions).longValue();
            }
        });
        PowerMock.replay(System.class);

        long time = System.currentTimeMillis();
        LazyParams.pickValue("timeIndex for " + time,
                Arrays.asList(timeOptions).indexOf((int)time),
                "ignored");
        Ramsa ramsa2verify = FullyCombined.pickFullyCombined();
        assertThat(ramsa2verify.ordinal())
                .as("Ramsa-to-verify ordinal")
                .isGreaterThan((int)(time % 3));
        LazyParams.<Ramsa>pickValue();
    }

    enum Ramsa {OLE,DOLE,DOFF;}
}
