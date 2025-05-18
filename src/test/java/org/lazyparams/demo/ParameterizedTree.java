/*
 * Copyright 2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.demo;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lazyparams.LazyParamsCoreUtil;

/**
 * An attempt to achieve parameterization of hierarchical tree strutures.
 * LazyParams internal test-suites takes advantage of this class when
 * verifying support for JUnit platform hierarchical engine features.
 *
 * @see org.lazyparams.internal.HierarchicalLifecycleTest
 * @author Henrik Kaipe
 */
public abstract class ParameterizedTree<N> {

    private final char baseCaption;
    private final N[] nodes;

    private boolean verbose = false, quiet = false;

    public ParameterizedTree(char baseCaption, N... nodes) {
        this.baseCaption = baseCaption;
        this.nodes = nodes.clone();
    }

    private Object paramId(final int count) {
        return new Supplier<Class<? extends ParameterizedTree>>() {
            @Override public boolean equals(Object other) {
                return this == other
                        || other instanceof Supplier
                        && this.get() == ((Supplier<?>)other).get()
                        && this.hashCode() == other.hashCode();
            }
            @Override public int hashCode() {
                return 731 * count + get().hashCode();
            }
            @Override public Class<? extends ParameterizedTree> get() {
                return ParameterizedTree.this.getClass();
            }
        };
    }

    protected boolean fallbackOnPreviousNode(Object paramId) {
        return 1 == LazyParamsCoreUtil.makePick(paramId, true, 2);
    }

    private boolean fallbackOnPreviousNode(int count) {
        return fallbackOnPreviousNode(paramId(count));
    }

    public ParameterizedTree<N> displayOnSuccess() {
        verbose = true;
        return this;
    }
    public ParameterizedTree<N> quietOnFailure() {
        quiet = true;
        return this;
    }

    private StringBuilder indentedChar(int indentLength, char endChar) {
        StringBuilder sb = new StringBuilder(indentLength + 10);
        while (0 <= --indentLength) {
            sb.append(' ');
        }
        return sb.append(endChar);
    }

    public List<StringBuilder> populate(Predicate<N> onRootCallback) {
        final List<StringBuilder> onDisplay = new ArrayList<>();
        final List<StringBuilder> onDisplayReversed = new AbstractList<StringBuilder>() {
            @Override public int size() { return onDisplay.size(); }
            @Override public StringBuilder get(int index) {
                return onDisplay.get(size() - 1 - index);
            }
        };
        int paramCount = 0;
        StringBuilder currentBranch = new StringBuilder("0 - " + baseCaption);
        onDisplay.add(currentBranch);
        ListIterator<N> nodeCrumbs = new ArrayList<N>().listIterator();
        Object __ = new Object();
        for (int nodeIndex = 0; ((nodeCrumbs.hasPrevious() && __!=nodeCrumbs.previous())
                ? connect(nodeCrumbs.next(), nodes[nodeIndex])
                : onRootCallback.test(nodes[nodeIndex])
                ) && ++nodeIndex < nodes.length;) {
            if (fallbackOnPreviousNode(++paramCount)) {
                int parentNodeIndex = currentBranch.length() - 5;
                while (nodeCrumbs.hasPrevious()
                        && fallbackOnPreviousNode(++paramCount)) {
                    nodeCrumbs.previous(); nodeCrumbs.remove();
                    parentNodeIndex -= 4;
                }
                for (StringBuilder completedBranch : onDisplayReversed) {
                    int lengthShortage = parentNodeIndex - completedBranch.length();
                    if (0 <= lengthShortage) {
                        completedBranch.append(indentedChar(lengthShortage,'|'));
                    } else if (' ' == completedBranch.charAt(parentNodeIndex)) {
                        completedBranch.setCharAt(parentNodeIndex, '|');
                    } else {
                        break;
                    }
                }
                onDisplay.add(indentedChar(parentNodeIndex + 2, '\\'));
                onDisplay.add(currentBranch = indentedChar(parentNodeIndex + 3, ' '));
            } else {
                nodeCrumbs.add(nodes[nodeIndex - 1]);
                currentBranch.append(" - ");
            }
            currentBranch.append((char)(baseCaption + nodeIndex));
        }

        StringBuilder displayApdx = mergeToOneLine(onDisplay);
        if (false == quiet) {
            LazyParamsCoreUtil.displayOnFailure(displayApdx,displayApdx);
        }
        if (verbose) {
            LazyParamsCoreUtil.displayOnSuccess(displayApdx,displayApdx);
        }
        return onDisplay;
    }

    public abstract boolean connect(N parent, N child);

    private StringBuilder mergeToOneLine(List<StringBuilder> onDisplay) {
        final Pattern displayLineRgx = Pattern.compile("^[\\| ]*+(.++)");
        Iterator<StringBuilder> displayLines = onDisplay.iterator();
        StringBuilder merged = new StringBuilder(displayLines.next());
        while (displayLines.hasNext()) {
            displayLines.next();
            Matcher m = displayLineRgx.matcher(displayLines.next());
            m.matches();
            merged.insert(m.start(1) - 3, " -()").insert(m.start(1), m.group(1));
        }
        return merged;
    }
}
