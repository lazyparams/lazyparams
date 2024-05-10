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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Rule;
import org.junit.Test;
import org.lazyparams.VerifyJupiterRule;

/**
 * @author Henrik Kaipe
 */
public class LazyEnsemblesTest {

    @Rule
    public VerifyJupiterRule expect = new VerifyJupiterRule(LazyEnsembles.class) {
        final Pattern comprFmt = Pattern.compile("^(f |t )?([^ ]++) (\\d++) (.*) (f|t)$");

        String explode(String compr) {
            Matcher m = comprFmt.matcher(compr);
            if (false == m.matches()) {
                return compr;
            } else {
                StringBuilder sb = new StringBuilder(" 0\\.\\d{3}s");
                String grp1 = m.group(1);
                if (null != grp1) {
                    sb.append(" expct=")
                            .append('t' == grp1.charAt(0) ? "true" : "false");
                }
                return sb.append(" order=").append(m.group(2))
                        .append(" int=").append(m.group(3))
                        .append(" desc=").append(m.group(4))
                        .append(" b=")
                        .append("t".equals(m.group(5)) ? "true" : "false")
                        .toString();
            }
        }

        @Override
        public VerifyJupiterRule.SpecifyFailMessageOrNextResult fail(String nameRgx) {
            return super.fail(explode(nameRgx));
        }
        @Override
        public VerifyJupiterRule.NextResult pass(String nameRgx) {
            return super.pass(explode(nameRgx));
        }
    };

    @Test
    public void testBool() {
        String normalFailmsg = "Q-Param boolean v.*";
        expect
                .pass("f 1st 1 nbr 1 f")
                .pass("t 2nd 22 nbr 2nd t")
                .fail("f 3rd 3 three t").withMessage(normalFailmsg)
                .fail("t 4th 404 goes forth f").withMessage(normalFailmsg)
                .fail("f 2nd 22 nbr 2nd t").withMessage(normalFailmsg)
                .fail("t 1st 1 nbr 1 f").withMessage(normalFailmsg)
                .pass("f 4th 404 goes forth f")
                .pass("t 3rd 3 three t")
                .fail("").withMessage("4.*fail.*total 8.*");
    }

    @Test
    public void testModulo() {
        String normalFailmsg = "Modulo 2.*";
        expect
                .fail("1st 1 nbr 1 f").withMessage(normalFailmsg)
                .pass("2nd 22 nbr 2nd t")
                .fail("3rd 3 three t").withMessage(normalFailmsg)
                .pass("4th 404 goes forth f")
                .fail("").withMessage("2.*fail.*total 4.*");
    }
}
