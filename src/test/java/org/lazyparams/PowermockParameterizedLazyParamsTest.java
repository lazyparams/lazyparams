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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Henrik Kaipe
 */
public class PowermockParameterizedLazyParamsTest {

    @Rule
    public final VerifyVintageRule expect =
            new VerifyVintageRule(PowermockParameterizedLazyParams.class) {
        final Pattern ints = Pattern.compile("(\\d++) ++");

        String format(String nameRgx) {
            Matcher m = ints.matcher(nameRgx);
            if (m.find() && 0 == m.start()) {
                StringBuilder sb = new StringBuilder("test\\[").append(m.group(1));
                m.find();
                return sb.append("\\] stubbedTime=").append(m.group(1))
                        .append(" timeIndex for ").append(m.group(1))
                        .append('=').append(nameRgx.substring(m.end())).toString();
            } else {
                return nameRgx;
            }
        }
        @Override
        public VerifyVintageRule.SpecifyFailMessageOrNextResult fail(String nameRgx) {
            return super.fail(format(nameRgx));
        }
        @Override
        public VerifyVintageRule.NextResult pass(String nameRgx) {
            return super.pass(format(nameRgx));
        }
    };

    String msg(int exp, int act) {
        return ".*Ramsa-to-verify ordinal.*" + exp + ".*" + act + ".*";
    }

    @Test
    public void test() {
        expect
                .fail("0 12 0 OLE").withMessage(msg(0,0))
                .fail("0 56 1 DOLE").withMessage(msg(1,2))
                .pass("0 84 2 DOFF OLE")
                .pass("0 84 ignored DOFF DOLE")
                .pass("0 84 2 DOFF DOFF")
                .pass("0 12 ignored DOLE DOLE")
                .pass("0 12 ignored DOLE OLE")
                .fail("0 56 ignored DOLE").withMessage(msg(1,2))
                .pass("0 84 ignored DOLE DOFF")
                .pass("0 12 ignored DOFF DOFF")
                .fail("0 56 1 OLE").withMessage(msg(0,2))
                .fail("0 84 2 OLE").withMessage(msg(0,0))
                .fail("0 56 ignored DOFF").withMessage(msg(2,2))
                .fail("0 84 ignored OLE").withMessage(msg(0,0))
                .fail("test\\[0\\]").withMessage(".*count.*max.*7.*")

                .fail("1 991011 0 OLE").withMessage(msg(0,0))
                .fail("1 9872050 1 DOLE").withMessage(msg(1,1))
                .pass("1 991011 ignored DOFF OLE")
                .pass("1 991011 ignored DOFF DOLE")
                .pass("1 9872050 ignored DOFF DOFF")
                .fail("1 9872050 ignored OLE").withMessage(msg(0,1))
                .pass("1 991011 ignored DOLE DOFF")
                .fail("1 9872050 ignored DOLE").withMessage(msg(1,1))
                .pass("1 9872050 ignored DOFF OLE")
                .pass("1 9872050 1 DOFF DOLE")
                .pass("1 991011 0 DOLE DOLE")
                .pass("1 991011 0 DOLE OLE")
                .fail("1 9872050 1 OLE").withMessage(msg(0,1))
                .pass("1 991011 0 DOFF DOFF")
                .pass("1 9872050 1 DOFF OLE")
                .pass("1 9872050 1 DOFF DOFF")
                .fail("1 991011 ignored OLE").withMessage(msg(0,0))
                .pass("1 9872050 ignored DOFF DOLE")
                .fail("test\\[1\\]").withMessage(".*6 .*fail.*total 18.*");
                ;
    }
}
