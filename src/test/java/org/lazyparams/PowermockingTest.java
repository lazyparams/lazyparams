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

import org.junit.Rule;
import org.junit.Test;

/**
 * @author Henrik Kaipe
 */
public class PowermockingTest {

    @Rule
    public final VerifyVintageRule expect = new VerifyVintageRule(Powermocking.class);

    @Test
    public void unmocked() {
        expect
                .fail(" \\|-\\|max-fails=2\\|param=\\[FIRST\\]")
                        .withMessage(".*\\[FIRST\\].*SECOND.*")
                .pass(" _-_max-fails=4_param=\\[FIRST, SECOND\\]")
                .pass(" - max-fails=2 param=\\[SECOND, THIRD\\]")
                .fail(" _-_max-fails=4_param=\\[FORTH\\]")
                        .withMessage(".*\\[FORTH\\].*SECOND.*")
                .pass(" \\|-\\|max-fails=4\\|param=\\[SECOND\\]")
                .pass(" - max-fails=2 param=\\[FIRST, SECOND\\]")
                .fail(" _-_max-fails=2_param=\\[THIRD\\]")
                        .withMessage(".*\\[THIRD\\].*SECOND.*")
                .fail("").withMessage(".*max.*2.*");
    }

    @Test
    public void powermocked() {
        expect
                .pass(" \\|-\\|max-fails=2\\|param=\\[SECOND\\]")
                .fail(" _-_max-fails=4_param=\\[THIRD\\]")
                        .withMessage(".*\\[THIRD\\].*SECOND.*")
                .fail(" - max-fails=2 param=\\[FORTH\\]")
                        .withMessage(".*\\[FORTH\\].*SECOND.*")
                .fail("").withMessage(".*max.*2.*");
    }
}
