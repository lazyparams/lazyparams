/*
 * Copyright 2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.lazyparams.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.platform.engine.support.hierarchical.EngineExecutionContext;
import org.junit.platform.engine.support.hierarchical.Node;

/**
 * Utility that is used by {@link ProvideJunitPlatformHierarchical.DescriptorContextGuard}
 * to preserve node test-descriptor state before invocation of
 * {@link Node#after(EngineExecutionContext)} and
 * {@link Node#cleanUp(EngineExecutionContext)}, so that state can be restored by
 * {@link ProvideJunitPlatformHierarchical.DescriptorContextGuard#prepare(EngineExecutionContext)}
 * later, in case the test-descriptor execution is repeated.
 * State-restoration will be on a best-effort basis. That is if some internal
 * field or collection cannot be restored then restoration will be skipped for
 * the item of concern, while restoration continues for other internal state.
 *
 * @author Henrik Kaipe
 */
class InternalState<T> {
    private final List<Field> restorableFields = new ArrayList<Field>();
    private final List<Object> restorableFieldData = new ArrayList<Object>();
    private final List<Collection> restorableCollections = new ArrayList<Collection>();
    private final List<Object[]> restorableCollectionData = new ArrayList<Object[]>();

    void restoreOn(T restored) {
        for (int i = restorableFields.size(); 0 <= --i;) {
            try {
                restorableFields.get(i).set(restored, restorableFieldData.get(i));
            } catch (Exception dontRestoreThisField) {}
        }
        for (int i = restorableCollections.size(); 0 <= --i;) {
            Collection collection2restore = restorableCollections.get(i);
            if (false == collection2restore.isEmpty()) {
                try {
                    Collections.addAll(collection2restore, restorableCollectionData.get(i));
                } catch (Exception ignoreOnCollectionThatSeemsToBeReadonly) {}
            }
        }
    }

    static <T> InternalState<T> of(T preserved) {
        InternalState<T> preservedState = new InternalState<T>();
        for (Class<?> c = preserved.getClass();
                Object.class != c; c = c.getSuperclass()) {
            preservedState.storeInternalState(c, preserved);
        }
        return preservedState;
    }

    private void storeInternalState(Class<?> c, T preserved) {
        for (Field field : c.getDeclaredFields()) {
            Object fieldData = null;
            try {
                field.setAccessible(true);
                fieldData = field.get(preserved);
            } catch (Exception dontRestoreThisField) {}
            if (null == fieldData) {
                continue;
            }
            if (false == Modifier.isFinal(field.getModifiers())) {
                restorableFields.add(field);
                restorableFieldData.add(fieldData);
            }
            if (fieldData instanceof Collection) {
                Collection collection = (Collection) fieldData;
                if (false == collection.isEmpty()) {
                    restorableCollections.add(collection);
                    restorableCollectionData.add(collection.toArray());
                }
            }
        }
    }
}
