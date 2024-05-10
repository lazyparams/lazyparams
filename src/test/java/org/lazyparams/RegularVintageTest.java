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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.model.Statement;
import org.lazyparams.internal.PowermockDelegate;
import org.lazyparams.internal.PowermockRegular;

/**
 * @author Henrik Kaipe
 */
@RunWith(Parameterized.class)
public class RegularVintageTest {

    @Rule
    public final VerifyVintageRule expectRule;

    public RegularVintageTest(RegularVintageTarget targetExpectRule) {
        this.expectRule = targetExpectRule.asRule();
    }

    @Parameterized.Parameters(name="{0}")
    public static List<?> tweaks() {
        RegularVintageTarget[] targetRules = RegularVintageTarget.values();
        List<Object[]> tweaks = new ArrayList<Object[]>();
        for (RegularVintageTarget eachTargetRule : targetRules) {
            tweaks.add(new Object[] {
                eachTargetRule
            });
        }
        return tweaks;
    }

    @Test
    public void normal() {
        expectRule
                .pass("normal 1st=34 2nd=sdf")
                .pass("normal 1st=42 2nd=dfwe")
                .fail("normal 1st=34 2nd=dfwe")
                        .withMessage("Fail here")
                .pass("normal 1st=42 2nd=sdf")
                .fail("normal")
                        .withMessage("1 test failed.*total 4.*");
    }

    @Test
    public void noParams() {
        expectRule.pass("noParams");
    }

    @Test
    public void failWithoutParams() {
        expectRule.fail("failWithoutParams")
                .withMessage("FAiLURE");
    }

    @Test
    public void manyParameters() {
        expectRule
                .pass("manyParameters 1st=34 2nd=sdf nbr=1 boolean=false")
                .pass("manyParameters 1st=42 2nd=dfwe nbr=2 boolean=false")
                .pass("manyParameters 1st=34 2nd=dfwe")
                .fail("manyParameters 1st=42 2nd=sdf nbr=3 boolean=true")
                        .withMessage(".*mix.*2.*but.*3.*")
                .pass("manyParameters 1st=34 2nd=sdf nbr=2 boolean=true")
                .fail("manyParameters 1st=42 2nd=dfwe nbr=1 boolean=true")
                        .withMessage(".*mix.*2.*but.*1.*")
                .pass("manyParameters 1st=34 2nd=sdf nbr=3 boolean=false")
                .pass("manyParameters 1st=42 2nd=dfwe nbr=3 boolean=false")
                .fail("manyParameters")
                        .withMessage("2 tests failed.*total 8.*");
    }

    enum RegularVintageTarget {
        RegularVintage(RegularVintage.class),
        RegularLegacy(RegularLegacy.class) {
            final Pattern commonTestNameMethodPart = Pattern
                    .compile("(?<=^.)[^(\\\\]++");

            @Override
            VerifyVintageRule asRule() {
                return new VerifyVintageRule(vintageTestClass) {
                    String toLegacyTestMethodName(String testName) {
                        Matcher m = commonTestNameMethodPart.matcher(testName);
                        return "test"
                                + Character.toUpperCase(testName.charAt(0))
                                + (m.find() ? m.group() : testName.substring(1));
                    }

                    @Override
                    public Statement apply(Statement base, Description desc) {
                        return super.apply(base, Description.createTestDescription(
                                RegularLegacy.class,
                                toLegacyTestMethodName(desc.getMethodName())));
                    }

                    @Override
                    protected void customize(List<VerifyVintageRule.ResultVerifier> expectations) {
                        if (expectations.isEmpty()) {
                            return;
                        }
                        List<VerifyVintageRule.ResultVerifier> original =
                                new ArrayList<VerifyVintageRule.ResultVerifier>(expectations);
                        String methodNameStart = original.get(0).displayNameRgx.substring(0,4);

                        expectations.clear();
                        for (VerifyVintageRule.ResultVerifier eachVrf : original) {
                            if (false == eachVrf.displayNameRgx.startsWith(methodNameStart)) {
                                expectations.add(eachVrf);
                                continue;
                            }
                            String newDisplayNameRgx =
                                    toLegacyTestMethodName(eachVrf.displayNameRgx);
                            String expectedMsgRgx = eachVrf.getMessageRgx();
                            if (null == expectedMsgRgx) {
                                pass(newDisplayNameRgx);
                            } else {
                                fail(newDisplayNameRgx)
                                        .withMessage(expectedMsgRgx);
                            }
                        }
                    }
                };
            }
        },
        DeprecatedVintage(DeprecatedVintage.class),
        PowermockDelegate(PowermockDelegate.class) {
            @Override VerifyVintageRule asRule() {
                return super.asRule().forceFilterDelegation();
            }
        },
        PowermockRegular(PowermockRegular.class);

        final Class<?> vintageTestClass;

        RegularVintageTarget(Class<?> vintageTestClass) {
            this.vintageTestClass = vintageTestClass;
        }

        VerifyVintageRule asRule() {
            return new VerifyVintageRule(vintageTestClass);
        }
    }
}
