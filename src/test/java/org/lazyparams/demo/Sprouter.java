/*
 * Copyright 2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.demo;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lazyparams.LazyParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Very basic test to verify compatibility with SpringExtension and Jupiter
 * Nested test class. It works but more tests would be desirable.
 *
 * @author Henrik Kaipe
 */
public class Sprouter {

    static class My23 {
        int get23() { return 23; }
    }

    @Bean
    public Object my23() {
        return new My23();
    }

    @Nested
    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = Sprouter.class)
    public class Sprinner {

        @Autowired My23 sprouter;

        @Test
        void threeTimes() {
            assertEquals(LazyParams.pickValue("expect", 32,23,101),
                    sprouter.get23(), "From My23");
        }
    }
}
