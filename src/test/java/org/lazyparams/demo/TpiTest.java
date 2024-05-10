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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.lazyparams.VerifyJupiterRule;

/**
 * @author Henrik Kaipe
 */
public class TpiTest {

    String displayPrefix = "", legacyPrefix = "";
    int tpiCount = 0;

    @Rule
    public final VerifyJupiterRule expect = new VerifyJupiterRule(Tpi.class) {
        String prefix(String prefix, String nameRgx) {
            return 0 == nameRgx.length() || ' ' == nameRgx.charAt(0)
                    ? prefix + nameRgx
                    : nameRgx;
        }
        @Override
        public VerifyJupiterRule.SpecifyFailMessageOrNextResult fail(String nameRgx) {
            return super.fail(
                    prefix(displayPrefix, nameRgx), prefix(legacyPrefix, nameRgx));
        }
        @Override
        public VerifyJupiterRule.NextResult pass(String nameRgx) {
            return super.pass(
                    prefix(displayPrefix, nameRgx), prefix(legacyPrefix, nameRgx));
        }
    };

    void setTpiPrefix(String tpiBools) {
        String methodName = expect.getMethod().getName();
        displayPrefix = methodName + "\\[fTpi="
                + String.format(tpiBools.replace(",", ",%s="), "pTpi1", "pTpi2") + "\\]";
        legacyPrefix = methodName + "(\\(.*\\))?\\[" + ++tpiCount + "\\]";
    }

    @Test
    public void allParams() {
        expect.methodParameterTypes(boolean.class, boolean.class);

        setTpiPrefix("false,false,false");
        expect.fail(" fLazy=false mLazy=false").withMessage("Fail on all false.*")
                .pass(" fLazy=true mLazy=true conditional_lazy=false")
                .pass(" fLazy=true mLazy=true conditional_lazy=true")
                .pass(" fLazy=false mLazy=true conditional_lazy=false")
                .pass(" fLazy=false mLazy=true conditional_lazy=true")
                .pass(" fLazy=true mLazy=false conditional_lazy=false")
                .pass(" fLazy=true mLazy=false conditional_lazy=true")
                .fail("").withMessage(".*1.*fail.*total 7.*");

        List<String> happyLazyIteration = Arrays.asList(
                " fLazy=false mLazy=false conditional_lazy=false",
                " fLazy=true mLazy=true conditional_lazy=false",
                " fLazy=false mLazy=true conditional_lazy=true",
                " fLazy=true mLazy=false conditional_lazy=true",
                "");
        Stream.of("false,false,true", "false,true,false", "false,true,true",
                "true,false,false", "true,false,true", "true,true,false")
                .forEach(tpiBool3x -> {
            setTpiPrefix(tpiBool3x);
            happyLazyIteration.forEach(expect::pass);
        });

        setTpiPrefix("true,true,true");
        expect.pass(" fLazy=false mLazy=false conditional_lazy=false")
                .fail(" fLazy=true mLazy=true").withMessage("Fail on all true.*")
                .pass(" fLazy=false mLazy=false conditional_lazy=true")
                .pass(" fLazy=true mLazy=false conditional_lazy=false")
                .pass(" fLazy=false mLazy=true conditional_lazy=true")
                .pass(" fLazy=true mLazy=false conditional_lazy=true")
                .pass(" fLazy=false mLazy=true conditional_lazy=false")
                .fail("").withMessage(".*1.*fail.*total 7.*");

        expect.pass("allParams\\(boolean, boolean\\)");
    }

    @Test
    public void lazyFieldOnly() {
        expect.pass(" fLazy=false").pass(" fLazy=true").pass("");
    }

    @Test
    public void fieldsOnly() {
        setTpiPrefix("false");
        expect.pass(" fLazy=false").pass(" fLazy=true").pass("");
        setTpiPrefix("true");
        expect.pass(" fLazy=false").pass(" fLazy=true").pass("");
        expect.pass("fieldsOnly(\\(\\))?");
    }
}
