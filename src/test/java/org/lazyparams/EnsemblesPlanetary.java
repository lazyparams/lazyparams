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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import org.junit.jupiter.api.Test;
import org.lazyparams.showcase.Ensembles;
import org.lazyparams.showcase.FalseOrTrue;
import org.lazyparams.showcase.ToList;
import org.lazyparams.showcase.ScopedLazyParameter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Template for verifying ensembles on JUnit-5
 *
 * @author Henrik Kaipe
 */
public class EnsemblesPlanetary {

    static class Person {
        private String firstName, lastName;
        private int yearsOfExperience;

        Person(String firstName, String lastName) {
            this(firstName, lastName, 0);
        }
        Person(String firstName, String lastName, int yearsOfExperience) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.yearsOfExperience = yearsOfExperience;
        }

        static Person newPerson(String firstName, String lastName, int yearsOfExperience) {
            return new Person(firstName, lastName, yearsOfExperience);
        }

        static Person pick() {
            if (1 == LazyParamsCoreUtil.makePick(Pick.ID, true, 2)) {
                return Ensembles.use("Robin", "Hood")
                        .or("Little", "John")
                        .asLazyDuo("first-name", "last-name")
                        .applyOn(Person::new);
            } else {
                return Ensembles
                        .asArgumentsTo(Person::newPerson)
                        .use("King", "Arthur", 50)
                        .or("Sir", "Lancelot", 9)
                        .asParameter(p -> "person=" + Arrays
                                .asList(p.firstName, p.lastName, p.yearsOfExperience))
                        .pickValue();
            }
        }
        static Ensembles.Trio<String,String,Integer> xperiencedOptions() {
            return Ensembles.use("Gustav", "Vasa", 500)
                    .or("Magnus", "Ladulås", 600)
                    .asLazyTrio((fName,lName,x) -> String.format("%s %s %s", fName, lName, x));
        }
        private enum Pick{ID};
    }

    static ScopedLazyParameter<Ensembles.Trio<Boolean,String,Integer>> myBis = Ensembles
            .use(false, "sadf", 314)
            .or(true, "okid", -241)
            .fullyCombinedGlobally()
            .asParameter("myBis");

    static ScopedLazyParameter<Person> personParam = Ensembles
            .asArgumentsTo((String fst, String lst, Integer yrs) -> new Person(fst,lst,yrs))
            .use("King", "Arthur", 51)
            .or("Sir", "Lancelot", 8)
            .fullyCombinedGlobally()
            .asParameter(p -> p.firstName + " the " + p.yearsOfExperience + "th " + p.lastName);

    static Ensembles.Trio<Integer,Boolean,String> lazyTrio = Ensembles
            .use(23, false, "ffASssffess")
            .or(-9, true, "nonsese")
            .asLazyTrio("myLazy");

    static ScopedLazyParameter<List<Person>> sherwood = Ensembles
            .use("Robin", "Hood", 1)
            .or("Little", "John", 2)
            .asArgumentsTo(Person::new)
            .asParameter(list -> "Thiefs" + list.stream()
                    .map(p -> p.lastName + " of " + p.firstName)
                    .collect(Collectors.toList())
                    .toString(),
                    ToList.combineOneOrTwo());

    @Test public void pickFromStaticParams() {
        Person person = personParam.pickValue();
        myBis.pickValue();
        if (FalseOrTrue.pickBoolean(
                "Reconfirm person first name", "Reconfirm person experience")) {
            assertThat(person.firstName).as("First name to reconfirm")
                    .isEqualTo(personParam.pickValue().firstName);
        } else {
            assertThat(person.yearsOfExperience).as("Experience to reconfirm")
                    .isEqualTo(personParam.pickValue().yearsOfExperience);
        }
    }

    @Test public void withLazy() {
        ScopedLazyParameter<Ensembles.Trio<Integer,String,Integer>> dbling = Ensembles
                .use(personParam.pickValue().yearsOfExperience, "personParam",
                        personParam.pickValue().yearsOfExperience * 2)
                .or(myBis.pickValue().applyOn((b,s,i) -> i), "myBis",
                        myBis.pickValue().applyOn((b,s,i) -> 2*i))
                .asParameter("int", "from", "doubled_to");
        lazyTrio.execute((i,b,s) -> pickFromStaticParams());
        List<?> thiefList = sherwood.pickValue();
        if (2 <= thiefList.size()) {
            assertThat(myBis.pickValue().applyOn((b,s,i) -> i).intValue())
                    .as("int of myBis")
                    .isGreaterThan(0);
        }
    }

    @Deprecated
    @Test void grpJoinBy() {
        String joined = Ensembles
                .use("foo", "bar", "buz")
                .or("213", "321", "000")
                .asLazyTrio(Ensembles.groupBy(list -> "args_x" + list.size()
                        + "=" + list.stream().collect(Collectors.joining(":"))))
                .applyOn(Ensembles.joining("__"));
        LazyParams.pickValue("joined", joined);
    }

    @Test void test2fail() {
        new EnsemblesLegacy().test2fail();
    }

    @Test void duo() throws IOException {
        new EnsemblesLegacy().testDuo();
    }

    @Test void maxAndMin() {
        new EnsemblesLegacy().testMaxMin();
    }

    @Test
    public void arrayParam() {
        new EnsemblesLegacy().testArrayParam();
    }

    @Test
    public void complicatedFunctionInstance() {
        new EnsemblesLegacy().testComplicatedFunctionInstance();
    }

    @Test void explicitReuse() {
        Ensembles.Trio<String, String, Integer> optionsPick = Person.xperiencedOptions();
        Person king = optionsPick.applyOn(Person::new);
        optionsPick.execute((fName,lName,x) -> {
            assertThat(king.firstName).as("King first name")
                    .isEqualTo(fName);
            assertThat(king.lastName).as("King last name")
                    .isEqualTo(lName);
            assertThat(king.yearsOfExperience)
                    .as("King experience")
                    .isGreaterThan(LazyParams.pickValue(
                            "minX", 350, 450, 505));
        });
        Person kingAgain = Person.xperiencedOptions().applyOn(Person::new);
        assertThat(kingAgain.firstName).as("King Again first name")
                .isEqualTo(king.firstName);
        assertThat(kingAgain.lastName).as("King Again last name")
                .isEqualTo(king.lastName);
        assertThat(kingAgain.yearsOfExperience).as("King Again experience")
                .isEqualTo(king.yearsOfExperience);
    }

    @Test void implicitReuse() {
        if (FalseOrTrue.pickBoolean("King or outlaw:")) {
            assertThat(Person.pick().firstName)
                    .as("First name")
                    .isNotEqualTo("Sir");
        }
        if (FalseOrTrue.pickBoolean("Not so experienced:")) {
            assertThat(Person.pick().yearsOfExperience)
                    .as("Years of experience")
                    .isLessThan(10);
        }
    }

    @Test void trio() throws IOException {
        Ensembles
                .use("Sko", 23, false)
                .or("Byxor", 21, true)
                .or("Hatt", 19, null)
                .or("Cykel", 15, true)
                .asLazyTrio("my trio")
                .applyOn(this::runTrio);
        Ensembles
                .asArgumentsTo(this::runTrio)
                .use("Sko", 23, false)
                .or("Byxor", 21, true)
                .or("Hatt", 19, null)
                .or("Cykel", 15, true)
                .asParameter("apply1st").pickValue();
    }

    String runTrio(String toWear, int toCount, Boolean isIt) throws IOException {
        assertThat(toWear).as("My clothes")
                .isIn("Byxor", "Hatt");
        return toWear + "-factory#" + toCount;
    }

    @Test void invokeNbrFunction() {
        /*
         * Tests on this project need to use java-8 - on later Java editions
         * (version 10 or later) it would be convenient to use keyword 'var'
         * for the 'options' and 'pickedOctet' declarations:  */
        Ensembles.Octet.EnsembleOptions<
                    Double, Double, Double, Double, Double,
                    ToDoubleFunction<double[]>, String, Double>
                options = Ensembles
                .use(3.42, 3.902, 12.03, -4.0, -11.3,
                        (ToDoubleFunction<double[]>)this::max, "max", 12.03)
                .or(3.03, -1.048, 0.234, -16.9, 23.0, nbrs -> min(nbrs), "min", -16.9)
                .or(-123.2, 12.0, 23.0, -3.0, 0.0, this::sum, "sum", -91.2)
                .or(12.3, -341.3, 32.0, 78.2356, 3.8, this::average, "average", -42.99);

        Ensembles.Octet<Double, Double, Double, Double, Double,
                ToDoubleFunction<double[]>, String, Double>
                pickedOctet = FalseOrTrue
                .pickBoolean("presented by lambda-function", "presented by parameter-names")
                ? options.asLazyOctet((n1,n2,n3,n4,n5, target, name,expectedResult)
                        -> "and expecting " + name + Arrays.asList(n1,n2,n3,n4,n5)
                        + " to return " + expectedResult)
                : options.asParameter("with args n1", "n2", "n3", "n4", "n5",
                        null, "on function", "expect")
                        .pickValue();
        pickedOctet.execute((n1,n2,n3,n4,n5, target, name, expectedResult) -> {
            assertThat(target.applyAsDouble(new double[] {n1,n2,n3,n4,n5}))
                    .as("Result from " + name)
                    .isEqualTo(expectedResult);                    
        });
    }

    double max(double... nbrs) {
        return DoubleStream.of(nbrs).max().orElseThrow(NoSuchElementException::new);
    }
    double min(double... nbrs) {
        return DoubleStream.of(nbrs).min().orElseThrow(NoSuchElementException::new);
    }
    double sum(double... nbrs) {
        return DoubleStream.of(nbrs).sum();
    }
    double average(double... nbrs) {
        return DoubleStream.of(nbrs).average().orElseThrow(NoSuchElementException::new);
    }
}
