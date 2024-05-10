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

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lazyparams.internal.PowerMockRunnerLight;
import org.lazyparams.showcase.ScopedLazyParameter;
import org.lazyparams.showcase.ToList;
import org.lazyparams.showcase.ToPick;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;

/**
 * Small test scenario that is far from covering all PowerMock corner cases.
 * @author Henrik Kaipe
 */
@RunWith(PowerMockRunnerLight.class)
@PrepareForTest(ToList.class)
public class Powermocking {

    void runScenario() {
        LazyParams.currentScopeConfiguration().setValueDisplaySeparator(
                LazyParams.pickValue(s -> s.trim() + "-", "|", "_", " "));
        LazyParams.currentScopeConfiguration().setMaxFailureCount(
                LazyParams.pickValue("max-fails", 2,4));

        List<Param> paramValuePicks = Stream
                .of(Param.values())
                .collect(ToPick.from())
                .asParameter("param", ToList.combineOneOrTwo())
                .pickValue();
        Assertions.assertThat(paramValuePicks)
                .as("param-list")
                .contains(Param.SECOND);
    }

    @Test
    public void unmocked() {
        runScenario();
    }

    @Test
    public void powermocked() {
        PowerMock.mockStatic(ToList.class);
        ToList.combineOneOrTwo();
        PowerMock.expectLastCall().andReturn(new ToList<Object>() {
            @Override
            public List<Object> applyOn(
                    List<? extends Object> inputParameterValues,
                    ScopedLazyParameter.CombiningCollector.Seeds combinedSeeds) {
                ClassLoader[] loaders = {
                    combinedSeeds.getClass().getClassLoader(),
                    getClass().getClassLoader(),
                    LazyParams.currentScopeConfiguration().getClass().getClassLoader(),
                    Test.class.getClassLoader()
                };
                return Collections.singletonList(
                        Param.values()[1 + combinedSeeds.next(3)]);
            }
        });
        PowerMock.replayAll();

        /* Thereafter same scenario as unmocked(): */
        runScenario();
    }

    enum Param { FIRST, SECOND, THIRD, FORTH };
}
