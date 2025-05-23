/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.jexl3.internal.introspection;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.jexl3.JexlException;
/**
 * Specialized executor to get a boolean property from an object.
 * @since 2.0
 */
public final class BooleanGetExecutor extends AbstractExecutor.Get {
    /**
     * Discovers a BooleanGetExecutor.
     * <p>The method to be found should be named "is{P,p}property and return a boolean.</p>
     *
     * @param is the introspector
     * @param clazz the class to find the get method from
     * @param property the property name
     * @return the executor if found, null otherwise
     */
    public static BooleanGetExecutor discover(final Introspector is, final Class<?> clazz, final String property) {
        if (property != null && !property.isEmpty()) {
            final java.lang.reflect.Method m = PropertyGetExecutor.discoverGet(is, "is", clazz, property);
            if (m != null && (m.getReturnType() == Boolean.TYPE || m.getReturnType() == Boolean.class)) {
                return new BooleanGetExecutor(clazz, m, property);
            }
        }
        return null;
    }

    /** The property. */
    private final String property;

    /**
     * Creates an instance by attempting discovery of the get method.
     * @param clazz the class to introspect
     * @param method the method held by this executor
     * @param key the property to get
     */
    private BooleanGetExecutor(final Class<?> clazz, final java.lang.reflect.Method method, final String key) {
        super(clazz, method);
        property = key;
    }

    @Override
    public Object getTargetProperty() {
        return property;
    }

    @Override
    public Object invoke(final Object obj) throws IllegalAccessException, InvocationTargetException {
        return method == null ? null : method.invoke(obj, (Object[]) null);
    }

    @Override
    public Object tryInvoke(final Object obj, final Object key) {
        if (obj != null && method !=  null
            // ensure method name matches the property name
            && property.equals(key)
            && objectClass.equals(obj.getClass())) {
            try {
                return method.invoke(obj, (Object[]) null);
            } catch (final IllegalAccessException xill) {
                return TRY_FAILED; // fail
            } catch (final InvocationTargetException xinvoke) {
                throw JexlException.tryFailed(xinvoke); // throw
            }
        }
        return TRY_FAILED;
    }
}