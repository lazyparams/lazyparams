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

import java.util.EnumSet;
import java.util.stream.Stream;
import static org.junit.Assert.assertFalse;

/**
 * @author Henrik Kaipe
 */
public enum VintageClosingResource implements AutoCloseable {
    vanilla1, vanilla2, vanilla3,
    vanilla4, vanilla5, vanilla6,
    powermocked1, powermocked2, powermocked3,
    powermocked4, powermocked5, powermocked6;

    private boolean open = false;

    public static void assertAllClosedExcept(VintageClosingResource... resources) {
        EnumSet<VintageClosingResource> shouldBeClosed =
                EnumSet.allOf(VintageClosingResource.class);
        Stream.of(resources).forEach(shouldBeClosed::remove);
        shouldBeClosed.forEach(VintageClosingResource::assertClosed);
    }
    private void assertClosed() { assertFalse(this + " must not be open!", open); }

    @Override
    public void close() { open = false; }
    public void open() { open = true; }
}
