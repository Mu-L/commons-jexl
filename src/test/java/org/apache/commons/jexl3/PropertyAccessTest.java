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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.jexl3.internal.Debugger;
import org.apache.commons.jexl3.internal.introspection.IndexedType;
import org.apache.commons.jexl3.internal.introspection.Uberspect;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.jexl3.junit.Asserter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for property access operator '.'
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
class PropertyAccessTest extends JexlTestCase {

    public static class Container extends PropertyContainer {
        public Container(final String name, final int number) {
            super(name, number);
        }

        public Object getProperty(final int ref) {
            switch (ref) {
                case 0:
                    return value0;
                case 1:
                    return value1;
                default:
                    return null;
            }
        }

        public void setProperty(final int ref, final int value) {
            if (1 == ref) {
                this.value1 = value;
            }
        }

        public void setProperty(final int ref, final String value) {
            if (0 == ref) {
                this.value0 = value;
            }
        }

        public void setProperty(final String name, final int value) {
            if ("number".equals(name)) {
                this.value1 = value;
            }
        }

        @Override
        public void setProperty(final String name, final String value) {
            if ("name".equals(name)) {
                this.value0 = value;
            }
        }
    }

    public static class Prompt {
        private final Map<String, PromptValue> values = new HashMap<>();

        public Object get(final String name) {
            final PromptValue v = values.get(name);
            return v != null ? v.getValue() : null;
        }

        public void set(final String name, final Object value) {
            values.put(name, new PromptValue(value));
        }
    }

    /**
     * A valued prompt.
     */
    public static class PromptValue {

        /** Prompt value. */
        private Object value;

        public PromptValue(final Object v) {
           value = v;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(final Object value) {
            this.value = value;
        }
    }

    /**
     * Overloads propertySet.
     */
    public static class PropertyArithmetic extends JexlArithmetic {
        int ncalls;

        public PropertyArithmetic(final boolean astrict) {
            super(astrict);
        }

        public int getCalls() {
            return ncalls;
        }

        public Object propertySet(final IndexedType.IndexedContainer map, final String key, final Integer value) {
            if (map.getContainerClass().equals(PropertyContainer.class)
                && map.getContainerName().equals("property")) {
                try {
                    map.set(key, value.toString());
                    ncalls += 1;
                } catch (final Exception xany) {
                    throw new JexlException.Operator(null, key + "." + value.toString(), xany);
                }
                return null;
            }
            return JexlEngine.TRY_FAILED;
        }
    }

    /**
     * A base property container; can only set from string.
     */
    public static class PropertyContainer {
        String value0;
        int value1;

        public PropertyContainer(final String name, final int number) {
            value0 = name;
            value1 = number;
        }

        public Object getProperty(final String name) {
            if ("name".equals(name)) {
                return value0;
            }
            if ("number".equals(name)) {
                return value1;
            }
            return null;
        }

        public void setProperty(final String name, final String value) {
            if ("name".equals(name)) {
                this.value0 = value.toUpperCase();
            }
            if ("number".equals(name)) {
                this.value1 = Integer.parseInt(value) + 1000;
            }
        }
    }

    private Asserter asserter;

    public PropertyAccessTest() {
        super("PropertyAccessTest");
    }

    @BeforeEach
    @Override
    public void setUp() {
        asserter = new Asserter(JEXL);
    }

    @Test
    void test250() throws Exception {
        final MapContext ctx = new MapContext();
        final HashMap<Object, Object> x = new HashMap<>();
        x.put(2, "123456789");
        ctx.set("x", x);
        // @formatter:off
        final JexlEngine engine = new JexlBuilder()
                .uberspect(new Uberspect(null, null, JexlPermissions.UNRESTRICTED))
                .strict(true).silent(false).create();
        // @formatter:on
        String stmt = "x.2.class.name";
        JexlScript script = engine.createScript(stmt);
        Object result = script.execute(ctx);
        assertEquals("java.lang.String", result);

        stmt = "x.3?.class.name";
        script = engine.createScript(stmt);
        result = script.execute(ctx);
        assertNull(result);

        stmt = "x?.3.class.name";
        final JexlScript script1 = engine.createScript(stmt);
        assertThrows(JexlException.class, () -> script1.execute(ctx));

        stmt = "x?.3?.class.name";
        script = engine.createScript(stmt);
        result = script.execute(ctx);
        assertNull(result);

        stmt = "y?.3.class.name";
        script = engine.createScript(stmt);
        result = script.execute(ctx);
        assertNull(result);

        stmt = "x?.y?.z";
        script = engine.createScript(stmt);
        result = script.execute(ctx);
        assertNull(result);

        stmt = "x? (x.y? (x.y.z ?: null) :null) : null";
        script = engine.createScript(stmt);
        result = script.execute(ctx);
        assertNull(result);
    }

