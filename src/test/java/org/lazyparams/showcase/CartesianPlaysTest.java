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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.lazyparams.VerifyJupiterRule;

/**
 * Most tests here verify test execution with four parameters (plus one
 * additional in class scope) in fine-grained detail.
 * The fine granulite level makes these tests complex and difficult to maintain
 * but it is for a reason. A couple of the {@link CartesianPlays} test-methods
 * demonstrate the existence of certain scenarios, during which a fully combined
 * pocket can help the {@link org.lazyparams.core.Lazer} algorithms to
 * fulfill all pairwise combinations more efficiently.<br/>
 * This particular phenomena happens when two independent parameters (in this
 * case <code>X-open</code> and <code>Y-open</code>) both have values that can
 * unlock the introduction of an extra parameter (<code>foo</code>). In this way
 * the two independent parameters (<code>X-open</code> and <code>Y-open</code>)
 * form a kind of three-way dependency with parameter <code>foo</code>.
 * The presence of this three-way dependency,
 * which involves one parameter that is not always present, can confuse
 * the {@link org.lazyparams.core.Lazer} pairwise combine strategy, for which
 * it gets more difficult to make parameter value picks that quickly cover
 * all pairwise combinations. What is not so intuitive is how the algorithm
 * efficiency can be helped by introducing a fully combined pocket for the two
 * parameters (<code>X-open</code> and <code>Y-open</code>) that determine
 * the introduction of the parameter (<code>foo</code>, as can be
 * studied by comparing how {@link CartesianPlays#singleCartesian()} and
 * {@link CartesianPlays#noCartesian()} find their ways through the parameter
 * combinations. It turns out
 * {@link CartesianPlays#noCartesian()} needs more iterations than
 * {@link CartesianPlays#singleCartesian()}(!!) (19 vs 17) to uncover all
 * pairwise combinations, even though the latter test must also satisfy a
 * fully combined pocket on top of all the pairwise combinations!
 * <br/>
 * In the process of uncovering all pairwise combinations,
 * {@link org.lazyparams.core.Lazer} can normally deal successfully with a
 * dependency between two parameters, as it comes natural with the pairwise
 * approach, but the three-way dependency in these tests makes it
 * difficult for the algorithm that cannot efficiently conclude that the
 * introduction of parameter <code>foo</code> is determined by values on both
 * parameters <code>X-open</code> and <code>Y-open</code>. Instead it gets lost
 * in the dark as it will require many iterations to conclude that the one
 * remaining parameter <code>1stFrom3</code> does not impact the introduction
 * of parameter <code>foo</code>. (This phenomena only occurs when there are
 * four or more parameters.) The introduction of a fully combined pocket for
 * <code>X-open</code> and <code>Y-open</code> (using
 * {@link ScopedLazyParameter.Combiner#fullyCombinedOn(CartesianProductHub)})
 * helps the algorithm by making it treat the combination of
 * <code>X-open</code> and <code>Y-open</code> as one parameter instead of two,
 * with each value being a pair combination of
 * <code>X-open</code> and <code>Y-open</code>. Therewith the three-way
 * dependency has been turned into two-way dependency that the
 * {@link org.lazyparams.core.Lazer} algorithm can quickly recognize while
 * uncovering the cartesian product of all values from
 * <code>X-open</code> and <code>Y-open</code>.
 * <br/><br/>
 * If all four parameters are independent of each other (from the
 * perspective of the pairwise combinations by {@link org.lazyparams.core.Lazer}
 * algorithm), as would be the case if all four parameters are introduced
 * regardless of values on other parameters (as is enforced when
 * {@link CartesianPlays#globalFooSupport} is <code>true</code>),
 * then the pairwise algorithms work well without cartesian-pocket
 * and as can be seen with {@link CartesianPlays#singleCartesian()} (when
 * {@link CartesianPlays#globalFooSupport} is <code>true</code>) it now requires
 * more iterations than {@link CartesianPlays#globalFooSupport} (16 vs 14).
 * I.e. the fully combined pocket is no longer helpful.
 *
 * @author Henrik Kaipe
 */
