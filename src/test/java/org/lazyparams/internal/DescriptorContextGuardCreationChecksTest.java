/*
 * Copyright 2024-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.internal;

import java.util.function.UnaryOperator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.platform.engine.TestDescriptor;
import org.lazyparams.VerifyVintageRule;

/**
 * @author Henrik Kaipe
 */
public class DescriptorContextGuardCreationChecksTest {

    @Rule
    public VerifyVintageRule expect = new VerifyVintageRule(DescriptorContextGuardCreationChecks.class) {
        String expandLocalShortHandConvention(String nameRgxRaw) {
            if (null == nameRgxRaw || nameRgxRaw.length() <= 1
                    || ' ' != nameRgxRaw.charAt(1)) {
                return nameRgxRaw;
            }
            return new StringBuilder()
                    .append(" 0\\.\\d{3}s")
                    .append(" initialized=")
                    .append(nameRgxRaw.startsWith("f ") ? "false "
                            : nameRgxRaw.startsWith("t ") ? "true "
                                    : "".substring(99))
                    .append(nameRgxRaw.substring(2))
                    .toString();
        }
        @Override
        public VerifyVintageRule.SpecifyFailMessageOrNextResult fail(String nameRgx) {
            return super.fail(expandLocalShortHandConvention(nameRgx));
        }
        @Override
        public VerifyVintageRule.NextResult pass(String nameRgx) {
            return super.pass(expandLocalShortHandConvention(nameRgx));
        }
    };

    @Test
    public void needIdentityEqualityAndHashCode() {
        expect.pass(" target=TestDescriptor initialized=false")
                .pass(" target=DescriptorNode initialized=true")
                .pass(" target=TestDescriptor initialized=true")
                .pass(" target=DescriptorNode initialized=false")
                .pass("");
    }

    @Test
    public void pickMethodsSorted() {
        expect.fail(" exec#1 BY_TO_DISPLAY").withMessage(".*xpected.*BY_TO_STRING.*was.*BY_TO_DISPLAY.*")
                .pass(" exec#2")
                .fail(" exec#3 BY_NAME_KEY").withMessage(".*xpected.*BY_TO_STRING.*was.*BY_NAME_KEY.*")
                .fail("").withMessage(".*2.*fail.*total 3.*");
    }

