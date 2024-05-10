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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import junit.framework.TestCase;
import org.lazyparams.showcase.Ensembles;
import org.lazyparams.showcase.Timing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.lazyparams.showcase.Ensembles.*;

/**
 * Template for verifying ensembles with JUnit-3 style tests
 *
 * @author Henrik Kaipe
 */
public class EnsemblesLegacy extends TestCase {

    static final Comparator<Number> numberComparator = new Comparator<Number>() {
        @Override public int compare(Number x, Number y) {
            BigDecimal bigX = x instanceof BigDecimal
                    ? (BigDecimal) x : new BigDecimal("" + x);
            BigDecimal bigY = y instanceof BigDecimal
                    ? (BigDecimal) y : new BigDecimal("" + y);
            return bigX.compareTo(bigY);
        }
    };

    @Override
    protected void setUp() {
        Timing.displayFromNow();
    }

    public void test2fail() {
        try {
            Ensembles.<Boolean,Class<? extends Exception>>
            use(false, SQLException.class)
            .or(true, IOException.class)
            .asLazyDuo("throw", "of")
            .execute(new Duo.Consumer<Boolean,Class<? extends Exception>,IOException>() {
                @Override
                public void accept(Boolean mustThrowIt, Class<? extends Exception> xType)
                        throws IOException {
                    if (xType == IOException.class) {
                        throw new IOException("Here we go");
                    }
                }
            });
        } catch (IOException expected) {
            return;
        }
        throw new AssertionError("Intentional failure");
    }

    public void testDuo() throws IOException {
        final FinalString result = asArgumentsTo(
                new Duo.Function<FinalString,String,FinalString,IOException>() {
            @Override
            public FinalString apply(FinalString finalStr, String name) throws IOException {
                return finalStr;
            }
        }).use(new FinalString("FOO"), "foo")
                .or(new FinalString("bar"), "BAR")
                .or(new FinalString("Hmm"), "")
                .asParameter(new ToDisplayFunction<FinalString>() {
            @Override public CharSequence apply(FinalString value) {
                return "finalStr=" + value.str;
            }
        }).pickValue();
        LazyParams.pickValue("Final string value", result.str);
    }

    public void testMaxMin() {
        BigDecimal bigDecMax = new BigDecimal("12000000032.78"),
                bigDecMin = new BigDecimal("-123000321092.3432");
        BigInteger bigIntMax = new BigInteger("1234567890321"),
                bigIntMin = new BigInteger("-12389012380");
        Number longMax = 987000123000L,
                dblMin = -3423523.2314;

        Number picked =
                use((Number)new BigInteger("12000000032"), (Number)12313213, (Number)bigDecMax)
                .or(bigIntMax, 12300892, new BigDecimal("1234567890320.99"))
                .or(new BigInteger("987000122999"), longMax, bigDecMin)
                .asParameter("max-options")
                .pickValue().applyOn(max());

        LazyParams.pickValue("max-pick", picked);
        assertThat(picked).as("Max pick")
                .isIn(/*Fail 1st intentionally:bigDecMax,*/ bigIntMax, longMax);

        picked = use((Number)(-999999), (Number)bigIntMin, (Number)bigDecMax)
                .or(new BigInteger("-123000321092"), -123000321092L, bigDecMin)
                .or(dblMin, new AtomicLong(-3423523), new BigInteger("-1239032"))
                .asLazyTrio("min-options")
                .applyOn(min());

        LazyParams.pickValue("min-pick", picked);
        assertThat(picked).as("Min pick").isIn(dblMin, bigDecMin, bigIntMin);
    }

    public void testArrayParam() {
        LazyParams.pickValue("x-int", 1, 2, 3);
        use(23, "1st", new Object[] {42, "2nd"})
                .or(42, "3rd", new Object[] {23, "4th"})
                .asParameter("number", "order", "array")
                .pickValue()
                .execute(new Trio.Consumer<Integer,String,Object[],Error>() {
            @Override
            public void accept(Integer nbr, String order, Object[] array) {
                assertThat(nbr + (Integer)array[0])
                        .as("Sum of ints")
                        .isEqualTo(65);
                if (1 == LazyParams.pickValue("x-int", 1,2,3)) {
                    assertEquals("order", "3rd", order);
                }
            }
        });
    }

    public void testComplicatedFunctionInstance() {
        int result = use(1,2,3,4,"4").or(5,6,7,8,"8")
                .asLazyQuintet("n1","n2","n3","n4","expect")
                .applyOn(new DoubledFunctions());
        switch (result) {
            default:
                throw new AssertionError("Unexpected result ");
            case 4: case 8: //Expected!
        }
    }

    static class FinalString
    implements Quintet.Function<Integer,Integer,Integer,Integer,String,Integer,Error> {
        final String str;
        FinalString(String str) { this.str = str; }

        @Override
        public Integer apply(Integer t, Integer u, Integer v, Integer w, String x) {
            return Ensembles.use(w, x).asLazyDuo("with being inspected")
                    .applyOn((Duo.Function<Integer,String,Integer,Error>)this);
        }
    }

    static class DoubledFunctions extends FinalString
    implements Duo.Function<Integer,String,Integer,Error> {
        public DoubledFunctions() { super(""); }

        @Override public Integer apply(Integer t, String u) {
            assertThat("" + t).as(t + " as string").isEqualTo(u);
            return t;
        }
    }

    <N extends Number> AllEnsemblesFunction<N,N,Error> max() {
        return Ensembles.groupBy(new GroupFunction<N,N,Error>() {
            @Override
            public N apply(List<N> arguments) {
                Collections.sort(arguments, numberComparator);
                return arguments.get(arguments.size() - 1);
            }
        });
    }
    <N extends Number> AllEnsemblesFunction<N,N,Error> min() {
        return Ensembles.groupBy(new GroupFunction<N,N,Error>() {
            @Override
            public N apply(List<N> arguments) {
                Collections.sort(arguments, numberComparator);
                return arguments.get(0);
            }
        });
    }
}