    @Test
    void test275a() throws Exception {
        final JexlEngine jexl = new JexlBuilder().strict(true).safe(false).create();
        final JexlContext ctxt = new MapContext();
        Object result = null;
        final Prompt p0 = new Prompt();
        p0.set("stuff", 42);
        ctxt.set("$in", p0);

        // unprotected navigation
        final JexlScript script0 = jexl.createScript("$in[p].intValue()", "p");
        assertThrows(JexlException.Property.class, () -> script0.execute(ctxt, "fail"));

        assertNull(result);
        result = script0.execute(ctxt, "stuff");
        assertEquals(42, result);

        // protected navigation
        JexlScript script = jexl.createScript("$in[p]?.intValue()", "p");
        result = script.execute(ctxt, "fail");
        assertNull(result);
        result = script.execute(ctxt, "stuff");
        assertEquals(42, result);

        // unprotected navigation
        final JexlScript script1 = jexl.createScript("$in.`${p}`.intValue()", "p");
        assertThrows(JexlException.Property.class, () -> script1.execute(ctxt, "fail"));

        result = script.execute(ctxt, "stuff");
        assertEquals(42, result);

        // protected navigation
        script = jexl.createScript("$in.`${p}`?.intValue()", "p");
        result = script.execute(ctxt, "fail");
        assertNull(result);
        result = script.execute(ctxt, "stuff");
        assertEquals(42, result);

    }

    @Test
    void test275b() throws Exception {
        final JexlEngine jexl = new JexlBuilder().strict(true).safe(true).create();
        final JexlContext ctxt = new MapContext();
        JexlScript script;
        final Prompt p0 = new Prompt();
        p0.set("stuff", 42);
        ctxt.set("$in", p0);

        // unprotected navigation
        script = jexl.createScript("$in[p].intValue()", "p");
        Object result = script.execute(ctxt, "fail");
        assertNull(result);

        result = script.execute(ctxt, "stuff");
        assertEquals(42, result);

        // unprotected navigation
        script = jexl.createScript("$in.`${p}`.intValue()", "p");
        result = script.execute(ctxt, "fail");
        assertNull(result);
        result = script.execute(ctxt, "stuff");
        assertEquals(42, result);

        // protected navigation
        script = jexl.createScript("$in.`${p}`?.intValue()", "p");
        result = script.execute(ctxt, "fail");
        assertNull(result);
        result = script.execute(ctxt, "stuff");
        assertEquals(42, result);
    }

    @Test
    void testErroneousIdentifier() throws Exception {
        final MapContext ctx = new MapContext();
        final JexlEngine engine = new JexlBuilder().strict(true).silent(false).create();

        // base succeeds
        String stmt = "(x)->{ x?.class ?? 'oops' }";
        JexlScript script = engine.createScript(stmt);
        Object result = script.execute(ctx, "querty");
        assertEquals("querty".getClass(), result);

        // fail with unknown property
        stmt = "(x)->{ x.class1 ?? 'oops' }";
        script = engine.createScript(stmt);
        result = script.execute(ctx, "querty");
        assertEquals("oops", result);

        // succeeds with jxlt & strict navigation
        ctx.set("al", "la");
        stmt = "(x)->{ x.`c${al}ss` ?? 'oops' }";
        script = engine.createScript(stmt);
        result = script.execute(ctx, "querty");
        assertEquals("querty".getClass(), result);

        // succeeds with jxlt & lenient navigation
        stmt = "(x)->{ x?.`c${al}ss` ?? 'oops' }";
        script = engine.createScript(stmt);
        result = script.execute(ctx, "querty");
        assertEquals("querty".getClass(), result);

        // fails with jxlt & lenient navigation
        stmt = "(x)->{ x?.`c${la}ss` ?? 'oops' }";
        script = engine.createScript(stmt);
        result = script.execute(ctx, "querty");
        assertEquals("oops", result);

        // fails with jxlt & strict navigation
        stmt = "(x)->{ x.`c${la}ss` ?? 'oops' }";
        script = engine.createScript(stmt);
        result = script.execute(ctx, "querty");
        assertEquals("oops", result);

        // parsing fails with jxlt & lenient navigation
        stmt = "(x)->{ x?.`c${la--ss` ?? 'oops' }";
        try {
            script = engine.createScript(stmt);
            result = script.execute(ctx, "querty");
        } catch (final JexlException xany) {
            assertNotNull(xany.getMessage());
            assertTrue(xany.getMessage().contains("c${la--ss"));
        }

        // parsing fails with jxlt & strict navigation
        stmt = "(x)->{ x.`c${la--ss` ?? 'oops' }";
        try {
        script = engine.createScript(stmt);
        result = script.execute(ctx, "querty");
        } catch (final JexlException xany) {
            assertNotNull(xany.getMessage());
            assertTrue(xany.getMessage().contains("c${la--ss"));
        }
    }

