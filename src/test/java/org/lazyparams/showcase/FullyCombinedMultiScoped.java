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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.lazyparams.showcase.FullyCombined.pickFullyCombined;

/**
 * To verify proper behavior of fully combined parameters across scopes.
 *
 * @author Henrik Kaipe
 */
public class FullyCombinedMultiScoped {

    static final ScopedLazyParameter<Integer> explicitDefinitionPreservesPickValueInSubscope =
            ScopedLazyParameter.from(1,2).fullyCombinedGlobally().asParameter("b");

    @BeforeAll static void onAll() {
        pickFullyCombined(v -> "a="+v, 1,2);
        explicitDefinitionPreservesPickValueInSubscope.pickValue();
        pickFullyCombined(v -> "c="+v, new Integer[] {1,2});
    }

    @Test void test() {
        onAll();
    }
}
