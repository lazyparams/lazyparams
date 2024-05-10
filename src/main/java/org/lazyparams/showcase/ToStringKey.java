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

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * @author Henrik Kaipe
 */
abstract class ToStringKey {
    private final String toString;
    private final Object[] extras;

    ToStringKey(String toString, Object... extras) {
        this.toString = toString;
        this.extras = extras.clone();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + (this.toString != null ? this.toString.hashCode() : 0);
        hash = 89 * hash + getClass().getName().hashCode();
        hash = 89 * hash + Arrays.hashCode(extras);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()
                && false == getClass().getName().equals(obj.getClass().getName())) {
            return false;
        }
        String otherToString = obj.toString();
        if ((this.toString == null) ? (otherToString != null)
                : false == this.toString.equals(otherToString)) {
            return false;
        }
        return Arrays.equals(this.extras, resolveExtrasOnOther(obj));
    }

    private Object[] resolveExtrasOnOther(Object obj) {
        if (obj instanceof ToStringKey) {
            return ((ToStringKey)obj).extras;
        } else {
            Field otherExtrasField = resolveExtrasFieldOnOther(obj.getClass());
            if (null == otherExtrasField) {
                return new Object[extras.length + 1];
            }
            otherExtrasField.setAccessible(true);
            try {
                return (Object[]) otherExtrasField.get(obj);
            } catch (Exception ex) {
                ex.printStackTrace();
                return new Object[extras.length + 1];
            }
        }
    }

    private static Field resolveExtrasFieldOnOther(Class<? extends Object> otherClass) {
        while (false == ToStringKey.class.getName().equals(otherClass.getName())) {
            otherClass = otherClass.getSuperclass();
            if (Object.class == otherClass) {
                return null;
            }
        }
        try {
            return otherClass.getDeclaredField("extras");
        } catch (NoSuchFieldException ex) {
            return null;
        }
    }

    @Override
    public String toString() {
        return toString;
    }
}