    @Test
    void testInnerProperty() throws Exception {
        final PropertyArithmetic pa = new PropertyArithmetic(true);
        final JexlEngine jexl = new JexlBuilder().arithmetic(pa).debug(true).strict(true).cache(32).create();
        final Container quux = new Container("quux", 42);
        final JexlScript get;
        Object result;

        final int calls = pa.getCalls();
        final JexlScript getName = JEXL.createScript("foo.property.name", "foo");
        result = getName.execute(null, quux);
        assertEquals("quux", result);

        final JexlScript get0 = JEXL.createScript("foo.property.0", "foo");
        result = get0.execute(null, quux);
        assertEquals("quux", result);

        final JexlScript getNumber = JEXL.createScript("foo.property.number", "foo");
        result = getNumber.execute(null, quux);
        assertEquals(42, result);

        final JexlScript get1 = JEXL.createScript("foo.property.1", "foo");
        result = get1.execute(null, quux);
        assertEquals(42, result);

        final JexlScript setName = JEXL.createScript("foo.property.name = $0", "foo", "$0");
        setName.execute(null, quux, "QUUX");
        result = getName.execute(null, quux);
        assertEquals("QUUX", result);
        result = get0.execute(null, quux);
        assertEquals("QUUX", result);

        final JexlScript set0 = JEXL.createScript("foo.property.0 = $0", "foo", "$0");
        set0.execute(null, quux, "BAR");
        result = getName.execute(null, quux);
        assertEquals("BAR", result);
        result = get0.execute(null, quux);
        assertEquals("BAR", result);

        final JexlScript setNumber = JEXL.createScript("foo.property.number = $0", "foo", "$0");
        setNumber.execute(null, quux, -42);
        result = getNumber.execute(null, quux);
        assertEquals(-42, result);
        result = get1.execute(null, quux);
        assertEquals(-42, result);

        final JexlScript set1 = JEXL.createScript("foo.property.1 = $0", "foo", "$0");
        set1.execute(null, quux, 24);
        result = getNumber.execute(null, quux);
        assertEquals(24, result);
        result = get1.execute(null, quux);
        assertEquals(24, result);

        assertEquals(calls, pa.getCalls());
    }

    @Test
    void testInnerViaArithmetic() throws Exception {
        final PropertyArithmetic pa = new PropertyArithmetic(true);
        final JexlEngine jexl = new JexlBuilder().arithmetic(pa).debug(true).strict(true).cache(32).create();
        final PropertyContainer quux = new PropertyContainer("bar", 169);
        Object result;

        final JexlScript getName = jexl.createScript("foo.property.name", "foo");
        result = getName.execute(null, quux);
        assertEquals("bar", result);
        final int calls = pa.getCalls();
        final JexlScript setName = jexl.createScript("foo.property.name = $0", "foo", "$0");
        setName.execute(null, quux, 123);
        result = getName.execute(null, quux);
        assertEquals("123", result);
        setName.execute(null, quux, 456);
        result = getName.execute(null, quux);
        assertEquals("456", result);
        assertEquals(calls + 2, pa.getCalls());
        setName.execute(null, quux, "quux");
        result = getName.execute(null, quux);
        assertEquals("QUUX", result);
        assertEquals(calls + 2, pa.getCalls());

        final JexlScript getNumber = jexl.createScript("foo.property.number", "foo");
        result = getNumber.execute(null, quux);
        assertEquals(169, result);
        final JexlScript setNumber = jexl.createScript("foo.property.number = $0", "foo", "$0");
        setNumber.execute(null, quux, 42);
        result = getNumber.execute(null, quux);
        assertEquals(1042, result);
        setNumber.execute(null, quux, 24);
        result = getNumber.execute(null, quux);
        assertEquals(1024, result);
        assertEquals(calls + 4, pa.getCalls());
        setNumber.execute(null, quux, "42");
        result = getNumber.execute(null, quux);
        assertEquals(1042, result);
        assertEquals(calls + 4, pa.getCalls());
    }

