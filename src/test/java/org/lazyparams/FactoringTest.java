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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * This test shows off stability of multiple scopes on the JUnit Platform support.
 * Unfortunately it is probably also the worst regression test of this code base.
 * Sorry ...
 *
 * @author Henrik Kaipe
 */
public class FactoringTest {

    @Rule
    public VerifyJupiterRule expect = new VerifyJupiterRule(Factoring.class);

    List<String> methodsExpectBase = Arrays.asList(
            "failWithoutParams :FA.*",
            "manyParameters 1st=34 2nd=sdf nbr=1 boolean=false",
            "manyParameters 1st=42 2nd=dfwe nbr=2 boolean=false",
            "manyParameters 1st=34 2nd=dfwe",
            "manyParameters 1st=42 2nd=sdf nbr=3 boolean=true:.*mix.*2.*3.*",
            "manyParameters 1st=34 2nd=sdf nbr=2 boolean=true",
            "manyParameters 1st=42 2nd=dfwe nbr=1 boolean=true:.*mix.*2.*1.*",
            "manyParameters 1st=34 2nd=sdf nbr=3 boolean=false",
            "manyParameters 1st=42 2nd=dfwe nbr=3 boolean=false",
            "manyParameters :2.*fail.*total 8.*",
            "noParamsHere ",
            "normal 1st=34 2nd=sdf",
            "normal 1st=42 2nd=dfwe",
            "normal 1st=34 2nd=dfwe:Fail here",
            "normal 1st=42 2nd=sdf",
            "normal :1.*fail.*total 4.*",
            "parameterization :.+",
            "repeat 1st=43 2nd=prime",
            "repeat 1st=242 2nd=other",
            "repeat 1st=43 2nd=other:Fail repeat",
            "repeat 1st=242 2nd=prime",
            "repeat :1.*fail.*total 4.*"
            );
    List<String> containerizedExpectBase = Stream.of("901","73","3","8")
            .map(" / failer="::concat)
            .collect(Collectors.toList());

    @Test
    public void tests() {
        Pattern baseRgx = Pattern.compile("([^:]++)\\:?(.+)?");
        Pattern bridgeRgx = Pattern.compile("scoped bridge (.+) /.*");
        for (String installScene : new String[] {"force uninstall", "already installed"}) {
            for (String factoryScene : new String[] {
                    "",
                    "filter on normal",
                    "filter on Param containerized",
                    "containerized",
                    "filter on normal containerized",
                    "filter on Param"}) {
                final String parentScene = installScene + " / *" + factoryScene;
                AtomicInteger indexCount = new AtomicInteger(0);
                AtomicReference<String> oldStartOfName = new AtomicReference<>("---");

                if (parentScene.contains("containerized")) {
                    containerizedExpectBase.stream()
                            .map(sfx -> "scoped bridge " +  parentScene + sfx)
                            .forEach(name -> {
                        Matcher m = bridgeRgx.matcher(name);
                        assertTrue("name must be on bridge pattern", m.matches());
                        String legacyName = "tests\\(\\) " + m.group(1)
                                + "\\[1\\] " + name.substring(m.start(1));
                        expect.pass(name, legacyName);
                    });
                    expect.pass("scoped bridge " + parentScene,
                            "tests\\(\\) " + parentScene + "\\[1\\] " + parentScene);
                }
                methodsExpectBase.stream()
                        .filter(parentScene.contains("normal")
                                ? xp -> xp.contains("normal")
                                : parentScene.contains("Param")
                                ? xp -> xp.contains("Param")
                                : xp -> true)
                        .forEach(xp -> {
                    Matcher m = baseRgx.matcher(xp);
                    assertTrue("Expect base must match: " + xp, m.matches());
                    String name = m.group(1);
                    int afterSpaceIndex = name.indexOf(' ') + 1;
                    if (false == name.startsWith(oldStartOfName.get())) {
                        indexCount.incrementAndGet();
                        oldStartOfName.set(name.substring(0, afterSpaceIndex));
                    }
                    name = name.substring(0, afterSpaceIndex)
                            + parentScene + " *(/ *)*" + name.substring(afterSpaceIndex);
                    String legacyName = parentScene.contains("containerized")
                            ? "tests\\(\\) " + parentScene + "\\[2\\] " + parentScene
                            + " /\\[" + indexCount + "\\] " + name.substring(afterSpaceIndex)
                            : "tests\\(\\) " + parentScene + "\\["
                            + indexCount +  "\\] *" + name.substring(afterSpaceIndex);
                    String msg = m.group(2);
                    if (null != msg) {
                        expect.fail(name, legacyName).withMessage(msg);
                    } else {
                        expect.pass(name, legacyName);
                    }
                });
                if (parentScene.endsWith("containerized")) {
                    expect.pass("container " + parentScene + "( /)*", "tests\\(\\) "
                            + parentScene + "\\[2\\] "
                            + parentScene + "( /)*");
                }
                expect.pass("tests " + parentScene + " */?",
                        "tests\\(\\) " + parentScene + " *");
            }
            expect.pass("tests " + installScene, "tests\\(\\) " + installScene);
            expect.pass(Factoring.class.getSimpleName() + " " + installScene,
                    Factoring.class.getName() + " " + installScene);
        }
    }
}
