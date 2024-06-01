/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3.internal;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.internal.introspection.ClassMisc;

/**
 * Helper class to create typed arrays.
 */
public class ArrayBuilder implements JexlArithmetic.ArrayBuilder {
    /** The number of primitive types. */
    private static final int PRIMITIVE_SIZE = 8;
    /** The boxing types to primitive conversion map. */
    private static final Map<Class<?>, Class<?>> BOXING_CLASSES;
    static {
        BOXING_CLASSES = new IdentityHashMap<>(PRIMITIVE_SIZE);
        BOXING_CLASSES.put(Boolean.class, Boolean.TYPE);
        BOXING_CLASSES.put(Byte.class, Byte.TYPE);
        BOXING_CLASSES.put(Character.class, Character.TYPE);
        BOXING_CLASSES.put(Double.class, Double.TYPE);
        BOXING_CLASSES.put(Float.class, Float.TYPE);
        BOXING_CLASSES.put(Integer.class, Integer.TYPE);
        BOXING_CLASSES.put(Long.class, Long.TYPE);
        BOXING_CLASSES.put(Short.class, Short.TYPE);
    }

    /**
     * Gets the primitive type of given class (when it exists).
     * @param parm a class
     * @return the primitive type or null it the argument is not unboxable
     */
    protected static Class<?> unboxingClass(final Class<?> parm) {
        return BOXING_CLASSES.getOrDefault(parm, parm);
    }

    /** The intended class array. */
    protected Class<?> commonClass;
    /** Whether the array stores numbers. */
    protected boolean isNumber = true;
    /** Whether we can try unboxing. */
    protected boolean unboxing = true;
    /** The untyped list of items being added. */
    protected final Object[] untyped;
    /** Number of added items. */
    protected int added;
    /** Extended? */
    protected final boolean extended;

    /**
     * Creates a new builder.
     * @param size the exact array size
     */
    public ArrayBuilder(final int size) {
        this(size, false);
    }

    /**
     * Creates a new builder.
     * @param size the exact array size
     * @param extended whether the array is extended
     */
    public ArrayBuilder(final int size, final boolean extended) {
        this.untyped = new Object[size];
        this.extended = extended;
    }
    @Override
    public void add(final Object value) {
        // for all children after first...
        if (!Object.class.equals(commonClass)) {
            if (value == null) {
                isNumber = false;
                unboxing = false;
            } else {
                Class<?> eclass = value.getClass();
                // base common class on first non-null entry
                if (commonClass == null) {
                    commonClass = eclass;
                    isNumber = isNumber && Number.class.isAssignableFrom(commonClass);
                } else if (!commonClass.isAssignableFrom(eclass)) {
                    // if both are numbers...
                    if (isNumber && Number.class.isAssignableFrom(eclass)) {
                        commonClass = Number.class;
                    } else {
                        isNumber = false;
                        commonClass = getCommonSuperClass(commonClass, eclass);
                    }
                }
            }
        }
        if (added >= untyped.length) {
            throw new IllegalArgumentException("add() over size");
        }
        untyped[added++] = value;
    }

    @Override
    public Object create(final boolean e) {
        if (untyped == null) {
            return new Object[0];
        }
        final int size = added;
        if (extended || e) {
            final List<Object> list = newList(commonClass, size);
            list.addAll(Arrays.asList(untyped).subList(0, size));
            return list;
        }
        // convert untyped array to the common class if not Object.class
        if (commonClass == null || Object.class.equals(commonClass)) {
            return untyped.clone();
        }
        // if the commonClass is a number, it has an equivalent primitive type, get it
        if (unboxing) {
            commonClass = unboxingClass(commonClass);
        }
        // allocate and fill up the typed array
        final Object typed = Array.newInstance(commonClass, size);
        for (int i = 0; i < size; ++i) {
            Array.set(typed, i, untyped[i]);
        }
        return typed;
    }

    /**
     * Computes the best super class/super interface.
     * <p>Used to try and maintain type safe arrays.</p>
     * @param baseClass the baseClass
     * @param other another class
     * @return a common ancestor, class or interface, worst case being class Object
     */
    protected Class<?> getCommonSuperClass(final Class<?> baseClass, final Class<?> other) {
        return ClassMisc.getCommonSuperClass(baseClass, other);
    }

    /**
     * Creates a new list (aka extended array)/
     * @param clazz the class
     * @param size the size
     * @return the instance
     * @param <T> the type
     */
    protected <T> List<T> newList(Class<? extends T> clazz, int size) {
        return new ArrayList<>(size);
    }
}
