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
package org.apache.commons.jexl3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

/**
 * Test cases for calling antish variables as method names (JEXL-240);
 * Also tests that a class instance is a functor that invokes the constructor when called.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
class AntishCallTest extends JexlTestCase {

    /**
     * An arithmetic that considers class objects as callable.
     */
    public class CallSupportArithmetic extends JexlArithmetic {
        public CallSupportArithmetic(final boolean strict) {
            super(strict);
        }

        public Object call(final Class<?> clazz, final Object... args) {
            return callConstructor(null, clazz, args);
        }

        public Object call(final ClassReference clazz, final Object... args) {
            return callConstructor(null, clazz, args);
        }
    }

    /**
     * A context that considers class references as callable.
     */
    public static class CallSupportContext extends MapContext {
        private JexlEngine engine;
        CallSupportContext(final Map<String, Object> map) {
            super(map);
        }

        public Object call(final Class<?> clazz, final Object... args) {
            return callConstructor(engine, clazz, args);
        }

        public Object call(final ClassReference clazz, final Object... args) {
            return callConstructor(engine, clazz, args);
        }

        CallSupportContext engine(final JexlEngine j) {
            engine = j;
            return this;
        }

        @Override public Object get(final String str) {
            if (!super.has(str)) {
                try {
                    return CallSupportContext.class.getClassLoader().loadClass(str);
                } catch (final Exception xany) {
                    return null;
                }
            }
            return super.get(str);
        }

        @Override public boolean has(final String str) {
            if (!super.has(str)){
                try {
                    return CallSupportContext.class.getClassLoader().loadClass(str) != null;
                } catch (final Exception xany) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Wraps a class.
     */
    public static class ClassReference {
        final Class<?> clazz;
        ClassReference(final Class<?> c) {
            this.clazz = c;
        }
    }
    public static Object callConstructor(final JexlEngine engine, final Class<?> clazz, final Object... args) {
        if (clazz == null || clazz.isPrimitive() || clazz.isInterface()
            || clazz.isMemberClass() || clazz.isAnnotation() || clazz.isArray()) {
            throw new ArithmeticException("not a constructible object");
        }
        JexlEngine jexl = engine;
        if (jexl == null) {
            jexl = JexlEngine.getThreadEngine();
            if (jexl == null) {
                throw new ArithmeticException("no engine to solve constructor");
            }
        }
        return jexl.newInstance(clazz, args);
    }

    /**
     * Considers any call using a class reference as functor as a call to its constructor.
     * <p>Note that before 3.2, a class was not considered a functor.
     * @param ref the ClassReference of the class we seek to instantiate
     * @param args the constructor arguments
     * @return an instance if that was possible
     */
    public static Object callConstructor(final JexlEngine engine, final ClassReference ref, final Object... args) {
        return callConstructor(engine, ref.clazz, args);
    }

    public AntishCallTest() {
        super("AntishCallTest");
    }

    void runTestCall(final JexlEngine jexl, final JexlContext jc) throws Exception {
        final JexlScript check1 = jexl.createScript("var x = java.math.BigInteger; x('1234')");
        final JexlScript check2 = jexl.createScript("java.math.BigInteger('4321')");

        final Object o1 = check1.execute(jc);
        assertEquals(new java.math.BigInteger("1234"), o1, "Result is not 1234");

        final Object o2 = check2.execute(jc);
        assertEquals(new java.math.BigInteger("4321"), o2, "Result is not 4321");
    }

    @Test
    void testAntishArithmetic() throws Exception {
        final CallSupportArithmetic ja = new CallSupportArithmetic(true);
        final JexlEngine jexl = new JexlBuilder().cache(512).arithmetic(ja).create();
        final Map<String, Object> lmap = new TreeMap<>();
        final JexlContext jc = new MapContext(lmap);
        lmap.put("java.math.BigInteger", java.math.BigInteger.class);
        runTestCall(jexl, jc);
        lmap.put("java.math.BigInteger", new ClassReference(BigInteger.class));
        runTestCall(jexl, jc);
        lmap.remove("java.math.BigInteger");
        assertThrows(JexlException.class, () -> runTestCall(jexl, jc));
    }

    @Test
    void testAntishContextVar() throws Exception {
        final Map<String,Object> lmap = new TreeMap<>();
        final JexlContext jc = new CallSupportContext(lmap).engine(JEXL);
        runTestCall(JEXL, jc);
        lmap.put("java.math.BigInteger", new ClassReference(BigInteger.class));
        runTestCall(JEXL, jc);
        lmap.remove("java.math.BigInteger");
        runTestCall(JEXL, jc);
    }

    // JEXL-300
    @Test
    void testSafeAnt() throws Exception {
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        ctxt.set("x.y.z", 42);
        JexlScript script;
        Object result;

        final JexlScript script0 = JEXL.createScript("x.y.z");
        result = script0.execute(ctxt);
        assertEquals(42, result);
        assertEquals(42, ctxt.get("x.y.z"));

        options.setAntish(false);
        assertThrows(JexlException.class, () -> script0.execute(ctxt), "antish var shall not be resolved");
        options.setAntish(true);

        script = JEXL.createScript("x?.y?.z");
        result = script.execute(ctxt);
        assertNull(result); // safe navigation, null

        final JexlScript script1 = JEXL.createScript("x?.y?.z = 3");
        assertThrows(JexlException.class, () -> script1.execute(ctxt), "not antish assign");

        final JexlScript script2 = JEXL.createScript("x.y?.z");
        assertThrows(JexlException.class, () -> script2.execute(ctxt), "x not defined");

        final JexlScript script3 = JEXL.createScript("x.y?.z = 3");
        assertThrows(JexlException.class, () -> script3.execute(ctxt), "x not defined");

        final JexlScript script4 = JEXL.createScript("x.`'y'`.z = 3");
        assertThrows(JexlException.class, () -> script4.execute(ctxt), "x not defined");
    }

}