public class CartesianPlaysTest {

    @Rule
    public VerifyJupiterRule expect = new VerifyJupiterRule(CartesianPlays.class);

    void noCommonCartesian(String testMethodName) {
        if (false == testMethodName.endsWith(")")) {
            testMethodName += "(\\(\\))?";
        }
        List<String> globallyClosedTests = Arrays.asList(
                "%s=1 %s=false %s=false",
                "%s=2 %s=true %s=false %s=A",
                "%s=3 %s=true %s=true %s=B",
                "%s=2 %s=true %s=true %s=C",
                "%s=3 %s=false %s=false",
                "%s=2 %s=false %s=false",
                "%s=3 %s=true %s=false %s=D",
                "%s=2 %s=false %s=true %s=D",
                "%s=3 %s=false %s=true %s=A",
                "%s=1 %s=false %s=true %s=C",
                "%s=1 %s=false %s=true %s=B",
                "%s=1 %s=true %s=false %s=C",
                "%s=1 %s=true %s=false %s=B",
                "%s=1 %s=true %s=true %s=A",
                "%s=2 %s=false %s=true %s=B",
                "%s=3 %s=true %s=true %s=C",
                "%s=1 %s=false %s=true %s=D");
        List<String> globallyOpenTests = Arrays.asList(
                "%s=1 %s=false %s=false %s=A",
                "%s=2 %s=true %s=false %s=B",
                "%s=3 %s=false %s=true %s=C",
                "%s=2 %s=true %s=true %s=D",
                "%s=1 %s=true %s=true %s=C",
                "%s=3 %s=false %s=false %s=D",
                "%s=2 %s=false %s=true %s=B",
                "%s=3 %s=true %s=false %s=A",
                "%s=1 %s=false %s=false %s=B",
                "%s=2 %s=false %s=false %s=C",
                "%s=1 %s=true %s=true %s=A",
                "%s=3 %s=true %s=true %s=B",
                "%s=1 %s=false %s=true %s=D",
                "%s=2 %s=true %s=false %s=A");
        Iterator<String> iterStaticTestNameParts = Arrays
                .asList(testMethodName, testMethodName + " Global open")
                .iterator();
        Iterator<String> iterFailureSummaries = Arrays
                .asList(".*3 .*fail.*total.* 17.*", ".*4 .*fail.*total.* 14.*")
                .iterator();
        for (List<String> repeatedMethodAppends : new List[] {
                globallyClosedTests, globallyOpenTests}) {
            String testNameStaticPart = iterStaticTestNameParts.next();
            for (String methodAppend : repeatedMethodAppends) {
                String fullTestName = testNameStaticPart + String.format(
                        methodAppend, "( /)? 1stFrom3", "X-open", "Y-open", "foo");
                if (fullTestName.endsWith("A")) {
                    expect.fail(fullTestName).withMessage(".*foo.*A.*A.*");
                } else {
                    expect.pass(fullTestName);
                }
            }
            String classDisplay = testNameStaticPart.replace(
                    testMethodName, CartesianPlays.class.getSimpleName());
            String classLegacy = testNameStaticPart.replace(
                    testMethodName, CartesianPlays.class.getName());
            expect.fail(testNameStaticPart).withMessage(iterFailureSummaries.next())
                    .pass(classDisplay, classLegacy);
        }
    }