    @Test
    void testPropertyProperty() throws Exception {
        final Integer i42 = Integer.valueOf(42);
        final Integer i43 = Integer.valueOf(43);
        final String s42 = "fourty-two";
        final Object[] foo = new Object[3];
        foo[0] = foo;
        foo[1] = i42;
        foo[2] = s42;
        asserter.setVariable("foo", foo);
        asserter.setVariable("zero", Integer.valueOf(0));
        asserter.setVariable("one", Integer.valueOf(1));
        asserter.setVariable("two", Integer.valueOf(2));
        for (int l = 0; l < 2; ++l) {
            asserter.assertExpression("foo.0", foo);
            asserter.assertExpression("foo.0.'0'", foo);
            asserter.assertExpression("foo.'1'", foo[1]);
            asserter.assertExpression("foo.0.'1'", foo[1]);
            asserter.assertExpression("foo.0.'1' = 43", i43);
            asserter.assertExpression("foo.0.'1'", i43);
            asserter.assertExpression("foo.0.'1' = 42", i42);
            //
            asserter.assertExpression("foo?.0.'1'", i42);
            asserter.assertExpression("foo?.0", foo);
            asserter.assertExpression("foo?.0.'0'", foo);
            asserter.assertExpression("foo?.'1'", foo[1]);
            asserter.assertExpression("foo.0?.'1'", foo[1]);
            asserter.assertExpression("foo?.0.'1' = 43", i43);
            asserter.assertExpression("foo?.0?.'1'", i43);
            asserter.assertExpression("foo?.0.'1' = 42", i42);
            asserter.assertExpression("foo?.0.'1'", i42);
            //
            asserter.assertExpression("foo?.0.`1`", i42);
            asserter.assertExpression("foo?.0", foo);
            asserter.assertExpression("foo?.0.'0'", foo);
            asserter.assertExpression("foo?.`1`", foo[1]);
            asserter.assertExpression("foo?.0.`1`", foo[1]);
            asserter.assertExpression("foo?.0.`${one}` = 43", i43);
            asserter.assertExpression("foo.0?.`${one}`", i43);
            asserter.assertExpression("foo.0.`${one}` = 42", i42);
            asserter.assertExpression("foo?.0?.`${one}`", i42);
            //
            asserter.assertExpression("foo?[0].'1'", i42);
            asserter.assertExpression("foo?[0]", foo);
            asserter.assertExpression("foo?[0].'0'", foo);
            asserter.assertExpression("foo?[1]", foo[1]);
            asserter.assertExpression("foo[0]?.'1'", foo[1]);
            asserter.assertExpression("foo?[0].'1' = 43", i43);
            asserter.assertExpression("foo?[0]?.'1'", i43);
            asserter.assertExpression("foo?[0].'1' = 42", i42);
            asserter.assertExpression("foo?[0].'1'", i42);
        }
    }
     @Test
    void testStringIdentifier() throws Exception {
        final Map<String, String> foo = new HashMap<>();

        final JexlContext jc = new MapContext();
        jc.set("foo", foo);
        foo.put("q u u x", "456");
        JexlExpression e = JEXL.createExpression("foo.\"q u u x\"");
        Object result = e.evaluate(jc);
        assertEquals("456", result);
        e = JEXL.createExpression("foo.'q u u x'");
        result = e.evaluate(jc);
        assertEquals("456", result);
        JexlScript s = JEXL.createScript("foo.\"q u u x\"");
        result = s.execute(jc);
        assertEquals("456", result);
        s = JEXL.createScript("foo.'q u u x'");
        result = s.execute(jc);
        assertEquals("456", result);

        final Debugger dbg = new Debugger();
        dbg.debug(e);
        final String dbgdata = dbg.toString();
        assertEquals("foo.'q u u x'", dbgdata);
    }

}
