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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.lazyparams.LazyParams;
import org.lazyparams.showcase.FalseOrTrue;
import org.lazyparams.showcase.Qronicly;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * POC to demonstrate how the abilities of LazyParams can be used.
 *
 * @author Henrik Kaipe
 */
public class DemoBusinessAnnotation {

    String visitOneOf(String... webPages) {
        return LazyParams.pickValue("visits "::concat, webPages);
    }

    @TestAs({User.ADMIN, User.EMPLOYEE, User.IMPOSTER})
    void onIntranet() {
        int shownWhenTestFails = Qronicly.pickValue("HIDDEN", 1,2);
        String activity = visitOneOf("business plans", "latest report");
        if (2 == shownWhenTestFails && activity.startsWith("business")) {
            throw new AssertionError(
                    "Failure will reveal HIDDEN parameter value");
        }
    }

    /**
     * This test-method demonstrates pairwise combining,
     * because it can introduce three or four parameters.
     * I.e. role, page and "then gives feedback:"(or not) plus
     * an extra forth parameter "THUMBS" that is introduced if
     * "then gives feedback:"!
     * Also notice how test is cut short at one occasion, without introducing
     * any other parameters, when executed "as_IMPOSTER".
     */
    @TestAs(/*no role specified = any role*/)
    void onPublicWeb() {
        visitOneOf("first-page", "news-page");
        if (FalseOrTrue.pickBoolean("then gives feedback:")) {
            LazyParams.pickValue("THUMBS", "UP", "DOWN");
        }
        visitOneOf("logout-page");
    }

    @TestAs(User.ADMIN)
    void onlyOneUserOptionDoesNotInitiateParametrizationOnItsOwn() {
        LazyParams.pickValue("and neither will this parameter",
                "because it only has one possible value!");
    }

    @TestAs(User.ADMIN)
    void butParameterWillShowIfThereIsOtherParameterWithMultipleValues() {
        Qronicly.pickValue("Qronic parameter that can make "
                + "two successful tests have the same name!", 1,2);
        LazyParams.pickValue(String::valueOf,
                "configures security", "generates reports");
    }

    /**
     * Seriously reusable annotation to replace @Test when the execution
     * requires login with certain role or roles.
     * (OK - no real login happens here.
     * - The purpose is to demonstrate how LazyParams can help to easily
     * implement a highly reusable extension with great potential)
     */
    @ExtendWith(TestAs.Extension.class)
    @Test
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface TestAs {
        User[] value() default {};

        class Extension
        implements BeforeEachCallback, AfterEachCallback {
            User pickUser(ExtensionContext ec) {
                TestAs annotationOnTest = ec.getRequiredTestMethod()
                        .getAnnotation(TestAs.class);
                return LazyParams.pickValue(annotationOnTest.value());
            }
            @Override
            public void beforeEach(ExtensionContext ec) {
                pickUser(ec).login();
            }
            @Override
            public void afterEach(ExtensionContext ec) {
                LazyParams.pickValue(
                        user -> " & finally " + user + " is logged out",
                        /* Notice how pickUser(...) keeps returning same value
                         * during this repetition scope! */
                        pickUser(ec).name());
            }
        }
    }

    public enum User {
        ADMIN, EMPLOYEE, CUSTOMER, IMPOSTER, GUEST;

        void login() {
            assertThat(this.name())
                    .as("User Role")
                    .isNotEqualTo(IMPOSTER.name());
        }

        @Override public String toString() {
            return "as_" + name();
        }
    }
}