    @Test
    public void noCartesian() {
        noCommonCartesian("noCartesian");
    }
    @Test
    public void individualCartesians() {
        noCommonCartesian("individualCartesians");
    }
    @Test
    public void singleCartesian() {
        expect.pass(" 1stFrom3=1 X-open=false Y-open=false")
                .fail(" 1stFrom3=2 X-open=true Y-open=true foo=A")
                        .withMessage(".*foo.*A.*A.*")
                .pass(" 1stFrom3=3 X-open=true Y-open=false foo=B")
                .pass(" 1stFrom3=2 X-open=true Y-open=false foo=C")
                .pass(" 1stFrom3=3 X-open=false Y-open=true foo=D")
                .fail(" 1stFrom3=3 X-open=false Y-open=true foo=A")
                        .withMessage(".*foo.*A.*A.*")
                .pass(" 1stFrom3=3 X-open=false Y-open=true foo=C")
                .pass(" 1stFrom3=2 X-open=false Y-open=true foo=B")
                .pass(" 1stFrom3=2 X-open=true Y-open=true foo=D")
                .pass(" 1stFrom3=3 X-open=true Y-open=true foo=C")
                .pass(" 1stFrom3=1 X-open=true Y-open=false foo=D")
                .pass(" 1stFrom3=1 X-open=true Y-open=true foo=B")
                .fail(" 1stFrom3=1 X-open=false Y-open=true foo=A")
                        .withMessage(".*foo.*A.*A.*")
                .pass(" 1stFrom3=1 X-open=false Y-open=true foo=C")
                .pass(" 1stFrom3=2 X-open=false Y-open=false")
                .fail(" 1stFrom3=3 X-open=true Y-open=false foo=A")
                        .withMessage(".*foo.*A.*A.*")
                .pass(" 1stFrom3=3 X-open=false Y-open=false")
                .fail("").withMessage(".*4 .*fail.*total.* 17.*")
                .pass(CartesianPlays.class.getSimpleName() + " *",
                        CartesianPlays.class.getName() + " *");
        expect.fail(" Global open / 1stFrom3=1 X-open=false Y-open=false foo=A")
                        .withMessage(".*foo.*A.*A.*")
                .pass(" Global open / 1stFrom3=2 X-open=true Y-open=true foo=B")
                .pass(" Global open / 1stFrom3=3 X-open=false Y-open=true foo=C")
                .pass(" Global open / 1stFrom3=2 X-open=true Y-open=false foo=D")
                .pass(" Global open / 1stFrom3=1 X-open=true Y-open=true foo=C")
                .pass(" Global open / 1stFrom3=3 X-open=false Y-open=false foo=D")
                .pass(" Global open / 1stFrom3=1 X-open=false Y-open=true foo=B")
                .fail(" Global open / 1stFrom3=2 X-open=true Y-open=false foo=A")
                        .withMessage(".*foo.*A.*A.*")
                .pass(" Global open / 1stFrom3=3 X-open=true Y-open=false foo=B")
                .pass(" Global open / 1stFrom3=1 X-open=true Y-open=true foo=D")
                .fail(" Global open / 1stFrom3=3 X-open=true Y-open=true foo=A")
                        .withMessage(".*foo.*A.*A.*")
                .pass(" Global open / 1stFrom3=2 X-open=false Y-open=false foo=C")
                .fail(" Global open / 1stFrom3=1 X-open=false Y-open=true foo=A")
                        .withMessage(".*foo.*A.*A.*")
                .pass(" Global open / 1stFrom3=2 X-open=false Y-open=true foo=D")
                .pass(" Global open / 1stFrom3=1 X-open=true Y-open=false foo=C")
                .pass(" Global open / 1stFrom3=3 X-open=false Y-open=false foo=B")
                .fail(" Global open")
                        .withMessage(".*4 .*fail.*total.* 16.*")
                .pass(CartesianPlays.class.getSimpleName() + " Global open",
                        CartesianPlays.class.getName() + " Global open");
    }

    @Test
    public void repeatBoolean() {
        expect.pass(" repeatedBool=false on new cartesian product hub: repeatedBool=false")
                .pass(" repeatedBool=true on new cartesian product hub: repeatedBool=true")
                .pass(" repeatedBool=false on new cartesian product hub: repeatedBool=true")
                .pass(" repeatedBool=true on new cartesian product hub: repeatedBool=false")
                .pass(" *")
                .pass(CartesianPlays.class.getSimpleName() + " *",
                        CartesianPlays.class.getName() + " *")
                .pass(" Global open / repeatedBool=false on same cartesian product hub ...")
                .pass(" Global open / repeatedBool=true on same cartesian product hub ...")
                .pass(" Global open")
                .pass(CartesianPlays.class.getSimpleName() + " Global open",
                        CartesianPlays.class.getName() + " Global open");
    }
}
