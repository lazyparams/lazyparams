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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author Henrik Kaipe
 */
public enum StaticScopeParam {
    NOTHING,
    DISPLAY_XyZ {
        /* This is same as NOTHING in terms of execution paths but with
         * some test-name addition in the report! */
        @Override void onStatic(String paramName) {
            Object displayKey = new Object();
            LazyParamsCoreUtil.displayOnFailure(displayKey, XyZ);
            LazyParamsCoreUtil.displayOnSuccess(displayKey, XyZ);
        }
    },
    FAILURE,
    PASS_ALL_OF1,
    FAIL_ALL_OF1,
    PASS_ALL_OF2,
    FAIL_1ST_OF2,
    FAIL_2ND_OF2,
    FAIL_ALL_OF2,
    PASS_ALL_OF3,
    FAIL_1ST_OF3,
    FAIL_2ND_OF3,
    FAIL_3RD_OF3;

    static final String XyZ = " XyZ";
    static final String staticPlace = "(staticplace)?";
    static final String BEFORE = "beforeAll", AFTER = "afterAll";

    final Object first = 42, second = "2nd", third = true;

    final Matcher<Object> isFailurePick =
            name().startsWith("FAIL_1") ? equalTo(first)
            : name().startsWith("FAIL_2") ? equalTo(second)
            : name().startsWith("FAIL_3") ? equalTo(third)
            : name().startsWith("FAIL") ? notNullValue()
            : nullValue();

    BeforeAllCallback asBeforeAll() {
        return ctx -> onStatic("beforeAll");
    }
    AfterAllCallback asAfterAll() {
        return ctx -> onStatic("afterAll");
    }

    void onStatic(String paramName) {
        assertThat(name() + '@' + paramName,
                pickValue(paramName),
                not(isFailurePick));
    }

    private char lastCharOfName() {
        return name().charAt(name().length() - 1);
    }

    private Optional<Object[]> pickables() {
        switch (lastCharOfName()) {
            case '1': return Optional.of(new Object[] {first});
            case '2': return Optional.of(new Object[] {first, second});
            case '3': return Optional.of(new Object[] {first, second, third});
            default: return Optional.empty();
        }
    }

    boolean moreThanOneOption() {
        switch (lastCharOfName()) {
            default:
                return false;
            case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
                return true;
        }
    }

    private Object pickValue(String paramName) {
        return pickables()
                .map(pickables -> LazyParams
                        .pickValue(paramName, pickables))
                .orElse(name());
    }

    static Object[] afterAllOptions() {
        return Stream.of(values())
                .filter(e -> false == e.name().endsWith("3"))
                .toArray();
    }

    Consumer<VerifyJupiterRule> beforeModifications() {
        return rule -> modify(rule, this::beforeAllTweaksOn);
    }
    Consumer<VerifyJupiterRule> afterModifications() {
        return rule -> modify(rule, this::afterAllTweaksOn);
    }

    private void modify(VerifyJupiterRule whatToModify,
        BiConsumer<VerifyJupiterRule,List<VerifyJupiterRule.ResultVerifier>> modifier) {
        VerifyJupiterRule buildStub = new VerifyJupiterRule(whatToModify.getTestClass());
        modifier.accept(buildStub,
                Collections.unmodifiableList(whatToModify.getExpectations()));
        whatToModify.getExpectations().clear();
        whatToModify.getExpectations().addAll(buildStub.getExpectations());
    }

    private static String applyStatic(String raw, String replacement) {
        if (raw.endsWith(staticPlace) && replacement.endsWith(" /")) {
            replacement = replacement.substring(0, replacement.length() - 2);
        }
        String result = raw.replace(staticPlace, replacement);
        if (result.equals(raw)) {
            throw new IllegalArgumentException("Static place missing: " + raw);
        }
        return result;
    }

    private void beforeAllTweaksOn(VerifyJupiterRule builder,
            List<VerifyJupiterRule.ResultVerifier> original) {
        String classDisplayRgx = builder.getTestClass().getSimpleName();
        String classLegacyRgx = builder.getTestClass().getName();
        switch (this) {
            case NOTHING:
                builder.getExpectations().addAll(original);
                return;
            case FAILURE:
                builder.fail(classDisplayRgx, classLegacyRgx)
                        .withMessage("(?s).*" + name() + ".*");
                return;
            case DISPLAY_XyZ:
                replaceStaticPlace(builder, original, XyZ + "( /)? /");
                builder.pass(classDisplayRgx + XyZ, classLegacyRgx + XyZ);
                return;
            default:
                /* Continue below ... */
        }
        for (Object pickOption : pickables().orElse(new Object[0])) {
            String staticAppendBase = " " + BEFORE + "=" + pickOption;
            if (isFailurePick.matches(pickOption)) {
                builder.fail(
                        classDisplayRgx + staticAppendBase,
                        classLegacyRgx + staticAppendBase)
                        .withMessage("(?s).*" + pickOption + ".*");
            } else {
                String staticAppend = staticAppendBase + "( /)? /";
                replaceStaticPlace(builder, original, staticAppend);
                builder.pass(
                        classDisplayRgx + staticAppendBase,
                        classLegacyRgx + staticAppendBase);
            }
        }
    }

