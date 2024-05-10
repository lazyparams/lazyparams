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

import org.junit.Rule;
import org.junit.Test;

/**
 * @author Henrik Kaipe
 */
public class EnsemblesPlanetaryTest {

    @Rule
    public final VerifyJupiterRule expect = new VerifyJupiterRule(EnsemblesPlanetary.class);

    @Test
    public void grpJoinBy() {
        expect.pass(" args_x3=foo:bar:buz joined=foo__bar__buz")
                .pass(" args_x3=213:321:000 joined=213__321__000")
                .pass("");
    }

    @Test
    public void test2fail() {
        expect.fail(" throw=false of=class java.sql.SQLException")
                .withMessage("Intentional.*")
                .pass(" throw=true of=class java.io.IOException")
                .fail("").withMessage("1 .*fail.*total 2.*");
    }

    @Test
    public void maxAndMin() {
        expect.fail(" max-options=\\[12000000032, 12313213, 12000000032.78\\] max-pick=12000000032.78")
                .pass(" max-options=\\[1234567890321, 12300892, 1234567890320.99\\]"
                + " max-pick=1234567890321 min-options=\\[-999999, -12389012380, 12000000032.78\\] min-pick=-12389012380")
                .pass(" max-options=\\[987000122999, 987000123000, -123000321092.3432\\]"
                + " max-pick=987000123000 min-options=\\[-123000321092, -123000321092, -123000321092.3432\\] min-pick=-123000321092.3432")
                .pass(" max-options=\\[1234567890321, 12300892, 1234567890320.99\\]"
                + " max-pick=1234567890321 min-options=\\[-3423523.2314, -3423523, -1239032\\] min-pick=-3423523.2314")
                .pass(" max-options=\\[987000122999, 987000123000, -123000321092.3432\\]"
                + " max-pick=987000123000 min-options=\\[-999999, -12389012380, 12000000032.78\\] min-pick=-12389012380")
                .pass(" max-options=\\[1234567890321, 12300892, 1234567890320.99\\]"
                + " max-pick=1234567890321 min-options=\\[-123000321092, -123000321092, -123000321092.3432\\] min-pick=-123000321092.3432")
                .pass(" max-options=\\[987000122999, 987000123000, -123000321092.3432\\]"
                + " max-pick=987000123000 min-options=\\[-3423523.2314, -3423523, -1239032\\] min-pick=-3423523.2314")
                .fail("").withMessage("1 .*fail.*total 7.*");
    }

    @Test
    public void duo() {
        expect.pass(" finalStr=FOO Final string value=FOO")
                .pass(" finalStr=bar Final string value=bar")
                .pass(" finalStr=Hmm Final string value=Hmm")
                .pass("");
    }

    @Test
    public void arrayParam() {
        expect.fail(" x-int=1 number=23 order=1st array=\\[42, 2nd\\]")
                        .withMessage(".*expect.+3rd.+was.+1st.*")
                .pass(" x-int=2 number=42 order=3rd array=\\[23, 4th\\]")
                .pass(" x-int=3 number=23 order=1st array=\\[42, 2nd\\]")
                .pass(" x-int=1 number=42 order=3rd array=\\[23, 4th\\]")
                .pass(" x-int=2 number=23 order=1st array=\\[42, 2nd\\]")
                .pass(" x-int=3 number=42 order=3rd array=\\[23, 4th\\]")
                .fail("").withMessage("1 .*fail.*total 6.*");
    }

    @Test
    public void complicatedFunctionInstance() {
        expect.pass(" n1=1 n2=2 n3=3 n4=4 expect=4 with being inspected=\\[4, 4\\]")
                .pass(" n1=5 n2=6 n3=7 n4=8 expect=8 with being inspected=\\[8, 8\\]")
                .pass("");
    }

    @Test
    public void trio() {
        expect.fail(" my trio=\\[Sko, 23, false\\]")
                        .withMessage(".*My clothes.*Sko.*")
                .fail(" my trio=\\[Byxor, 21, true\\]")
                        .withMessage(".*My clothes.*Sko.*")
                .pass(" my trio=\\[Hatt, 19, null\\] apply1st=Byxor-factory#21")
                .fail(" my trio=\\[Cykel, 15, true\\]")
                        .withMessage(".*My clothes.*Cykel.*")
                .pass(" my trio=\\[Byxor, 21, true\\] apply1st=Hatt-factory#19")
                .fail(" my trio=\\[Hatt, 19, null\\]")
                        .withMessage(".*My clothes.*Cykel.*")
                .pass(" my trio=\\[Byxor, 21, true\\] apply1st=Byxor-factory#21")
                .fail(" my trio=\\[Hatt, 19, null\\]")
                        .withMessage(".*My clothes.*Sko.*")
                .fail("").withMessage("Failure count has reached its max at 5.*");
    }

