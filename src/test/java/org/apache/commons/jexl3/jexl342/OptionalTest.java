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
package org.apache.commons.jexl3.jexl342;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.junit.Assert;
import org.junit.Test;

public class OptionalTest {

    public static class StreamContext extends MapContext {
        public Stream map(final Collection<Object> c, final JexlScript s) {
            final JexlContext context = JexlEngine.getThreadContext();
            return c.stream().map(a->s.execute(context, a));
        }
        public Object reduce(final Stream<Object> stream, final JexlScript script) {
            final Object reduced = stream.reduce((identity, element)->{
                final JexlContext context = JexlEngine.getThreadContext();
                return script.execute(context, identity, element);
            });
            return reduced instanceof Optional<?>
                    ? ((Optional<?>) reduced).get()
                    : reduced;
        }
    }

    public static class Thing {
        String name;
        public Optional<String> findName() {
            return  Optional.ofNullable(name);
        }

        public Optional<List<String>> findNames() {
            if (name == null) {
                return Optional.empty();
            }
            return Optional.of(Collections.singletonList(name));
        }
    }

    @Test
    public void test342() {
        final JexlBuilder builder = new JexlBuilder();
        final JexlUberspect uber = builder.create().getUberspect();
        final JexlEngine jexl = builder.uberspect(new ReferenceUberspect(uber)).safe(false).create();
        final JexlInfo info = new JexlInfo("test352", 1, 1);
        final Thing thing = new Thing();
        JexlScript script;

        script = jexl.createScript(info.at(53, 1),"thing.name.length()", "thing");
        Object result = script.execute(null, thing);
        Assert.assertNull(result);

        thing.name = "foo";
        result = script.execute(null, thing);
        Assert.assertEquals(3, result);

        try {
            script = jexl.createScript(info.at(62, 1), "thing.name.size()", "thing");
            result = script.execute(null, thing);
            Assert.fail("should have thrown");
        } catch (final JexlException.Method xmethod) {
            Assert.assertEquals("size", xmethod.getDetail());
            Assert.assertEquals("test352@62:11 unsolvable function/method 'size'", xmethod.getMessage());
        }

        try {
            script = jexl.createScript(info.at(71, 1), "thing.name?.size()", "thing");
            result = script.execute(null, thing);
        } catch (final JexlException.Method xmethod) {
            Assert.fail("should not have thrown");
        }

        thing.name = null;
        script = jexl.createScript(info,"thing.names.size()", "thing");
        result = script.execute(null, thing);
        Assert.assertNull(result);
        thing.name = "froboz";
        script = jexl.createScript(info,"thing.names", "thing");
        result = script.execute(null, thing);
        Assert.assertNotNull(result);
        script = jexl.createScript(info,"thing.names.size()", "thing");
        result = script.execute(null, thing);
        Assert.assertEquals(1, result);
    }

    @Test
    public void testOptionalArgs() {
        final JexlBuilder builder = new JexlBuilder();
        final JexlArithmetic jexla = new OptionalArithmetic(true);
        final JexlUberspect uber = builder.create().getUberspect();
        final JexlEngine jexl = builder.uberspect(new ReferenceUberspect(uber)).arithmetic(jexla).safe(false).create();
        final JexlInfo info = new JexlInfo("testStream", 1, 1);
        final MapContext context = new StreamContext();
        final String src = "x + x";
        final JexlScript script = jexl.createScript(src, "x");
        final Optional<Integer> x = Optional.of(21);
        final Object result = script.execute(context, x);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testStream0() {
        final String src = "$0.map(x -> x * x).reduce((a, x) -> a + x)";
        final JexlBuilder builder = new JexlBuilder();
        final JexlUberspect uber = builder.create().getUberspect();
        final JexlArithmetic jexla = new OptionalArithmetic(true);
        final JexlEngine jexl = builder.uberspect(new ReferenceUberspect(uber)).arithmetic(jexla).safe(false).create();
        final JexlInfo info = new JexlInfo("testStream", 1, 1);
        final MapContext context = new StreamContext();
        final JexlScript script = jexl.createScript(src, "$0");
        final Object result = script.execute(context, Arrays.asList(1, 2, 3));
        Assert.assertEquals(14, result);
    }

    @Test
    public void testStream1() {
        final String src = "$0.map(x -> x * x).reduce((a, x) -> a + x)";
        final JexlEngine jexl = new JexlBuilder().safe(false).create();
        final JexlInfo info = new JexlInfo("testStream", 1, 1);
        final MapContext context = new StreamContext();
        final JexlScript script = jexl.createScript(src, "$0");
        final Object result = script.execute(context, Arrays.asList(1, 2d, "3"));
        Assert.assertEquals(14.0d, (double) result , 0.00001d);
    }
}