    private void replaceStaticPlace(VerifyJupiterRule builder,
            List<VerifyJupiterRule.ResultVerifier> original,
            String staticReplacement) {
        for (VerifyJupiterRule.ResultVerifier old : original) {
            String newDisplayRgx = applyStatic(
                    old.displayNameRgx, staticReplacement);
            String newLegacyRgx = applyStatic(
                    old.legacyNameRgx, staticReplacement);
            String messageRgx = old.getMessageRgx();
            if (null == messageRgx) {
                builder.pass(newDisplayRgx, newLegacyRgx);
            } else {
                builder.fail(newDisplayRgx, newLegacyRgx)
                        .withMessage(messageRgx);
            }
        }
    }

    private void afterAllTweaksOn(VerifyJupiterRule builder,
            List<VerifyJupiterRule.ResultVerifier> original) {
        String classDisplayRgx = builder.getTestClass().getSimpleName();
        String classLegacyRgx = builder.getTestClass().getName();
        final List<?> pickables = Stream
                .of(pickables().orElse(new Object[] {""}))
                .collect(Collectors.toList());
        if (original.isEmpty()) {
            if (moreThanOneOption()) {
                for (Object pickOption : pickables) {
                    String staticAppend = " " + AFTER + "=" + pickOption;
                    builder.fail(
                            classDisplayRgx + staticAppend,
                            classLegacyRgx + staticAppend)
                            .withMessage(".*" + pickOption + ".*");
                }
            }

        } else if (original.get(0).displayNameRgx.contains(staticPlace)) {
            String leafReplacement = "";
            for (Object pickOption : pickables) {
                replaceStaticPlace(builder, original, leafReplacement);
                if (moreThanOneOption()) {
                    String pickSuffix = " " + AFTER + "=" + pickOption;
                    String newClassDisplayRgx = classDisplayRgx + pickSuffix;
                    String newClassLegacyRgx = classLegacyRgx + pickSuffix;
                    if (isFailurePick.matches(pickOption)) {
                        builder.fail(newClassDisplayRgx, newClassLegacyRgx)
                                .withMessage(".*" + pickOption + ".*");
                    } else {
                        builder.pass(newClassDisplayRgx, newClassLegacyRgx);
                    }
                    leafReplacement = "( /){0,2}";

                } else if (isFailurePick.matches(pickOption)) {
                    builder.fail(classDisplayRgx, classLegacyRgx);
                }
            }

        } else if (1 == original.size() && false == moreThanOneOption()
                && original.get(0).legacyNameRgx.startsWith(classLegacyRgx)) {
            builder.fail(classDisplayRgx, classLegacyRgx);

        } else if (NOTHING == this) {
            builder.getExpectations().addAll(original);

        } else if (DISPLAY_XyZ == this) {
            for (VerifyJupiterRule.ResultVerifier verifier : original) {
                if (false == verifier.legacyNameRgx.startsWith(classLegacyRgx)) {
                    builder.getExpectations().add(verifier);
                } else {
                    String newDisplayRgx = verifier.displayNameRgx + XyZ;
                    String newLegacyRgx = verifier.legacyNameRgx + XyZ;
                    if (null != verifier.getMessageRgx()) {
                        builder.fail(newDisplayRgx, newLegacyRgx)
                                .withMessage(verifier.getMessageRgx());
                    } else {
                        builder.pass(newDisplayRgx, newLegacyRgx);
                    }
                }
            }

        } else {
            Supplier<?>[] afterValueFeeds = IntStream.range(0, pickables.size())
                    .<Supplier<?>>mapToObj(firstIndex -> new Supplier<Object>() {
                int i = firstIndex;
                final int mod = pickables.size();

                @Override public Object get() {
                    return pickables.get(i++ % mod);
                }
            }).toArray(Supplier[]::new);
            for (Supplier<?> afterFeed : afterValueFeeds) {
                for (VerifyJupiterRule.ResultVerifier verifier : original) {

                    if (false == verifier.legacyNameRgx.startsWith(classLegacyRgx)) {
                        builder.getExpectations().add(verifier);

                    } else {
                        Object afterPick = afterFeed.get();
                        String newDisplayRgx = "".equals(afterPick)
                                ? verifier.displayNameRgx
                                : verifier.displayNameRgx + " " + AFTER
                                + "=" + afterPick;
                        String newLegacyRgx = "".equals(afterPick)
                                ? verifier.legacyNameRgx
                                : verifier.legacyNameRgx + " " + AFTER
                                + "=" + afterPick;

                        if (null != verifier.getMessageRgx()) {
                            /* Message from beforeAll prevails ... */
                            builder.fail(newDisplayRgx, newLegacyRgx)
                                    .withMessage(verifier.getMessageRgx());

                        } else if (isFailurePick.matches(afterPick)) {
                            builder.fail(newDisplayRgx, newLegacyRgx)
                                    .withMessage("(?s).+" + afterPick + ".+");

                        } else {
                            builder.pass(newDisplayRgx, newLegacyRgx);
                        }
                    }
                }
            }
        }
    }
}