    @Test
    public void invokeNbrFunction() {
        expect.pass(" presented by parameter-names with args n1=3.42 n2=3.902 n3=12.03 n4=-4.0 n5=-11.3 on function=max expect=12.03")
                .pass(" presented by lambda-function and expecting max\\[3.42, 3.902, 12.03, -4.0, -11.3\\] to return 12.03")
                .pass(" presented by parameter-names with args n1=3.03 n2=-1.048 n3=0.234 n4=-16.9 n5=23.0 on function=min expect=-16.9")
                .pass(" presented by lambda-function and expecting min\\[3.03, -1.048, 0.234, -16.9, 23.0\\] to return -16.9")
                .pass(" presented by parameter-names with args n1=-123.2 n2=12.0 n3=23.0 n4=-3.0 n5=0.0 on function=sum expect=-91.2")
                .pass(" presented by lambda-function and expecting sum\\[-123.2, 12.0, 23.0, -3.0, 0.0\\] to return -91.2")
                .fail(" presented by parameter-names with args n1=12.3 n2=-341.3 n3=32.0 n4=78.2356 n5=3.8 on function=average expect=-42.99")
                        .withMessage(".*42\\.99.*42\\.99.*")
                .fail(" presented by lambda-function and expecting average\\[12.3, -341.3, 32.0, 78.2356, 3.8\\] to return -42.99")
                        .withMessage(".*42\\.99.*42\\.99.*")
                .fail("").withMessage("2 .*fail.*total 8.*");
    }

    @Test
    public void implicitReuse() {
        expect.pass(" *")
                .pass(" King or outlaw: person=\\[King, Arthur, 50\\]")
                .pass(" King or outlaw: first-name=Robin last-name=Hood Not so experienced:")
                .fail(" King or outlaw: person=\\[Sir, Lancelot, 9\\]")
                        .withMessage(".*First name.*")
                .pass(" King or outlaw: first-name=Little last-name=John *")
                .fail(" +Not so experienced: person=\\[King, Arthur, 50\\]")
                        .withMessage(".*Years of experience.*")
                .pass(" +Not so experienced: person=\\[Sir, Lancelot, 9\\]")
                .pass(" +Not so experienced: first-name=Robin last-name=Hood")
                .pass(" +Not so experienced: first-name=Little last-name=John")
                .fail(" King or outlaw: person=\\[King, Arthur, 50\\] Not so experienced:")
                        .withMessage(".*Years of experience.*")
                .pass(" King or outlaw: first-name=Robin last-name=Hood *")
                .pass(" King or outlaw: first-name=Little last-name=John Not so experienced:")
                .fail("").withMessage("3 .*fail.*total 12.*");
    }

    @Test
    public void explicitReuse() {
        expect.pass(" Gustav Vasa 500 minX=350")
                .pass(" Magnus Ladulås 600 minX=450")
                .fail(" Gustav Vasa 500 minX=505")
                        .withMessage(".*King experience.*500.*505.*")
                .pass(" Magnus Ladulås 600 minX=350")
                .pass(" Gustav Vasa 500 minX=450")
                .pass(" Magnus Ladulås 600 minX=505")
                .fail("").withMessage("1 .*fail.*total 6.*");
    }

    @Test
    public void pickFromStaticParams() {
        expect.pass(" King the 51th Arthur myBis=\\[false, sadf, 314\\] Reconfirm person experience")
                .pass(" Sir the 8th Lancelot myBis=\\[true, okid, -241\\] Reconfirm person experience")
                .pass(" King the 51th Arthur myBis=\\[true, okid, -241\\] Reconfirm person first name")
                .pass(" Sir the 8th Lancelot myBis=\\[false, sadf, 314\\] Reconfirm person first name")
                .pass(" King the 51th Arthur myBis=\\[false, sadf, 314\\] Reconfirm person first name")
                .pass(" Sir the 8th Lancelot myBis=\\[true, okid, -241\\] Reconfirm person first name")
                .pass(" King the 51th Arthur myBis=\\[true, okid, -241\\] Reconfirm person experience")
                .pass(" Sir the 8th Lancelot myBis=\\[false, sadf, 314\\] Reconfirm person experience")
                .pass("");
    }
}