    @Test
    public void topOfNode() {
        try {
            TestDescriptor.class.getMethod("getAncestors");
        } catch (NoSuchMethodException junit_platform_1_9_or_earlier) {
            expect.pass("f impl DescriptorContextGuard::equals")
                    .pass("t proxy method=findByUniqueId")
                    .pass("t proxy name=addChild")
                    .pass("f proxy Node::after")
                    .pass("f impl method=isTest")
                    .pass("f proxy Node::around")
                    .pass("f impl name=getDisplayName")
                    .pass("f proxy method=removeChild")
                    .pass("f impl name=getExclusiveResources")
                    .pass("f proxy method=removeFromHierarchy")
                    .pass("f impl name=getExecutionMode")
                    .pass("f proxy Node::execute")
                    .pass("f impl method=getLegacyReportingName")
                    .pass("f proxy name=findByUniqueId")
                    .fail("f impl BY_TO_DISPLAY DescriptorContextGuard::getParent")
                    .pass("f proxy name=getDescendants")
                    .fail("f impl BY_TO_STRING method=getParent")
                    .pass("f proxy TestDescriptor::getTags")
                    .pass("f impl method=getSource")
                    .pass("f proxy name=getType returns=CONTAINER")
                    .pass("f proxy TestDescriptor::isContainer")
                    .pass("f proxy name=isRoot")
                    .pass("f impl method=getChildren")
                    .pass("t impl name=isTest")
                    .pass("t proxy method=after")
                    .pass("t proxy name=nodeSkipped")
                    .pass("t impl method=getUniqueId")
                    .pass("t proxy name=prune")
                    .fail("t impl BY_TO_STRING method=prepare")
                    .pass("t proxy name=removeChild")
                    .pass("t impl method=getExecutionMode")
                    .pass("t proxy name=removeFromHierarchy")
                    .pass("t impl method=shouldBeSkipped")
                    .pass("t proxy name=getType returns=TEST")
                    .fail("t impl BY_TO_STRING method=of")
                    .pass("t proxy name=getType returns=CONTAINER_AND_TEST")
                    .pass("t impl method=setParent")
                    .pass("t proxy name=after")
                    .pass("t proxy method=addChild")
                    .pass("t proxy name=before")
                    .pass("t impl method=equals")
                    .pass("t proxy name=cleanUp")
                    .pass("f impl DescriptorContextGuard::isTest")
                    .pass("t proxy method=isContainer")
                    .pass("f impl DescriptorContextGuard::mayRegisterTests")
                    .pass("t proxy name=getTags")
                    .pass("f proxy TestDescriptor::accept")
                    .pass("t impl method=mayRegisterTests")
                    .fail("f impl BY_TO_DISPLAY DescriptorContextGuard::of")
                    .pass("t proxy name=isContainer")
                    .pass("f proxy TestDescriptor::addChild")
                    .pass("t impl method=hashCode")
                    .fail("f impl BY_TO_DISPLAY DescriptorContextGuard::prepare")
                    .pass("t proxy name=findByUniqueId")
                    .pass("f proxy Node::nodeFinished")
                    .pass("t impl method=getDisplayName")
                    .pass("f impl DescriptorContextGuard::setParent")
                    .pass("t proxy name=getDescendants")
                    .pass("f proxy Node::nodeSkipped")
                    .pass("t impl method=toString")
                    .pass("f impl DescriptorContextGuard::shouldBeSkipped")
                    .pass("t proxy name=isRoot")
                    .pass("f proxy TestDescriptor::prune")
                    .pass("t impl method=isTest")
                    .pass("f impl DescriptorContextGuard::toString")
                    .pass("t proxy name=getType returns=CONTAINER")
                    .pass("f proxy TestDescriptor::removeChild")
                    .pass("t impl method=getLegacyReportingName")
                    .pass("f impl DescriptorContextGuard::getSource")
                    .pass("t impl name=getUniqueId")
                    .pass("f proxy TestDescriptor::removeFromHierarchy")
                    .pass("t impl method=getChildren")
                    .pass("f proxy TestDescriptor::getType returns=TEST")
                    .pass("t impl name=getSource")
                    .pass("f proxy TestDescriptor::getType returns=CONTAINER_AND_TEST")
                    .fail("")
                    .withMessage(".*6 .*fail.*total 75.*");
            return;
        }
        try {
            TestDescriptor.class.getMethod("orderChildren",UnaryOperator.class);
        } catch (NoSuchMethodException junit_platform_1_10_or_1_11) {
            expect
                    .pass("f impl DescriptorContextGuard::equals")
                    .pass("t proxy method=findByUniqueId")
                    .pass("t proxy name=addChild")
                    .pass("t proxy Node::after")
                    .pass("f proxy Node::around")
                    .pass("f impl method=isTest")
                    .pass("f proxy method=removeChild")
                    .pass("f impl name=getDisplayName")
                    .pass("f proxy name=cleanUp")
                    .pass("f impl DescriptorContextGuard::getExclusiveResources")
                    .pass("f proxy method=isContainer")
                    .pass("f impl name=getExecutionMode")
                    .pass("f proxy TestDescriptor::findByUniqueId")
                    .pass("f impl method=getLegacyReportingName")
                    .pass("f proxy name=getAncestors")
                    .fail("f impl BY_TO_DISPLAY DescriptorContextGuard::getParent")
                    .pass("f proxy method=getDescendants")
                    .pass("f impl name=getSource")
                    .pass("f proxy TestDescriptor::getTags")
                    .pass("f impl method=getSource")
                    .pass("f proxy name=getType returns=CONTAINER")
                    .pass("f proxy name=isContainer")
                    .pass("f proxy TestDescriptor::isRoot")
                    .pass("f impl method=getChildren")
                    .pass("f proxy name=nodeFinished")
                    .pass("t impl DescriptorContextGuard::isTest")
                    .pass("t proxy method=around")
                    .pass("t impl DescriptorContextGuard::mayRegisterTests")
                    .pass("t proxy name=prune")
                    .fail("t impl BY_TO_STRING method=prepare")
                    .pass("t proxy TestDescriptor::removeChild")
                    .pass("t impl method=getExecutionMode")
                    .pass("t proxy name=removeFromHierarchy")
                    .pass("t impl DescriptorContextGuard::setParent")
                    .pass("t proxy method=execute")
                    .pass("t proxy name=getType returns=TEST")
                    .pass("t impl DescriptorContextGuard::shouldBeSkipped")
                    .pass("t proxy method=addChild")
                    .pass("t proxy name=getType returns=CONTAINER_AND_TEST")
                    .pass("t impl method=setParent")
                    .pass("t proxy Node::before")
                    .pass("t proxy name=execute")
                    .pass("t proxy Node::cleanUp")
                    .pass("t impl method=equals")
                    .pass("t proxy name=findByUniqueId")
                    .fail("f impl BY_TO_DISPLAY DescriptorContextGuard::of")
                    .pass("t proxy method=getAncestors")
                    .pass("f impl name=isTest")
                    .pass("t proxy TestDescriptor::getDescendants")
                    .pass("f impl method=getUniqueId")
                    .pass("t proxy name=getTags")
                    .pass("f proxy method=getTags")
                    .pass("t impl DescriptorContextGuard::getChildren")
                    .fail("f impl BY_NAME_KEY name=prepare")
                    .pass("t proxy method=accept")
                    .pass("f proxy TestDescriptor::accept")
                    .pass("t impl name=hashCode")
                    .pass("f impl name=setParent")
                    .pass("t proxy Node::nodeFinished")
                    .pass("f proxy method=getType returns=TEST")
                    .pass("t impl method=mayRegisterTests")
                    .pass("t proxy name=after")
                    .pass("t proxy TestDescriptor::isRoot")
                    .fail("f impl BY_TO_STRING method=of")
                    .pass("t proxy name=getType returns=CONTAINER")
                    .pass("f proxy Node::after")
                    .pass("f impl method=setParent")
                    .pass("t proxy name=nodeSkipped")
                    .pass("t proxy TestDescriptor::getType returns=CONTAINER_AND_TEST")
                    .pass("t proxy method=getType returns=CONTAINER_AND_TEST")
                    .pass("t impl name=getExclusiveResources")
                    .pass("f proxy TestDescriptor::prune")
                    .pass("t impl method=getDisplayName")
                    .pass("f proxy name=after")
                    .pass("f proxy TestDescriptor::removeFromHierarchy")
                    .pass("t impl method=toString")
                    .pass("f proxy name=removeChild")
                    .pass("t impl DescriptorContextGuard::getLegacyReportingName")
                    .pass("f proxy method=getType returns=CONTAINER_AND_TEST")
                    .pass("t impl name=getUniqueId")
                    .pass("f proxy Node::nodeSkipped")
                    .fail("t impl BY_TO_STRING method=getParent")
                    .pass("f proxy method=getType returns=CONTAINER")
                    .fail("")
                       .withMessage(".*6 .*fail.*total 83.*");
            return;
        }
        expect
                .pass("f impl DescriptorContextGuard::equals")
                .pass("t proxy method=findByUniqueId")
                .pass("t proxy name=addChild")
                .pass("t proxy Node::after")
                .pass("t proxy method=addChild")
                .pass("f proxy Node::before")
                .pass("f impl name=getChildren")
                .pass("f proxy name=cleanUp")
                .pass("f impl method=mayRegisterTests")
                .pass("f proxy Node::execute")
                .pass("f impl name=getExclusiveResources")
                .pass("f proxy method=isRoot")
                .pass("f impl method=getDisplayName")
                .pass("f proxy name=getAncestors")
                .pass("f impl DescriptorContextGuard::getLegacyReportingName")
                .pass("f proxy method=getDescendants")
                .fail("f impl BY_TO_DISPLAY DescriptorContextGuard::getParent")
                .pass("f proxy name=getTags")
                .fail("f impl BY_TO_STRING method=getParent")
                .pass("f proxy TestDescriptor::getType returns=CONTAINER")
                .pass("f proxy name=isContainer")
                .pass("f proxy TestDescriptor::isRoot")
                .pass("f impl name=getUniqueId")
                .pass("f proxy method=prune")
                .pass("f impl DescriptorContextGuard::hashCode")
                .pass("f proxy method=after")
                .pass("f impl name=isTest")
                .pass("t impl DescriptorContextGuard::mayRegisterTests")
                .pass("t proxy name=orderChildren")
                .pass("t proxy method=cleanUp")
                .fail("t impl BY_TO_DISPLAY DescriptorContextGuard::of")
                .pass("t proxy name=removeChild")
                .pass("t impl method=getExecutionMode")
                .pass("t proxy TestDescriptor::removeFromHierarchy")
                .pass("t impl method=shouldBeSkipped")
                .pass("t proxy name=getType returns=TEST")
                .pass("t impl DescriptorContextGuard::shouldBeSkipped")
                .pass("t proxy method=execute")
                .pass("t proxy name=getType returns=CONTAINER_AND_TEST")
                .pass("t impl DescriptorContextGuard::toString")
                .pass("t proxy method=removeChild")
                .pass("t proxy name=execute")
                .pass("t proxy Node::cleanUp")
                .pass("t proxy method=getAncestors")
                .pass("t proxy name=findByUniqueId")
                .pass("t impl DescriptorContextGuard::getChildren")
                .pass("t proxy method=before")
                .pass("f impl name=mayRegisterTests")
                .pass("t proxy TestDescriptor::getDescendants")
                .fail("f impl BY_TO_STRING method=prepare")
                .pass("t proxy name=isRoot")
                .pass("f proxy TestDescriptor::accept")
                .pass("t impl method=equals")
                .fail("f impl BY_NAME_KEY name=prepare")
                .pass("t proxy TestDescriptor::isContainer")
                .pass("f proxy method=getTags")
                .pass("t impl name=hashCode")
                .pass("f impl DescriptorContextGuard::setParent")
                .pass("t proxy name=nodeFinished")
                .pass("f proxy method=getType returns=TEST")
                .pass("t impl method=hashCode")
                .pass("t proxy Node::nodeSkipped")
                .pass("f impl name=shouldBeSkipped")
                .pass("t proxy method=getType returns=CONTAINER")
                .pass("f impl DescriptorContextGuard::toString")
                .pass("t impl name=getDisplayName")
                .pass("f proxy method=getType returns=CONTAINER_AND_TEST")
                .pass("t impl name=getExecutionMode")
                .pass("f proxy Node::around")
                .pass("t impl method=getLegacyReportingName")
                .fail("f impl BY_NAME_KEY name=getParent")
                .pass("f proxy TestDescriptor::orderChildren")
                .pass("t impl method=getSource")
                .pass("f impl name=getSource")
                .pass("f proxy TestDescriptor::prune")
                .pass("t impl method=getExclusiveResources")
                .pass("f impl DescriptorContextGuard::getDisplayName")
                .fail("t impl BY_NAME_KEY name=getParent")
                .pass("f proxy method=nodeFinished")
                .pass("t impl DescriptorContextGuard::getSource")
                .pass("f proxy name=removeFromHierarchy")
                .fail("")
                .withMessage(".*7 .*fail.*total 81.*");
    }
}
