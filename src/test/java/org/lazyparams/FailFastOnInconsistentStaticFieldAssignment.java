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

import org.junit.jupiter.api.Test;
import org.lazyparams.showcase.FalseOrTrue;

/**
 * @author Henrik Kaipe
 */
public class FailFastOnInconsistentStaticFieldAssignment {

    final static int nonRepeatableStaticParameter = LazyParams.pickValue("nonrepeatable_static_field", 1,2);

    private boolean pendingNotRepeatable = true;

    @Test void notParameterized() {}

    @Test void oneParameter() { LazyParams.pickValue("method-scope_int", 2,3); }

    @Test void pickNonRepeatable() {
        if (pendingNotRepeatable) {
            pendingNotRepeatable = false;
            LazyParams.pickValue("not_repeatable", 99,98);
        }
    }

    @Test void earlyNonRepeatablePick() {
        if (FalseOrTrue.pickBoolean("try_nonrepeatable")) {
            pickNonRepeatable();
        }
        oneParameter();
    }
}
