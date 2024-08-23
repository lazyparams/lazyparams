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

import java.util.function.Consumer;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * It's mandatory to have this first on
 * {@link CompositeExtensions#extensions} or there will be risk for uninstall
 * after first parameters are already introduced.
 * <br/>
 * Test execution that tries different [un]install scenarios has been
 * disabled as default, because it takes too long to run (minutes). The tests
 * that evaluate these [un]install scenarios have been very useful during the
 * development of this framework but they are ill-suited for unit-test suites
 * that must run fast.
 * <br/>
 * As project has matured there is now faith in the framework stability during
 * different install scenarios and there are also some specific tests (e.g.
 * {@link org.lazyparams.internal.UninstallTest UninstallTest}, {@link FactoringTest},
 * {@link org.lazyparams.internal.UninstallMultiscopedTest UninstallMultiscopedTest})
 * that can hopefully cover as regression tests for critical [un]install scenarios.
 * But it's still desirable to once-in-a-while have these install scenarios thoroughly
 * reevaluated, as can be done by executing test-class {@link LeafParameterizedJupiterTest}
 * with {@link #SYSTEM_PROPERTY_TO_ENABLE_ALL_INSTALL_SCENARIOS
 * system-property lazyparams.test.installscenarios=true}. (Maven profile
 * "enableInstallScenarios" can be used to set this system property.)
 *
 * @author Henrik Kaipe
 * @see LeafParameterizedJupiterTest#LeafParameterizedJupiterTest(InstallScenario,StaticScopeParam,Object,Object,Object,Object,StaticScopeParam,MaxCountsTweak)
 * @see LeafParameterizedJupiterTest#tweaks()
 */
public interface InstallScenario extends BeforeAllCallback, Consumer<VerifyJupiterRule> {

    String SYSTEM_PROPERTY_TO_ENABLE_ALL_INSTALL_SCENARIOS = "lazyparams.test.installscenarios";

    /** Easy access on most common implementation */
    public Impl ALREADY_INSTALLED = Impl.ALREADY_INSTALLED;

    /**
     * Will return all install scenario extensions if system-property
     * {@link #SYSTEM_PROPERTY_TO_ENABLE_ALL_INSTALL_SCENARIOS lazyparams.test.installscenarios}
     * is true, as will be the case if Maven profile "enableInstallScenarios" is used.
     * Otherwise only {@link #ALREADY_INSTALLED} will be returned.
     */
    public static InstallScenario[] values() {

        if (Boolean.getBoolean("lazyparams.test.installscenarios")) {
            /* Evaluate all install scenarios: */
            return Impl.values();

        } else {
            /*
             * Only ALREADY_INSTALLED will be provided!
             * ... and make sure it is compatible with the reality here: */
            LazyParams.install();
            return new InstallScenario[] {ALREADY_INSTALLED};
        }
    }

    enum Impl implements InstallScenario {
        STARTUP_REINSTALL {
            @Override
            public void accept(VerifyJupiterRule t) {
                LazyParams.uninstall();
                LazyParams.install();
            }
            @Override public void beforeAll(ExtensionContext context) {}
        },
        IMPLICIT_REINSTALL {
            @Override
            public void accept(VerifyJupiterRule t) {
                LazyParams.uninstall();
            }
            @Override public void beforeAll(ExtensionContext context) {}
        },
        BEFORE_ALL_REINSTALL {
            final ThreadLocal<Object> pendingReinstall = new ThreadLocal<>();

            @Override
            public void accept(VerifyJupiterRule t) {
                LazyParams.uninstall();
                pendingReinstall.set("");
            }
            @Override
            public void beforeAll(ExtensionContext context) {
                if (null != pendingReinstall.get()) {
                    pendingReinstall.remove();
                    LazyParams.install();
                }
            }
        },
        ALREADY_INSTALLED {
            @Override public void accept(VerifyJupiterRule t) {}
            @Override public void beforeAll(ExtensionContext context) {}
        };
    }
}
